# Naerio (내리오)

**차에서 어디서 내리고 어디에 주차할지, 도착 전에 정해 내비로 보내는 Pleos Connect 앱**

방문할 동(건물), 방문 방식(주차하고 방문, 내려준 뒤 주차, 내려주고 떠나기), 이동 조건(계단 없이, 지붕 아래로)을 고르면,
확인된 근거로 조건을 충족하는 하차점과 주차 진입구 조합을 추천하고 정차 중에 Pleos 내비 경로(경유지와 목적지)로 보냅니다.

> 현재 장소 데이터는 **시연용 가상 데이터**(가상 A단지)입니다. 실제 조사한 장소가 아니며 실제 운전 안내에 사용할 수 없습니다.

## APK 위치

- `apk/Naerio_Naerio_release.apk` (release 빌드, APK Signature Scheme v3 서명, 패키지 `app.naerio`)

## 실행 방법

1. Pleos Connect 에뮬레이터를 켭니다(아래 테스트 환경 참고).
2. APK를 설치하고 실행합니다.
   ```bash
   adb install -r apk/Naerio_Naerio_release.apk
   adb shell am start -n app.naerio/.MainActivity
   ```
   앱 서랍에서 **Naerio**를 눌러 실행해도 됩니다.
3. 첫 실행 안내(4장)를 보거나 건너뜁니다. 상단 ? 버튼으로 다시 볼 수 있습니다.
4. 103동 → 내려준 뒤 주차 → 후보 보기 → 추천 후보 선택 → 다음 → **내비에 적용**.
   결과 카드에 "내비 반영 확인"이 나오면 성공입니다.

### 화면 상단의 두 모드

- **모의 내비**(기본): 내비 반영을 시뮬레이션합니다. 에뮬레이터 설정 없이 전체 흐름을 볼 수 있습니다. 확인 화면의 "시연 설정"에서 오류, 무응답, 경유지 누락, 주행 제한 상황을 재현할 수 있습니다.
- **실제 SDK**: 에뮬레이터의 Pleos 내비에 SDK 시험 좌표를 경유지(하차점)와 목적지(주차 진입구)로 보냅니다. 내비 앱에서 경유지와 목적지 목록으로 결과를 확인할 수 있습니다.

## 테스트 환경 및 필요한 설정

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect v2.1.0 (Android Automotive, API 34, arm64-v8a) |
| 화면 규격 | 2560x1440, density 160 (앱 창은 오른쪽 Application window, 약 1706x1210dp) |
| 개발 도구 | Android Studio, JDK 17 이상(Android Studio 내장 JBR 21 사용) |
| Pleos SDK | Vehicle 2.3.2, NaviHelper 3.0.5 (`https://nexus-playground.pleos.ai/repository/maven-releases/`) |
| 권한 | `pleos.car.permission.NAVI_ROUTE`, `NAVI_ROUTE_SEARCH` 선언(개발 단계는 선언만으로 사용) |

- **경로 적용은 정차 중에만 됩니다.** Virtual Console(`http://127.0.0.1:8088/`, `adb forward tcp:8088 tcp:8088` 필요)에서 기어를 P, 속도 0으로 두세요. D단 주행 중에는 "주행 중, 적용할 수 없음"으로 막힙니다.
- **실제 SDK** 모드는 에뮬레이터에 Pleos 내비 서비스가 실행 중이어야 합니다. 지도 타일은 에뮬레이터에서 그려지지 않으니 내비의 경유지·목적지 목록으로 확인하세요.
- `scripts/start-emulator.sh`: 에뮬레이터를 켜고 부팅을 기다린 뒤 Virtual Console 포트를 연결합니다(AVD 이름 `Pleos_Connect_v2.1.0` 기준).

## 소스에서 빌드

```bash
./gradlew :app:testDebugUnitTest   # 단위 테스트 30개
./gradlew :app:installDebug        # 디버그 빌드 설치(개발용 진단 화면 포함)
./gradlew exportReleaseApk         # release APK → apk/Naerio_Naerio_release.apk
```
release 서명 값은 저장소에 없습니다. `~/.gradle/gradle.properties`에 `NAERIO_STORE_FILE`, `NAERIO_STORE_PASSWORD`, `NAERIO_KEY_ALIAS`, `NAERIO_KEY_PASSWORD`를 넣어야 release 빌드가 됩니다.

## 핵심 원칙

- 확인된 근거가 있는 조건만 "충족"으로 표시하고, 앱이 조건을 임의로 완화하지 않습니다.
- 주행 제한(CarUxRestrictions)이 있거나 상태를 알 수 없으면 경로를 바꾸지 않습니다.
- 내비 요청이 끝난 것을 반영 완료로 보지 않습니다. 보낸 뒤 내비 경로를 다시 읽어 일치할 때만 "반영 확인"으로 표시합니다. 확인 기록은 `docs/SDK_VERIFICATION.md`에 있습니다.

## 구조

```
app/src/main/java/app/naerio/
  domain/      후보 계산(조건 평가, 도착 계획 생성, 순위)
  data/        장소 데이터 읽기와 검증
  navigation/  NaviHelper 연동, 모의 내비, 요청 후 재확인(coordinator)
  vehicle/     주행 제한 정책
  ui/          화면(첫 실행 안내 포함), Pleos 디자인 토큰과 컴포넌트
app/src/main/assets/places/demo_site.json   시연용 가상 장소 데이터
docs/SDK_VERIFICATION.md                    SDK 동작 확인 기록
docs/evidence/                              에뮬레이터 실험과 화면 캡처
docs/design/NAERIO_UI_RULES.md              UI 규칙
insane-design/pleos/                        Pleos 디자인 시스템 분석(design.md, report.ko.html)
```

## 디자인

Pleos Connect 디자인 가이드(Asta Sans, basic grayscale, Pleos 팔레트)를 기준으로 했습니다.
경로는 실선(차)과 점선(도보) 도식으로 보여 주고, 설명은 첫 실행 안내로 옮겼습니다.
