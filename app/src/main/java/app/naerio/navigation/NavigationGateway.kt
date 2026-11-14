package app.naerio.navigation

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

/*
 * 앱 내부 경계. 실제 Pleos SDK가 같은 인터페이스를 제공한다는 뜻이 아니다.
 * SDK 호출은 PleosNavigationGateway 한 곳에 모은다.
 */

enum class GatewayKind { SIMULATED, REAL_SDK }

sealed interface Connection {
    data object Connecting : Connection
    data object Connected : Connection
    data class Failed(val message: String) : Connection
}

data class NavPoint(val name: String?, val latitude: Double, val longitude: Double, val poiId: String? = null)

/** 조회 결과. NoResponse는 "목적지가 없다"는 뜻이 아니라 응답을 받지 못했다는 뜻이다. */
sealed interface Read<out T> {
    data class Value<T>(val value: T) : Read<T>
    data object NoResponse : Read<Nothing>
}

data class RouteSnapshot(
    val isRouting: Read<Boolean>,
    val destination: Read<NavPoint?>,
    val waypoints: Read<List<NavPoint>>,
)

sealed interface NavEvent {
    data object RouteStarted : NavEvent
    data object RoutingCompleted : NavEvent
    data object DestinationChanged : NavEvent
    data object WaypointChanged : NavEvent
    data object RouteCancelled : NavEvent
    data class Error(val code: String) : NavEvent
}

/** 내비에 보낼 지점. asWaypoint=true는 경유지, false는 최종 목적지 */
data class RequestPoint(val id: String, val name: String, val latitude: Double, val longitude: Double, val asWaypoint: Boolean)

interface NavigationGateway {
    val kind: GatewayKind
    val connection: StateFlow<Connection>
    val events: SharedFlow<NavEvent>
    fun connect()
    suspend fun readRoute(timeoutMs: Long = 4000): RouteSnapshot
    /** 요청 호출 완료는 반영 완료가 아니다. 결과는 이벤트와 재조회로 확인한다. */
    fun request(points: List<RequestPoint>)
    fun release()
}

fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(bLat - aLat); val dLng = Math.toRadians(bLng - aLng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * sin(dLng / 2).pow(2)
    return 2 * r * asin(sqrt(h))
}
