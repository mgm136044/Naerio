package app.naerio.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

enum class ApplyStage { IDLE, REQUESTING, VERIFYING, CONFIRMED, FAILED, UNCONFIRMED, UNSUPPORTED, RESTRICTED }

data class ApplyState(
    val stage: ApplyStage = ApplyStage.IDLE,
    val requestNo: Int = 0,
    val planId: String? = null,
    val message: String = "",
    val detail: List<String> = emptyList(),
)

/**
 * 경로 변경 요청을 한 번에 하나만 처리하고, 이벤트 대기와 재조회로 반영 여부를 판단한다.
 * SDK에는 요청 식별자가 없으므로, 요청 번호만으로 오래된 콜백을 완전히 구별한다고 가정하지 않는다.
 * 최종 판단은 재조회 결과와 승인된 계획의 대조로 한다.
 */
class NavigationRequestCoordinator(
    private val gateway: NavigationGateway,
    private val matchToleranceMeters: Double = MATCH_TOLERANCE_METERS,
    private val eventTimeoutMs: Long = 20_000,
    private val postStartGraceMs: Long = 2_000,
) {
    private val _state = MutableStateFlow(ApplyState())
    val state = _state.asStateFlow()
    private var counter = 0

    val busy get() = _state.value.stage == ApplyStage.REQUESTING || _state.value.stage == ApplyStage.VERIFYING

    /** 사용자가 확인한 계획을 고정해 요청한다. 진행 중이면 무시한다(중복 입력 방지). */
    suspend fun apply(planId: String, points: List<RequestPoint>, isRestricted: () -> Boolean?): ApplyState {
        if (busy) return _state.value
        val no = ++counter
        if (points.none { !it.asWaypoint }) return set(ApplyState(ApplyStage.UNSUPPORTED, no, planId, "보낼 목적지가 없습니다."))
        when (isRestricted()) {
            true -> return set(ApplyState(ApplyStage.RESTRICTED, no, planId, "주행 제한 상태에서는 경로를 바꿀 수 없습니다."))
            null -> return set(ApplyState(ApplyStage.RESTRICTED, no, planId, "주행 제한 상태를 확인하지 못해 경로 변경을 보류했습니다."))
            false -> {}
        }
        if (gateway.connection.value is Connection.Connecting)
            withTimeoutOrNull(5_000) { gateway.connection.first { it !is Connection.Connecting } }
        if (gateway.connection.value !is Connection.Connected)
            return set(ApplyState(ApplyStage.UNSUPPORTED, no, planId, "내비 연결을 사용할 수 없습니다."))

        set(ApplyState(ApplyStage.REQUESTING, no, planId, "내비에 요청 중"))
        // 이벤트 구독을 먼저 시작한 뒤 요청을 보낸다. 특정 이벤트 하나만 기다리다 끝나지 않는 구조를 피하려 제한 시간을 둔다.
        val outcome = coroutineScope {
            val waiter = async(start = CoroutineStart.UNDISPATCHED) {
                // 실험 결과: Pleos 에뮬레이터는 요청 후 onRouteStarted만 보내고 onRoutingCompleted, onDestinationChanged는
                // 오지 않을 수 있다. 탐색 시작을 받으면 짧게 오류를 더 기다린 뒤 재조회로 판단한다.
                withTimeoutOrNull(eventTimeoutMs) {
                    val first = gateway.events.first { it is NavEvent.Error || it is NavEvent.RouteStarted || it is NavEvent.RoutingCompleted || it is NavEvent.DestinationChanged }
                    if (first is NavEvent.RouteStarted) {
                        withTimeoutOrNull(postStartGraceMs) { gateway.events.first { it is NavEvent.Error || it is NavEvent.RoutingCompleted } } ?: first
                    } else first
                }
            }
            try { gateway.request(points) } catch (e: Throwable) {
                waiter.cancel()
                return@coroutineScope NavEvent.Error(e.message ?: e.javaClass.simpleName)
            }
            waiter.await()
        }
        return finish(no, planId, points, outcome)
    }

    private suspend fun finish(no: Int, planId: String, points: List<RequestPoint>, outcome: NavEvent?): ApplyState {
        if (outcome is NavEvent.Error)
            return set(ApplyState(ApplyStage.FAILED, no, planId, "내비가 경로 요청을 처리하지 못했습니다.", listOf("오류 코드: ${outcome.code}")))

        set(ApplyState(ApplyStage.VERIFYING, no, planId, "경로 반영 확인 중"))
        val snap = gateway.readRoute()
        val verdict = verify(points, snap)
        val notes = buildList {
            if (outcome == null) add("내비 이벤트를 받지 못했습니다.")
            addAll(verdict.notes)
        }
        return set(
            when (verdict.result) {
                Verdict.MATCH -> ApplyState(ApplyStage.CONFIRMED, no, planId, "반영 확인 완료", notes)
                Verdict.MISMATCH -> ApplyState(ApplyStage.UNCONFIRMED, no, planId, "내비 경로가 승인한 계획과 다릅니다. 반영 확인이 필요합니다.", notes)
                Verdict.NO_DATA -> ApplyState(ApplyStage.UNCONFIRMED, no, planId, "경로 요청을 보냈지만 반영 여부를 확인하지 못했습니다.", notes)
            }
        )
    }

    enum class Verdict { MATCH, MISMATCH, NO_DATA }
    data class Verification(val result: Verdict, val notes: List<String>)

    fun verify(points: List<RequestPoint>, snap: RouteSnapshot): Verification {
        val notes = mutableListOf<String>()
        val want = points.last { !it.asWaypoint }
        val wantWps = points.filter { it.asWaypoint }
        val dest = snap.destination
        if (dest !is Read.Value || dest.value == null) {
            notes += "목적지 조회 응답 없음"
            return Verification(Verdict.NO_DATA, notes)
        }
        val d = distanceMeters(want.latitude, want.longitude, dest.value.latitude, dest.value.longitude)
        notes += "목적지 오차 ${"%.1f".format(d)}m (기준 ${matchToleranceMeters.toInt()}m)"
        var ok = d <= matchToleranceMeters
        if (wantWps.isNotEmpty()) {
            val wps = snap.waypoints
            if (wps !is Read.Value) { notes += "경유지 조회 응답 없음"; return Verification(Verdict.NO_DATA, notes) }
            if (wps.value.size != wantWps.size) { notes += "경유지 개수 ${wps.value.size}개, 요청 ${wantWps.size}개"; ok = false }
            else wantWps.zip(wps.value).forEachIndexed { i, (w, got) ->
                if (got.latitude == 0.0 && got.longitude == 0.0) {
                    // 실험 결과: Pleos 에뮬레이터는 경유지 좌표를 0,0으로 돌려준다. 이 경우 이름과 순서로만 확인한다.
                    val same = got.name == w.name
                    notes += "경유지 ${i + 1}: SDK가 좌표를 주지 않아 이름과 순서로 확인 (${if (same) "일치" else "불일치: ${got.name}"})"
                    if (!same) ok = false
                } else {
                    val e = distanceMeters(w.latitude, w.longitude, got.latitude, got.longitude)
                    notes += "경유지 ${i + 1} 오차 ${"%.1f".format(e)}m"
                    if (e > matchToleranceMeters) ok = false
                }
            }
        }
        return Verification(if (ok) Verdict.MATCH else Verdict.MISMATCH, notes)
    }

    /** 앱 내부 대기만 해제한다. 차량 내비 길 안내는 취소하지 않는다. */
    fun dismiss() { if (!busy) _state.value = ApplyState(requestNo = counter) }

    private fun set(s: ApplyState): ApplyState { _state.value = s; return s }

    companion object {
        /** 반환 좌표 비교 허용 오차. 에뮬레이터 실험 결과로 정한다(docs/SDK_VERIFICATION.md). */
        const val MATCH_TOLERANCE_METERS = 15.0
    }
}
