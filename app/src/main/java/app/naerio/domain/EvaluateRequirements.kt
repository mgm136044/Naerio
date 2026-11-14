package app.naerio.domain

/** 사실 하나를 사용자 조건 충족 여부로 바꾼다. 미확인, 충돌, 재확인 필요는 충족으로 처리하지 않는다. */
fun Fact?.toResult(): RequirementResult = when {
    this == null -> RequirementResult.UNVERIFIED
    status == FactStatus.KNOWN && value == true -> RequirementResult.SATISFIED
    status == FactStatus.KNOWN && value == false -> RequirementResult.NOT_SATISFIED
    else -> RequirementResult.UNVERIFIED
}

fun Fact?.statusPhrase(): String = when (this?.status) {
    null, FactStatus.UNKNOWN -> "확인되지 않았습니다"
    FactStatus.CONFLICTING -> "안내가 서로 달라 확인이 필요합니다"
    FactStatus.RECHECK_REQUIRED -> "재확인이 필요합니다"
    FactStatus.KNOWN -> if (value == true) "확인되었습니다" else "해당하지 않는 것으로 확인되었습니다"
}

/** 필수 확인 항목으로 계획의 적격성을 정한다. 선호 조건은 적격성에 영향을 주지 않는다. */
fun eligibilityOf(checks: List<Check>): PlanEligibility {
    val required = checks.filter { it.required }
    return when {
        required.any { it.result == RequirementResult.NOT_SATISFIED } -> PlanEligibility.INELIGIBLE
        required.any { it.result == RequirementResult.UNVERIFIED } -> PlanEligibility.NEEDS_VERIFICATION
        else -> PlanEligibility.ELIGIBLE
    }
}
