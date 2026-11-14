package app.naerio.navigation

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/** 실제 Pleos NaviHelper SDK(3.0.5) 연동. 조회 API는 값을 돌려주지 않고 리스너 콜백으로 결과를 준다. */
class PleosNavigationGateway(context: Context) : NavigationGateway {
    override val kind = GatewayKind.REAL_SDK
    private val helper = NaviHelper(context.applicationContext)
    private val _connection = MutableStateFlow<Connection>(Connection.Connecting)
    override val connection = _connection.asStateFlow()
    private val _events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 32)
    override val events = _events.asSharedFlow()

    @Volatile private var pendingDest: CompletableDeferred<NavPoint?>? = null
    @Volatile private var pendingWaypoints: CompletableDeferred<List<NavPoint>>? = null
    @Volatile private var pendingState: CompletableDeferred<Boolean>? = null
    private var listenerAdded = false

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) { log("onRouteStarted $routeStartInfo"); _events.tryEmit(NavEvent.RouteStarted) }
        override fun onRoutingCompleted() { log("onRoutingCompleted"); _events.tryEmit(NavEvent.RoutingCompleted) }
        override fun onDestinationChanged() { log("onDestinationChanged"); _events.tryEmit(NavEvent.DestinationChanged) }
        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) { log("onWaypointChanged $waypointChangedInfo"); _events.tryEmit(NavEvent.WaypointChanged) }
        override fun onRouteCancelled() { log("onRouteCancelled"); _events.tryEmit(NavEvent.RouteCancelled) }
        override fun onError(errorCode: NaviErrorCode) { log("onError $errorCode"); _events.tryEmit(NavEvent.Error(errorCode.name)) }
        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            log("onDestinationInfo $destinationInfo")
            // 실험 결과: 경로가 없으면 좌표 0,0과 빈 이름을 돌려준다. 이를 "목적지 없음"으로 본다.
            val empty = destinationInfo.latitude == 0.0 && destinationInfo.longitude == 0.0
            pendingDest?.complete(if (empty) null else NavPoint(destinationInfo.name, destinationInfo.latitude, destinationInfo.longitude, destinationInfo.poiId))
        }
        override fun onWayPointInfo(waypointInfo: List<WaypointInfo>) {
            log("onWayPointInfo $waypointInfo")
            pendingWaypoints?.complete(waypointInfo.map { NavPoint(it.name, it.latitude, it.longitude, it.poiId) })
        }
        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            log("onRouteStateInfo $routeStateInfo")
            // 실험 결과: 경로 안내 중에도 isRouting=false가 온다. 안내 여부는 naviMode로 판단한다.
            val guiding = routeStateInfo.naviMode == NaviMode.ROUTE_GUIDING || routeStateInfo.naviMode == NaviMode.SIMULATION_GUIDING
            pendingState?.complete(guiding || routeStateInfo.isRouting)
        }
    }

    override fun connect() {
        _connection.value = Connection.Connecting
        try {
            helper.initialize(
                {
                    if (!listenerAdded) { helper.addListener(listener); listenerAdded = true }
                    _connection.value = Connection.Connected
                    log("connected")
                },
                { e -> _connection.value = Connection.Failed(e.message ?: e.javaClass.simpleName); log("connect failed $e") },
            )
        } catch (e: Throwable) {
            _connection.value = Connection.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    override suspend fun readRoute(timeoutMs: Long): RouteSnapshot {
        val state = CompletableDeferred<Boolean>().also { pendingState = it }
        helper.getRouteStateInfo()
        val routing = withTimeoutOrNull(timeoutMs) { state.await() }

        val dest = CompletableDeferred<NavPoint?>().also { pendingDest = it }
        helper.getDestinationInfo()
        val destination = withTimeoutOrNull(timeoutMs) { dest.await() }

        val wps = CompletableDeferred<List<NavPoint>>().also { pendingWaypoints = it }
        helper.getWaypointInfo()
        val waypoints = withTimeoutOrNull(timeoutMs) { wps.await() }

        return RouteSnapshot(
            isRouting = routing?.let { Read.Value(it) } ?: Read.NoResponse,
            destination = destination?.let { Read.Value(it) } ?: Read.NoResponse,
            waypoints = waypoints?.let { Read.Value(it) } ?: Read.NoResponse,
        )
    }

    override fun request(points: List<RequestPoint>) {
        val dest = points.last { !it.asWaypoint }
        val route = RouteInfo(
            latitude = dest.latitude, longitude = dest.longitude,
            displayLatitude = null, displayLongitude = null,
            poiName = dest.name, poiId = "", poiSubId = "", address = "",
        )
        val wps = points.filter { it.asWaypoint }
        if (wps.isEmpty()) {
            log("requestRoute ${dest.name} ${dest.latitude},${dest.longitude}")
            helper.requestRoute(route)
        } else {
            require(wps.size <= 2) { "경유지는 최대 2개" }
            val list = wps.mapIndexed { i, w ->
                RequestWaypointInfo(
                    latitude = w.latitude, longitude = w.longitude, poiName = w.name,
                    waypointIndex = if (i == 0) WaypointIndex.FIRST else WaypointIndex.SECOND,
                    poiId = "", address = "", poiSubId = "",
                )
            }
            log("requestRouteWithWaypoints ${wps.map { it.name }} -> ${dest.name}")
            helper.requestRouteWithWaypoints(RouteWithWaypointsRequest(route, list))
        }
    }

    override fun release() {
        try { if (listenerAdded) helper.removeListener(listener); helper.release() } catch (_: Throwable) {}
        listenerAdded = false
    }

    private fun log(m: String) = Log.d("NaerilgotNavi", m)
}
