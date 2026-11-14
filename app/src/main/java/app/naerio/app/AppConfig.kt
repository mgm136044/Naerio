package app.naerio.app

/** 실제 SDK 검증 결과에 따라 켜는 기능 스위치. 근거는 docs/SDK_VERIFICATION.md */
object VerifiedCapabilities {
    /**
     * 하차 경유지 + 주차 목적지 요청이 Pleos 에뮬레이터에서 실제로 유지되는 것을 확인했는가.
     * 2026-11-15 확인: 경유지 이름과 순서, 목적지 좌표 유지. 단, 경유지 좌표는 SDK 조회에서 0,0으로 돌아와 이름으로만 대조한다.
     */
    const val WAYPOINT_FLOW_VERIFIED = true
}
