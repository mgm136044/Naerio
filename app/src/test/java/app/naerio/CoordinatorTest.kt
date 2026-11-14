package app.naerio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import app.naerio.navigation.*
import org.junit.Assert.*
import org.junit.Test

/** 실제 SDK 동작을 흉내 내는 테스트 더블. SDK가 주지 않는 요청 식별자는 넣지 않는다. */
private class FakeGateway(var connected: Boolean = true) : NavigationGateway {
    override val kind = GatewayKind.SIMULATED
    override val connection = MutableStateFlow<Connection>(if (connected) Connection.Connected else Connection.Failed("x"))
    override val events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 16)
    var requests = 0
    var onRequest: suspend (List<RequestPoint>) -> Unit = {}
    var snapshot: RouteSnapshot = RouteSnapshot(Read.NoResponse, Read.NoResponse, Read.NoResponse)
    var scope: CoroutineScope? = null
    override fun connect() {}
    override suspend fun readRoute(timeoutMs: Long) = snapshot
    override fun request(points: List<RequestPoint>) { requests++; scope!!.launch { onRequest(points) } }
    override fun release() {}
}

private val B = RequestPoint("drop_b", "하차 후보 B", 37.48712, 127.03433, asWaypoint = true)
private val P = RequestPoint("park_p_ent", "방문객 주차장 P 진입구", 37.4882, 127.0329, asWaypoint = false)
private val A = RequestPoint("drop_a", "하차 후보 A", 37.48545, 127.03637, asWaypoint = false)
private fun snap(dest: NavPoint?, wps: List<NavPoint> = emptyList()) = RouteSnapshot(Read.Value(dest != null), Read.Value(dest), Read.Value(wps))

@OptIn(ExperimentalCoroutinesApi::class)
class CoordinatorTest {

    private fun TestScope.setup(g: FakeGateway) = NavigationRequestCoordinator(g, eventTimeoutMs = 1_000, postStartGraceMs = 100).also { g.scope = this }

    @Test fun singleDestination_confirmedWhenReadbackMatches() = runTest {
        val g = FakeGateway(); val c = setup(g)
        g.onRequest = { g.snapshot = snap(NavPoint(A.name, A.latitude, A.longitude)); g.events.emit(NavEvent.RouteStarted) }
        val r = c.apply("p", listOf(A)) { false }
        assertEquals(ApplyStage.CONFIRMED, r.stage)
    }

    @Test fun eventOnlyWithoutMatchingReadback_isNotSuccess() = runTest {
        val g = FakeGateway(); val c = setup(g)
        // 경로 탐색 완료 이벤트는 왔지만 목적지가 다른 곳(외부 변경 등)
        g.onRequest = { g.snapshot = snap(NavPoint("다른 곳", 37.5, 127.0)); g.events.emit(NavEvent.RoutingCompleted) }
        val r = c.apply("p", listOf(A)) { false }
        assertEquals(ApplyStage.UNCONFIRMED, r.stage)
    }

    @Test fun nearbyDifferentEntrance_isNotTreatedAsSame() = runTest {
        val g = FakeGateway(); val c = setup(g)
        // 30m 떨어진 다른 입구로 반영되면 허용 오차(15m) 밖이다
        g.onRequest = { g.snapshot = snap(NavPoint("옆 입구", A.latitude + 0.00027, A.longitude)); g.events.emit(NavEvent.RouteStarted) }
        assertEquals(ApplyStage.UNCONFIRMED, c.apply("p", listOf(A)) { false }.stage)
    }

    @Test fun noResponse_isUnconfirmedNotFailed() = runTest {
        val g = FakeGateway(); val c = setup(g)
        val r = c.apply("p", listOf(A)) { false }
        assertEquals(ApplyStage.UNCONFIRMED, r.stage)
        assertTrue(r.detail.any { "이벤트를 받지 못했습니다" in it })
    }

    @Test fun sdkError_isFailed() = runTest {
        val g = FakeGateway(); val c = setup(g)
        g.onRequest = { g.events.emit(NavEvent.Error("CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE")) }
        val r = c.apply("p", listOf(A)) { false }
        assertEquals(ApplyStage.FAILED, r.stage)
        assertTrue(r.detail.single().contains("TOO_SHORT"))
    }

    @Test fun waypointDropped_isNotConfirmed() = runTest {
        val g = FakeGateway(); val c = setup(g)
        g.onRequest = { g.snapshot = snap(NavPoint(P.name, P.latitude, P.longitude), emptyList()); g.events.emit(NavEvent.RouteStarted) }
        assertEquals(ApplyStage.UNCONFIRMED, c.apply("p", listOf(B, P)) { false }.stage)
    }

    @Test fun waypointWithZeroCoordinates_matchedByNameAndOrder() = runTest {
        val g = FakeGateway(); val c = setup(g)
        g.onRequest = { g.snapshot = snap(NavPoint(P.name, P.latitude, P.longitude), listOf(NavPoint(B.name, 0.0, 0.0))); g.events.emit(NavEvent.RouteStarted) }
        val r = c.apply("p", listOf(B, P)) { false }
        assertEquals(ApplyStage.CONFIRMED, r.stage)
        assertTrue(r.detail.any { "이름과 순서로 확인" in it })
    }

    @Test fun waypointWrongName_isNotConfirmed() = runTest {
        val g = FakeGateway(); val c = setup(g)
        g.onRequest = { g.snapshot = snap(NavPoint(P.name, P.latitude, P.longitude), listOf(NavPoint("기존 경유지", 0.0, 0.0))); g.events.emit(NavEvent.RouteStarted) }
        assertEquals(ApplyStage.UNCONFIRMED, c.apply("p", listOf(B, P)) { false }.stage)
    }

    @Test fun duplicateTaps_sendOnlyOneRequest() = runTest {
        val g = FakeGateway(); val c = setup(g)
        g.onRequest = { delay(300); g.snapshot = snap(NavPoint(A.name, A.latitude, A.longitude)); g.events.emit(NavEvent.RouteStarted) }
        val first = async { c.apply("p", listOf(A)) { false } }
        runCurrent()
        c.apply("p", listOf(A)) { false }   // 진행 중 두 번째 입력
        c.apply("p", listOf(A)) { false }
        first.await()
        assertEquals(1, g.requests)
    }

    @Test fun restricted_orUnknownRestriction_blocksRequest() = runTest {
        val g = FakeGateway(); val c = setup(g)
        assertEquals(ApplyStage.RESTRICTED, c.apply("p", listOf(A)) { true }.stage)
        assertEquals(ApplyStage.RESTRICTED, c.apply("p", listOf(A)) { null }.stage)
        assertEquals(0, g.requests)
    }

    @Test fun disconnected_isUnsupported_andNoSimulatedFallback() = runTest {
        val g = FakeGateway(connected = false); val c = setup(g)
        assertEquals(ApplyStage.UNSUPPORTED, c.apply("p", listOf(A)) { false }.stage)
        assertEquals(0, g.requests)
    }

    @Test fun lateResponseAfterTimeout_stillVerifiedByReadback() = runTest {
        val g = FakeGateway(); val c = setup(g)
        // 이벤트는 제한 시간 뒤에 오지만 재조회 시점에는 이미 반영돼 있다
        g.onRequest = { g.snapshot = snap(NavPoint(A.name, A.latitude, A.longitude)); delay(5_000); g.events.emit(NavEvent.RouteStarted) }
        val r = c.apply("p", listOf(A)) { false }
        assertEquals(ApplyStage.CONFIRMED, r.stage)
        assertTrue(r.detail.any { "이벤트를 받지 못했습니다" in it })
    }
}
