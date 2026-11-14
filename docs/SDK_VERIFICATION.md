# Pleos SDK 동작 확인 기록

환경: Pleos Connect v2.1.0 에뮬레이터(API 34, 2560x1440, density 160), NaviHelper 3.0.5, Vehicle 2.3.2. 확인일: 2026-11-15.
SDK 호출이 끝난 것을 반영 완료로 보지 않고, 요청 뒤 내비 경로를 다시 읽어 비교했다.

| 확인 항목 | 결과 | 앱에서의 처리 |
|---|---|---|
| `requestRoute`(좌표만 지정) | 동작함. 약 7초 뒤 `onRouteStarted` 수신 | `RouteStarted`를 진행 신호로 보고 2초 유예 뒤 재조회 |
| 요청 좌표와 다시 읽은 목적지 좌표 | 0.0m 차이 | 허용 오차 15m(`MATCH_TOLERANCE_METERS`)로 비교 |
| `onRoutingCompleted`, `onDestinationChanged` | 15초 안에 오지 않음 | 이 이벤트에 의존하지 않음 |
| `isRouting` | 안내 중에도 false | 안내 여부는 `naviMode == ROUTE_GUIDING`으로 판정 |
| 경로가 없을 때 `getDestinationInfo` | 위도·경도 0,0과 빈 이름 | 0,0을 "목적지 없음"으로 처리 |
| 경유지(B) + 목적지(P) 요청 | 목적지는 좌표까지 일치. 경유지는 이름("하차 후보 B")과 남은 거리만 오고 좌표는 0,0 | 경유지는 이름과 순서로 반영을 확인(`WAYPOINT_FLOW_VERIFIED = true`) |
| 지도 타일 | 에뮬레이터에서 그려지지 않음 | 반영 확인은 내비 목적지·경유지 목록과 앱의 재조회 결과로 보여 줌 |
| 주행 제한(CarUxRestrictions) | D단 62km/h에서 제한(값 80), P단 0km/h에서 허용 | 제한 중이거나 상태를 알 수 없으면 경로 변경을 막음 |

증거 화면: `docs/evidence/sdk_A_single_navi.png`, `sdk_B_single_diag.png`, `sdk_B_waypoint_P_navi.png`, `sdk_diag_log.png`.

## 한계

- 경유지 위치 자체는 SDK가 좌표를 돌려주지 않아 검증하지 못한다. 이름과 순서만 확인한다.
- Vehicle SDK(도어, 좌석, 기어)는 2차 PT 단계에서 연동할 예정이며 아직 앱 기능에 쓰지 않는다.
