package app.naerio.domain

/** 판정과 정렬은 분리한다. 점수로 필수 조건 위반을 상쇄하지 않는다. */
data class RankedPlans(
    val eligible: List<ArrivalPlan>,
    val needsVerification: List<ArrivalPlan>,
    val ineligible: List<ArrivalPlan>,
) {
    val recommended: ArrivalPlan? get() = eligible.firstOrNull()
    /** 1위와 근거가 같은 후보가 있으면 유일한 최적이라고 주장하지 않는다. */
    val tiedAtTop: Boolean get() = eligible.size >= 2 && eligible[0].preferredSatisfied() == eligible[1].preferredSatisfied() &&
        (eligible[0].walkDistance() == null || eligible[0].walkDistance() == eligible[1].walkDistance())
}

fun ArrivalPlan.preferredSatisfied(): Int =
    checks.count { !it.required && it.result == RequirementResult.SATISFIED }

/** 비교 가능한 실제 보행 거리. 하나라도 모르면 null. */
fun ArrivalPlan.walkDistance(): Int? {
    val legs = listOfNotNull(passengerWalk, driverWalk)
    if (legs.isEmpty() || legs.any { it.walkDistanceMeters == null }) return null
    return (passengerWalk ?: driverWalk)!!.walkDistanceMeters
}

class RankEligiblePlans {
    fun rank(plans: List<ArrivalPlan>): RankedPlans {
        val eligible = plans.filter { it.eligibility == PlanEligibility.ELIGIBLE }
        val allHaveDistance = eligible.isNotEmpty() && eligible.all { it.walkDistance() != null }
        // sortedWith는 안정 정렬이므로 근거가 같으면 데이터 순서를 유지한다.
        val sorted = eligible.sortedWith(
            compareByDescending<ArrivalPlan> { it.preferredSatisfied() }
                .thenBy { if (allHaveDistance) it.walkDistance()!! else 0 }
        )
        return RankedPlans(
            eligible = sorted,
            needsVerification = plans.filter { it.eligibility == PlanEligibility.NEEDS_VERIFICATION },
            ineligible = plans.filter { it.eligibility == PlanEligibility.INELIGIBLE },
        )
    }
}
