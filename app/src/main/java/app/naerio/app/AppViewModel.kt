package app.naerio.app

import app.naerio.BuildConfig

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import app.naerio.data.JsonPlaceRepository
import app.naerio.data.LoadResult
import app.naerio.domain.*
import app.naerio.navigation.*
import app.naerio.vehicle.*

enum class Screen { DESTINATION, VISIT, CANDIDATES, CONFIRM, DIAGNOSTICS }

data class ExistingRoute(val loading: Boolean = false, val snapshot: RouteSnapshot? = null)

data class UiState(
    val load: LoadResult? = null,
    val screen: Screen = Screen.DESTINATION,
    val siteId: String? = null,
    val destinationId: String? = null,
    val mode: VisitMode? = null,
    val conditions: UserConditions = UserConditions(),
    val selectedPlanId: String? = null,
    val navKind: GatewayKind = GatewayKind.SIMULATED,
    val simOutcome: SimulatedNavigationGateway.Outcome = SimulatedNavigationGateway.Outcome.SUCCESS,
    val existing: ExistingRoute = ExistingRoute(),
    val routeChangeAcknowledged: Boolean = false,
) {
    val data get() = (load as? LoadResult.Loaded)?.data
    val site get() = data?.sites?.firstOrNull { it.id == siteId }
    val destination get() = site?.destinations?.firstOrNull { it.id == destinationId }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("draft", Context.MODE_PRIVATE)
    private val repo = JsonPlaceRepository { app.assets.open(it).bufferedReader().use { r -> r.readText() } }
    private val builder = BuildArrivalPlans()
    private val ranker = RankEligiblePlans()

    private val simGateway = SimulatedNavigationGateway(viewModelScope).also { it.connect() }
    private var realCreated = false
    private val realGateway: PleosNavigationGateway by lazy { realCreated = true; PleosNavigationGateway(app).also { it.connect() } }
    private val simCoordinator = NavigationRequestCoordinator(simGateway, eventTimeoutMs = 5_000)
    private val realCoordinator by lazy { NavigationRequestCoordinator(realGateway) }

    val simPolicy = SimulatedInteractionPolicyProvider()
    private val realPolicy = PleosInteractionPolicyProvider(app).also { it.start() }

    private val _ui = MutableStateFlow(restoreDraft())
    val ui = _ui.asStateFlow()

    private val _diag = MutableStateFlow<List<String>>(emptyList())
    val diag = _diag.asStateFlow()

    init {
        _ui.update { it.copy(load = repo.load()) }
        _ui.update { s -> if (s.siteId == null) s.copy(siteId = s.data?.sites?.firstOrNull()?.id) else s }
        if (_ui.value.navKind == GatewayKind.REAL_SDK) realGateway
    }

    // ---------- 파생 값 ----------
    fun gateway(s: UiState = _ui.value): NavigationGateway = if (s.navKind == GatewayKind.REAL_SDK) realGateway else simGateway
    fun coordinator(s: UiState = _ui.value) = if (s.navKind == GatewayKind.REAL_SDK) realCoordinator else simCoordinator
    fun policy(s: UiState = _ui.value): InteractionPolicyProvider = if (s.navKind == GatewayKind.REAL_SDK) realPolicy else simPolicy
    val realInteraction: StateFlow<Interaction> get() = realPolicy.state

    fun ranked(s: UiState = _ui.value): RankedPlans? {
        val site = s.site ?: return null
        val dest = s.destinationId ?: return null
        val mode = s.mode ?: return null
        return ranker.rank(builder.build(site, dest, mode, s.conditions))
    }

    fun selectedPlan(s: UiState = _ui.value): ArrivalPlan? {
        val r = ranked(s) ?: return null
        return (r.eligible + r.needsVerification + r.ineligible).firstOrNull { it.id == s.selectedPlanId }
    }

    /** 실제 SDK로 보낼 지점. 가상 데이터는 시험 좌표만 쓰고, 좌표가 없으면 null */
    fun requestPoints(plan: ArrivalPlan, s: UiState = _ui.value): List<RequestPoint>? = plan.navTargets.map { t ->
        val c = t.point.coordinate ?: t.point.sdkTestCoordinate ?: return null
        RequestPoint(t.point.id, t.point.name, c.latitude, c.longitude, t.asWaypoint)
    }

    /** 현재 환경에서 이 계획을 실제로 보낼 수 있는가. null이면 가능, 문자열이면 불가 이유 */
    fun unsupportedReason(plan: ArrivalPlan, s: UiState = _ui.value): String? = when {
        plan.eligibility != PlanEligibility.ELIGIBLE -> "필요한 조건이 확인되지 않은 후보입니다."
        s.navKind == GatewayKind.REAL_SDK && plan.mode == VisitMode.DROPOFF_THEN_PARK && !VerifiedCapabilities.WAYPOINT_FLOW_VERIFIED ->
            "하차 경유지와 주차 목적지를 함께 보내는 기능은 실제 내비에서 아직 검증되지 않았습니다. 계획 미리보기만 제공합니다."
        requestPoints(plan, s) == null -> "이 후보에는 내비에 보낼 좌표가 없습니다."
        else -> null
    }

    // ---------- 사용자 동작 ----------
    fun go(screen: Screen) { update { it.copy(screen = screen) }; if (screen == Screen.CONFIRM) refreshExistingRoute() }
    fun back() = go(when (_ui.value.screen) {
        Screen.VISIT -> Screen.DESTINATION
        Screen.CANDIDATES -> Screen.VISIT
        Screen.CONFIRM -> Screen.CANDIDATES
        else -> Screen.DESTINATION
    })
    fun selectDestination(id: String) = update { it.copy(destinationId = id, selectedPlanId = null) }
    fun selectMode(m: VisitMode) = update { it.copy(mode = m, selectedPlanId = null) }
    fun setConditions(c: UserConditions) = update { it.copy(conditions = c, selectedPlanId = null) }
    fun selectPlan(id: String) = update { it.copy(selectedPlanId = id, routeChangeAcknowledged = false) }
    fun acknowledgeRouteChange(v: Boolean) = update { it.copy(routeChangeAcknowledged = v) }

    fun setNavKind(k: GatewayKind) {
        if (coordinator().busy) return
        update { it.copy(navKind = k, existing = ExistingRoute(), routeChangeAcknowledged = false) }
        if (k == GatewayKind.REAL_SDK) realGateway
        coordinator().dismiss()
        if (_ui.value.screen == Screen.CONFIRM) refreshExistingRoute()
    }

    fun setSimOutcome(o: SimulatedNavigationGateway.Outcome) { simGateway.outcome = o; update { it.copy(simOutcome = o) } }
    fun resetSimRoute() { simGateway.reset(); simCoordinator.dismiss(); refreshExistingRoute() }

    fun refreshExistingRoute() {
        val s = _ui.value
        update { it.copy(existing = ExistingRoute(loading = true)) }
        viewModelScope.launch {
            val g = gateway(s)
            if (g.connection.value !is Connection.Connected) g.connection.first { it !is Connection.Connecting }
            val snap = if (g.connection.value is Connection.Connected) g.readRoute() else null
            update { it.copy(existing = ExistingRoute(false, snap)) }
        }
    }

    fun applySelected() {
        val s = _ui.value
        val plan = selectedPlan(s) ?: return
        if (unsupportedReason(plan, s) != null) return
        val pts = requestPoints(plan, s) ?: return
        val coord = coordinator(s)
        if (coord.busy) return
        viewModelScope.launch {
            coord.apply(plan.id, pts) {
                when (policy(s).state.value) { Interaction.ALLOWED -> false; Interaction.RESTRICTED -> true; Interaction.UNKNOWN -> null }
            }
        }
    }

    fun dismissApply() = coordinator().dismiss()

    // ---------- 진단(실제 SDK 실험) ----------
    private fun log(m: String) { _diag.update { (it + "${java.time.LocalTime.now().withNano(0)} $m").takeLast(80) } }

    fun diagConnect() { realGateway; log("NaviHelper 연결 상태: ${realGateway.connection.value}") }

    fun diagRead() = viewModelScope.launch {
        val snap = realGateway.readRoute()
        log("조회: 안내중=${snap.isRouting.text()} 목적지=${snap.destination.text()} 경유지=${snap.waypoints.text()}")
    }

    fun diagRequest(pointIds: List<String>) = viewModelScope.launch {
        val site = _ui.value.site ?: return@launch
        val pts = pointIds.mapIndexed { i, id ->
            val p = site.points.first { it.id == id }
            val c = p.sdkTestCoordinate ?: return@launch log("$id: 시험 좌표 없음")
            RequestPoint(p.id, p.name, c.latitude, c.longitude, asWaypoint = i < pointIds.size - 1)
        }
        log("요청: ${pts.joinToString(" > ") { "${it.name}(${it.latitude},${it.longitude})" }}")
        // 진단 요청도 실제 주행 제한을 따른다(정차 중에만 경로 변경).
        val r = realCoordinator.apply("diag:" + pointIds.joinToString(","), pts) {
            when (realPolicy.state.value) { Interaction.ALLOWED -> false; Interaction.RESTRICTED -> true; Interaction.UNKNOWN -> null }
        }
        log("결과: ${r.stage} ${r.message} ${r.detail.joinToString("; ")}")
        diagRead()
    }

    private fun Read<*>.text(): String = when (this) {
        is Read.NoResponse -> "응답없음"
        is Read.Value -> when (val v = value) {
            null -> "없음"
            is NavPoint -> "${v.name}(${"%.6f".format(v.latitude)},${"%.6f".format(v.longitude)})"
            is List<*> -> "[" + v.joinToString { (it as NavPoint).let { p -> "${p.name}(${"%.6f".format(p.latitude)},${"%.6f".format(p.longitude)})" } } + "]"
            else -> v.toString()
        }
    }

    // ---------- 초안 저장과 복원(경로 재전송은 하지 않음) ----------
    private fun update(f: (UiState) -> UiState) { _ui.update(f); saveDraft(_ui.value) }

    private fun saveDraft(s: UiState) = prefs.edit()
        .putString("screen", if (s.screen == Screen.DIAGNOSTICS) Screen.DESTINATION.name else s.screen.name)
        .putString("site", s.siteId).putString("dest", s.destinationId).putString("mode", s.mode?.name)
        .putString("stepFree", s.conditions.stepFree.name).putString("appliesTo", s.conditions.stepFreeAppliesTo.name)
        .putString("covered", s.conditions.covered.name).putString("plan", s.selectedPlanId)
        .putString("nav", s.navKind.name).apply()

    private fun restoreDraft(): UiState = UiState(
        screen = prefs.getString("screen", null)?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
            ?.takeIf { it != Screen.DIAGNOSTICS || BuildConfig.DEBUG } ?: Screen.DESTINATION,
        siteId = prefs.getString("site", null),
        destinationId = prefs.getString("dest", null),
        mode = prefs.getString("mode", null)?.let { runCatching { VisitMode.valueOf(it) }.getOrNull() },
        conditions = UserConditions(
            stepFree = prefs.getString("stepFree", null)?.let { runCatching { Strength.valueOf(it) }.getOrNull() } ?: Strength.REQUIRED,
            stepFreeAppliesTo = prefs.getString("appliesTo", null)?.let { runCatching { AppliesTo.valueOf(it) }.getOrNull() } ?: AppliesTo.PASSENGER,
            covered = prefs.getString("covered", null)?.let { runCatching { Strength.valueOf(it) }.getOrNull() } ?: Strength.OFF,
        ),
        selectedPlanId = prefs.getString("plan", null),
        navKind = prefs.getString("nav", null)?.let { runCatching { GatewayKind.valueOf(it) }.getOrNull() } ?: GatewayKind.SIMULATED,
    )

    override fun onCleared() {
        realPolicy.stop()
        if (realCreated) runCatching { realGateway.release() }
    }
}
