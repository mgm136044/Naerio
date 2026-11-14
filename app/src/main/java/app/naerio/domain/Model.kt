package app.naerio.domain

import kotlinx.serialization.Serializable

/*
 * Naerio(내리오) 앱 내부 데이터 모델. Pleos SDK 클래스가 아니다.
 * 상위 구역, 실제 방문 장소, 차량 지점(진입, 하차, 주차 진입), 보행 연결을 분리해 둔다.
 */

/** 데이터 파일 전체의 성격. 시설 단위 표시와 별개로 개별 사실의 확인 상태를 따로 둔다. */
@Serializable
enum class DataKind { FICTIONAL_SAMPLE, PARTIALLY_VERIFIED, FIELD_VERIFIED }

@Serializable
enum class SiteType { APARTMENT, SCHOOL, OFFICE, RESTAURANT, COMPLEX }

/** 차량 또는 보행 지점의 역할. 이름과 좌표만 있는 POI 하나로 합치지 않는다. */
@Serializable
enum class PointRole { VEHICLE_ENTRANCE, DROPOFF, PARKING_ENTRANCE, PARKING_PEDESTRIAN_EXIT, PEDESTRIAN_ENTRANCE }

/** 사실의 확인 상태 */
@Serializable
enum class FactStatus { KNOWN, UNKNOWN, CONFLICTING, RECHECK_REQUIRED }

@Serializable
enum class SourceType { FICTIONAL, OPERATOR_NOTICE, PUBLIC_DATA, FIELD_SURVEY }

@Serializable
data class Coordinate(val latitude: Double, val longitude: Double)

/** 하나의 사실. value는 status가 KNOWN일 때만 의미가 있다. */
@Serializable
data class Fact(
    val status: FactStatus = FactStatus.UNKNOWN,
    val value: Boolean? = null,
    val evidenceIds: List<String> = emptyList(),
    val note: String? = null,
)

@Serializable
data class Evidence(
    val id: String,
    val sourceType: SourceType,
    val sourceTitle: String,
    val sourceUrl: String? = null,
    val sourcePublishedAt: String? = null,
    val sourceUpdatedAt: String? = null,
    val checkedAt: String? = null,
    val applicablePlaceIds: List<String> = emptyList(),
    val validUntil: String? = null,
    val recheckRequired: Boolean = false,
    val confirmedFact: String,
    val permittedUseNote: String? = null,
)

/** 차량 또는 보행 지점. coordinate는 실제 확인된 접근 좌표, sdkTestCoordinate는 에뮬레이터 SDK 시험 전용 값이다. */
@Serializable
data class Point(
    val id: String,
    val role: PointRole,
    val name: String,
    val description: String? = null,
    val coordinate: Coordinate? = null,
    val sdkTestCoordinate: Coordinate? = null,
    val parkingFacilityId: String? = null,
    /** 차량이 이 지점까지 접근할 수 있는가 */
    val vehicleAccess: Fact? = null,
    /** 이 지점에서 동승자 하차가 허용되는가 (DROPOFF) */
    val dropoffAllowed: Fact? = null,
)

@Serializable
data class ParkingFacility(
    val id: String,
    val name: String,
    /** 방문객 주차 가능 여부 */
    val visitorParking: Fact,
    val visitorCondition: String? = null,
    val operatingHours: String? = null,
)

/** 차에서 내린 사람이 실제 방문 장소로 가는 연결. 직선이 아니라 확인된 안내를 담는다. */
@Serializable
data class PedestrianConnection(
    val id: String,
    val fromPointId: String,
    val toDestinationId: String,
    val guidance: String,
    val stepFree: Fact = Fact(),
    val covered: Fact = Fact(),
    /** 실제 보행 거리. 모르면 null로 둔다(0으로 바꾸지 않음). */
    val walkDistanceMeters: Int? = null,
)

/** 구역 내부 두 차량 지점 사이의 이동 가능 여부 */
@Serializable
data class VehicleConnection(
    val id: String,
    val fromPointId: String,
    val toPointId: String,
    val allowed: Fact,
)

@Serializable
data class VisitDestination(
    val id: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class Site(
    val id: String,
    val name: String,
    val siteType: SiteType,
    val description: String? = null,
    val destinations: List<VisitDestination>,
    val points: List<Point>,
    val parkingFacilities: List<ParkingFacility> = emptyList(),
    val pedestrianConnections: List<PedestrianConnection> = emptyList(),
    val vehicleConnections: List<VehicleConnection> = emptyList(),
)

@Serializable
data class PlaceData(
    val schemaVersion: Int,
    val dataKind: DataKind,
    val notice: String,
    val sites: List<Site>,
    val evidence: List<Evidence> = emptyList(),
)

// ---------- 사용자 선택 ----------

enum class VisitMode { PARK_AND_VISIT, DROPOFF_THEN_PARK, DROPOFF_AND_LEAVE }

enum class Strength { OFF, PREFERRED, REQUIRED }

/** 계단 없는 연결 조건을 누구의 이동에 적용하는지 */
enum class AppliesTo { PASSENGER, EVERYONE }

data class UserConditions(
    val stepFree: Strength = Strength.REQUIRED,
    val stepFreeAppliesTo: AppliesTo = AppliesTo.PASSENGER,
    val covered: Strength = Strength.OFF,
)

// ---------- 판정 결과 ----------

enum class RequirementResult { SATISFIED, NOT_SATISFIED, UNVERIFIED, NOT_APPLICABLE }

enum class PlanEligibility { ELIGIBLE, INELIGIBLE, NEEDS_VERIFICATION }

/** 계획 하나를 구성하는 개별 확인 항목 */
data class Check(
    val label: String,
    val required: Boolean,
    val result: RequirementResult,
    val reason: String,
    val evidenceIds: List<String> = emptyList(),
)

/** 내비에 보낼 지점 하나 */
data class NavTarget(val point: Point, val asWaypoint: Boolean)

data class ArrivalPlan(
    val id: String,
    val mode: VisitMode,
    val destination: VisitDestination,
    val dropoff: Point?,
    val parkingEntrance: Point?,
    val parkingFacility: ParkingFacility?,
    val passengerWalk: PedestrianConnection?,
    val driverWalk: PedestrianConnection?,
    val checks: List<Check>,
    val eligibility: PlanEligibility,
) {
    /** 차량에 보낼 순서: 경유지(하차점) 다음 최종 목적지 */
    val navTargets: List<NavTarget>
        get() = when (mode) {
            VisitMode.PARK_AND_VISIT -> listOfNotNull(parkingEntrance?.let { NavTarget(it, false) })
            VisitMode.DROPOFF_AND_LEAVE -> listOfNotNull(dropoff?.let { NavTarget(it, false) })
            VisitMode.DROPOFF_THEN_PARK -> listOfNotNull(
                dropoff?.let { NavTarget(it, true) },
                parkingEntrance?.let { NavTarget(it, false) },
            )
        }

    val title: String
        get() = when (mode) {
            VisitMode.PARK_AND_VISIT -> parkingEntrance?.name ?: "주차 진입점 없음"
            VisitMode.DROPOFF_AND_LEAVE -> dropoff?.name ?: "하차점 없음"
            VisitMode.DROPOFF_THEN_PARK -> "${dropoff?.name ?: "하차점 없음"} 하차 후 ${parkingEntrance?.name ?: "주차 진입점 없음"}"
        }
}
