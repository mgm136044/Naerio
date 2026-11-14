package app.naerio.domain

/**
 * 방문 장소, 방문 방식, 사용자 조건으로 도착 계획 후보를 만든다.
 * 같은 구역에 있다는 이유로 임의의 하차점과 주차장을 조합하지 않고, 연결 기록을 확인한다.
 */
class BuildArrivalPlans {

    fun build(site: Site, destinationId: String, mode: VisitMode, conditions: UserConditions): List<ArrivalPlan> {
        val destination = site.destinations.firstOrNull { it.id == destinationId } ?: return emptyList()
        val dropoffs = site.points.filter { it.role == PointRole.DROPOFF }
        val parkingEntrances = site.points.filter { it.role == PointRole.PARKING_ENTRANCE }

        return when (mode) {
            VisitMode.DROPOFF_AND_LEAVE -> dropoffs.map { d ->
                val walk = walkFrom(site, d.id, destination.id)
                val checks = dropoffChecks(d, walk, destination, conditions, passenger = true)
                plan(mode, destination, d, null, null, walk, null, checks)
            }

            VisitMode.PARK_AND_VISIT -> parkingEntrances.map { e ->
                val facility = site.parkingFacilities.firstOrNull { it.id == e.parkingFacilityId }
                val driverWalk = parkingWalk(site, facility, destination.id)
                val checks = parkingChecks(e, facility, driverWalk, destination, conditions, walkerIsPassenger = false, appliesToWalker = true)
                plan(mode, destination, null, e, facility, null, driverWalk, checks)
            }

            VisitMode.DROPOFF_THEN_PARK -> dropoffs.flatMap { d ->
                parkingEntrances.map { e ->
                    val facility = site.parkingFacilities.firstOrNull { it.id == e.parkingFacilityId }
                    val passengerWalk = walkFrom(site, d.id, destination.id)
                    val driverWalk = parkingWalk(site, facility, destination.id)
                    val link = site.vehicleConnections.firstOrNull { it.fromPointId == d.id && it.toPointId == e.id }
                    val checks = buildList {
                        addAll(dropoffChecks(d, passengerWalk, destination, conditions, passenger = true))
                        add(
                            Check(
                                label = "하차 후 주차장 이동",
                                required = true,
                                result = link?.allowed.toResult(),
                                reason = when (link?.allowed.toResult()) {
                                    RequirementResult.SATISFIED -> "${d.name}에서 ${e.name}(으)로 차량 이동이 확인되었습니다."
                                    RequirementResult.NOT_SATISFIED -> "${d.name}에서 ${e.name}(으)로 차량이 이동할 수 없습니다."
                                    else -> "${d.name}에서 ${e.name}(으)로 이어지는 차량 연결을 확인할 수 없습니다."
                                },
                                evidenceIds = link?.allowed?.evidenceIds.orEmpty(),
                            )
                        )
                        addAll(
                            parkingChecks(
                                e, facility, driverWalk, destination, conditions,
                                walkerIsPassenger = false,
                                appliesToWalker = conditions.stepFreeAppliesTo == AppliesTo.EVERYONE,
                            )
                        )
                    }
                    plan(mode, destination, d, e, facility, passengerWalk, driverWalk, checks)
                }
            }
        }
    }

    private fun walkFrom(site: Site, pointId: String, destinationId: String) =
        site.pedestrianConnections.firstOrNull { it.fromPointId == pointId && it.toDestinationId == destinationId }

    /** 주차 진입점이 아니라 주차시설의 보행 출구에서 시작하는 연결을 찾는다. */
    private fun parkingWalk(site: Site, facility: ParkingFacility?, destinationId: String): PedestrianConnection? {
        if (facility == null) return null
        val exits = site.points.filter { it.role == PointRole.PARKING_PEDESTRIAN_EXIT && it.parkingFacilityId == facility.id }
        return exits.firstNotNullOfOrNull { walkFrom(site, it.id, destinationId) }
    }

    private fun dropoffChecks(
        d: Point, walk: PedestrianConnection?, dest: VisitDestination, c: UserConditions, passenger: Boolean,
    ): List<Check> = buildList {
        add(factCheck("차량 접근", true, d.vehicleAccess, "${d.name}까지 차량 접근이"))
        add(factCheck("하차 가능", true, d.dropoffAllowed, "${d.name}에서 하차가"))
        if (walk == null) {
            add(Check("보행 연결", true, RequirementResult.UNVERIFIED, "${d.name}에서 ${dest.name}(으)로 가는 보행 연결 정보가 없습니다."))
        } else {
            add(Check("보행 연결", true, RequirementResult.SATISFIED, "${d.name}에서 ${dest.name}: ${walk.guidance}"))
            stepFreeCheck(walk, dest, c, applies = passenger, who = "내리는 동승자")?.let(::add)
            coveredCheck(walk, c)?.let(::add)
        }
    }

