package app.naerio.data

import app.naerio.domain.*

/** 장소 데이터의 참조, 좌표, 날짜, 근거, 데이터 성격 혼합 오류를 찾는다. */
object PlaceDataValidator {

    const val SUPPORTED_SCHEMA = 1
    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")

    fun validate(data: PlaceData): List<String> = buildList {
        if (data.schemaVersion != SUPPORTED_SCHEMA) add("지원하지 않는 스키마 버전: ${data.schemaVersion}")

        val evidenceById = data.evidence.associateBy { it.id }
        duplicates(data.evidence.map { it.id }).forEach { add("중복 근거 ID: $it") }
        data.evidence.forEach { ev ->
            listOfNotNull(ev.checkedAt, ev.sourcePublishedAt, ev.sourceUpdatedAt, ev.validUntil).forEach { d ->
                if (!DATE.matches(d)) add("근거 ${ev.id}의 날짜 형식 오류: $d")
            }
            if (data.dataKind == DataKind.FICTIONAL_SAMPLE && ev.sourceType != SourceType.FICTIONAL)
                add("가상 데이터 파일에 실제 출처 근거가 섞였습니다: ${ev.id}")
            if (data.dataKind != DataKind.FICTIONAL_SAMPLE && ev.sourceType == SourceType.FICTIONAL)
                add("실제 데이터 파일에 가상 근거가 섞였습니다: ${ev.id}")
        }

        duplicates(data.sites.map { it.id }).forEach { add("중복 구역 ID: $it") }
        for (site in data.sites) {
            val allIds = site.destinations.map { it.id } + site.points.map { it.id } +
                site.parkingFacilities.map { it.id } + site.pedestrianConnections.map { it.id } + site.vehicleConnections.map { it.id }
            duplicates(allIds).forEach { add("${site.id}: 중복 ID $it") }

            val points = site.points.associateBy { it.id }
            val destIds = site.destinations.map { it.id }.toSet()
            val facilityIds = site.parkingFacilities.map { it.id }.toSet()

            fun checkFact(owner: String, fact: Fact?, required: Boolean) {
                if (fact == null) return
                fact.evidenceIds.forEach { if (it !in evidenceById) add("$owner: 존재하지 않는 근거 $it") }
                if (fact.status == FactStatus.KNOWN && fact.value == null) add("$owner: 확인된 사실에 값이 없습니다")
                if (required && fact.status == FactStatus.KNOWN && fact.evidenceIds.isEmpty()) add("$owner: 확인된 사실의 근거가 없습니다")
            }

            for (p in site.points) {
                p.coordinate?.let { if (!validCoordinate(it)) add("${p.id}: 좌표 범위 오류 $it") }
                p.sdkTestCoordinate?.let { if (!validCoordinate(it)) add("${p.id}: 시험 좌표 범위 오류 $it") }
                if (data.dataKind == DataKind.FICTIONAL_SAMPLE && p.coordinate != null)
                    add("${p.id}: 가상 데이터에는 실제 접근 좌표를 넣지 않습니다(sdkTestCoordinate 사용)")
                if (p.role in setOf(PointRole.PARKING_ENTRANCE, PointRole.PARKING_PEDESTRIAN_EXIT)) {
                    if (p.parkingFacilityId == null || p.parkingFacilityId !in facilityIds)
                        add("${p.id}: 주차시설 참조 오류 ${p.parkingFacilityId}")
                }
                checkFact("${p.id}.vehicleAccess", p.vehicleAccess, true)
                checkFact("${p.id}.dropoffAllowed", p.dropoffAllowed, true)
            }
            site.parkingFacilities.forEach { checkFact("${it.id}.visitorParking", it.visitorParking, true) }

            for (c in site.pedestrianConnections) {
                val from = points[c.fromPointId]
                if (from == null) add("${c.id}: 출발 지점 없음 ${c.fromPointId}")
                else if (from.role !in setOf(PointRole.DROPOFF, PointRole.PARKING_PEDESTRIAN_EXIT))
                    add("${c.id}: 보행 연결은 하차점 또는 주차 보행 출구에서 시작해야 합니다(${from.role})")
                if (c.toDestinationId !in destIds) add("${c.id}: 방문 장소 없음 ${c.toDestinationId}")
                if (c.walkDistanceMeters != null && c.walkDistanceMeters < 0) add("${c.id}: 음수 거리")
                checkFact("${c.id}.stepFree", c.stepFree, true)
                checkFact("${c.id}.covered", c.covered, false)
            }
            for (v in site.vehicleConnections) {
                val from = points[v.fromPointId]; val to = points[v.toPointId]
                if (from == null || to == null) add("${v.id}: 지점 참조 오류")
                else if (from.role != PointRole.DROPOFF || to.role != PointRole.PARKING_ENTRANCE)
                    add("${v.id}: 차량 연결은 하차점에서 주차 진입점으로 이어져야 합니다")
                checkFact("${v.id}.allowed", v.allowed, true)
            }
        }
    }

    fun validCoordinate(c: Coordinate) =
        c.latitude.isFinite() && c.longitude.isFinite() && c.latitude in -90.0..90.0 && c.longitude in -180.0..180.0 &&
            !(c.latitude == 0.0 && c.longitude == 0.0)

    private fun duplicates(ids: List<String>) = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
