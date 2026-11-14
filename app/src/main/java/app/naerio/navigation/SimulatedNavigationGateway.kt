package app.naerio.navigation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** 시연용 모의 내비. 성공뿐 아니라 오류, 무응답, 경유지 누락도 재현한다. */
class SimulatedNavigationGateway(private val scope: CoroutineScope) : NavigationGateway {
    enum class Outcome { SUCCESS, ERROR, NO_RESPONSE, DROP_WAYPOINT }

    override val kind = GatewayKind.SIMULATED
    private val _connection = MutableStateFlow<Connection>(Connection.Connecting)
    override val connection = _connection.asStateFlow()
    private val _events = MutableSharedFlow<NavEvent>(extraBufferCapacity = 16)
    override val events = _events.asSharedFlow()

    var outcome = Outcome.SUCCESS
    private var destination: NavPoint? = null
    private var waypoints: List<NavPoint> = emptyList()
    private var silent = false

    override fun connect() { _connection.value = Connection.Connected }

    override suspend fun readRoute(timeoutMs: Long): RouteSnapshot {
        delay(300)
        if (silent) return RouteSnapshot(Read.NoResponse, Read.NoResponse, Read.NoResponse)
        return RouteSnapshot(Read.Value(destination != null), Read.Value(destination), Read.Value(waypoints))
    }

    override fun request(points: List<RequestPoint>) {
        scope.launch {
            delay(600)
            when (outcome) {
                Outcome.ERROR -> _events.emit(NavEvent.Error("CAUSE_SEARCH_ROUTE"))
                Outcome.NO_RESPONSE -> silent = true
                Outcome.SUCCESS, Outcome.DROP_WAYPOINT -> {
                    silent = false
                    val d = points.last { !it.asWaypoint }
                    destination = NavPoint(d.name, d.latitude, d.longitude)
                    waypoints = if (outcome == Outcome.DROP_WAYPOINT) emptyList()
                    else points.filter { it.asWaypoint }.map { NavPoint(it.name, it.latitude, it.longitude) }
                    _events.emit(NavEvent.RouteStarted)
                    delay(400)
                    _events.emit(NavEvent.DestinationChanged)
                    _events.emit(NavEvent.RoutingCompleted)
                }
            }
        }
    }

    fun reset() { destination = null; waypoints = emptyList(); silent = false }

    override fun release() {}
}