    private fun parkingChecks(
        e: Point, facility: ParkingFacility?, walk: PedestrianConnection?, dest: VisitDestination,
        c: UserConditions, walkerIsPassenger: Boolean, appliesToWalker: Boolean,
    ): List<Check> = buildList {
        add(factCheck("차량 접근", true, e.vehicleAccess, "${e.name}까지 차량 접근이"))
        if (facility == null) {
            add(Check("방문객 주차", true, RequirementResult.UNVERIFIED, "${e.name}이(가) 어느 주차시설에 속하는지 확인되지 않았습니다."))
        } else {
            add(
                Check(
                    "방문객 주차", true, facility.visitorParking.toResult(),
                    when (facility.visitorParking.toResult()) {
                        RequirementResult.SATISFIED -> "${facility.name}은(는) 방문객 이용이 확인되었습니다." +
                            (facility.visitorCondition?.let { " 조건: $it" } ?: "")
                        RequirementResult.NOT_SATISFIED -> "${facility.name}은(는) 방문객이 이용할 수 없습니다."
                        else -> "${facility.name}의 방문객 이용 여부가 ${facility.visitorParking.statusPhrase()}"
                    },
                    facility.visitorParking.evidenceIds,
                )
            )
        }
        if (walk == null) {
            add(Check("주차 후 보행 연결", true, RequirementResult.UNVERIFIED, "주차시설 보행 출구에서 ${dest.name}(으)로 가는 연결 정보가 없습니다."))
        } else {
            add(Check("주차 후 보행 연결", true, RequirementResult.SATISFIED, "주차시설 보행 출구에서 ${dest.name}: ${walk.guidance}"))
            val who = if (walkerIsPassenger) "동승자" else "주차 후 걷는 사람"
            stepFreeCheck(walk, dest, c, applies = appliesToWalker, who = who)?.let(::add)
            coveredCheck(walk, c)?.let(::add)
        }
    }

    private fun stepFreeCheck(walk: PedestrianConnection, dest: VisitDestination, c: UserConditions, applies: Boolean, who: String): Check? {
        if (c.stepFree == Strength.OFF) return null
        if (!applies) {
            return Check("계단 없는 연결($who)", false, RequirementResult.NOT_APPLICABLE, "계단 없는 연결 조건은 이 이동에 적용하지 않았습니다.")
        }
        val r = walk.stepFree.toResult()
        val reason = when (r) {
            RequirementResult.SATISFIED -> "${dest.name}까지 계단 없는 연결이 확인되었습니다."
            RequirementResult.NOT_SATISFIED -> "${dest.name}까지 계단을 거쳐야 합니다."
            else -> "${dest.name}까지 계단 없는 연결이 ${walk.stepFree.statusPhrase()}"
        }
        return Check("계단 없는 연결($who)", c.stepFree == Strength.REQUIRED, r, reason, walk.stepFree.evidenceIds)
    }

    private fun coveredCheck(walk: PedestrianConnection, c: UserConditions): Check? {
        if (c.covered == Strength.OFF) return null
        val r = walk.covered.toResult()
        val reason = when (r) {
            RequirementResult.SATISFIED -> "지붕이 있는 연결입니다."
            RequirementResult.NOT_SATISFIED -> "지붕이 없는 구간이 있습니다."
            else -> "지붕 여부가 ${walk.covered.statusPhrase()}"
        }
        return Check("지붕 있는 연결", c.covered == Strength.REQUIRED, r, reason, walk.covered.evidenceIds)
    }

    private fun factCheck(label: String, required: Boolean, fact: Fact?, subject: String): Check {
        val r = fact.toResult()
        val reason = when (r) {
            RequirementResult.SATISFIED -> "$subject 확인되었습니다."
            RequirementResult.NOT_SATISFIED -> "$subject 허용되지 않습니다."
            else -> "$subject ${fact.statusPhrase()}"
        }
        return Check(label, required, r, reason, fact?.evidenceIds.orEmpty())
    }

    private fun plan(
        mode: VisitMode, dest: VisitDestination, d: Point?, e: Point?, f: ParkingFacility?,
        pw: PedestrianConnection?, dw: PedestrianConnection?, checks: List<Check>,
    ) = ArrivalPlan(
        id = listOfNotNull(mode.name, d?.id, e?.id).joinToString(":"),
        mode = mode, destination = dest, dropoff = d, parkingEntrance = e, parkingFacility = f,
        passengerWalk = pw, driverWalk = dw, checks = checks, eligibility = eligibilityOf(checks),
    )
}
