---
schema_version: 3.2
slug: pleos
service_name: Pleos (Pleos Connect)
site_url: https://pleos.ai/?locale=ko-KR
fetched_at: 2026-11-15
default_theme: mixed
brand_color: "#2350FF"
primary_font: Asta Sans
font_weight_normal: 400
token_prefix: "--color-pleos-"

bold_direction: "Calm Cockpit"
aesthetic_category: "other"
signature_element: minimal_extreme
code_complexity: medium

medium: design-system
medium_confidence: medium

archetype: automotive
archetype_confidence: high
design_system_level: lv3
design_system_level_evidence: "pleos.ai CSS에 --color-pleos-* 20개 named palette, Pleos Connect 공식 디자인 가이드에 basic_00~900 grayscale + background/button/surface/text system token 2계층과 Headline/Title/Body/Label 위계가 명시됨"

colors:
  pleos-black: "#111111"
  pleos-white: "#FFFFFF"
  pleos-blue-1: "#CDDCFF"
  pleos-blue-2: "#5087FF"
  pleos-blue-3: "#2350FF"
  pleos-blue-4: "#0F235A"
  pleos-green-1: "#BEFFE1"
  pleos-green-2: "#69F5B9"
  pleos-green-3: "#00D791"
  pleos-green-4: "#053C32"
  pleos-red-1: "#FFD7DC"
  pleos-red-2: "#FF96A0"
  pleos-red-3: "#FA293C"
  pleos-red-4: "#5F0F14"
  pleos-light-gray-1: "#F5F5F5"
  pleos-light-gray-2: "#DCDCDC"
  pleos-light-gray-3: "#B9B9B9"
  pleos-dark-gray-1: "#373737"
  pleos-dark-gray-2: "#5F5F5F"
  pleos-dark-gray-3: "#949494"
  basic-00: "#FFFFFF"
  basic-50: "#F7F8FA"
  basic-100: "#EDEEF2"
  basic-200: "#E0E1E6"
  basic-300: "#C7C9CE"
  basic-400: "#A4A8AE"
  basic-500: "#686A72"
  basic-600: "#44464E"
  basic-700: "#313236"
  basic-800: "#1D1E21"
  basic-900: "#131417"

typography:
  display: "Asta Sans"
  body: "Asta Sans"
  ladder:
    - { token: headline-l, size: "60px", weight: 800 }
    - { token: headline-m, size: "56px", weight: 800 }
    - { token: headline-s, size: "48px", weight: 700 }
    - { token: title-l, size: "40px", weight: 700 }
    - { token: title-m, size: "36px", weight: 700 }
    - { token: title-s, size: "32px", weight: 700 }
    - { token: body-l, size: "30px", weight: 400 }
    - { token: body-m, size: "28px", weight: 400 }
    - { token: body-s, size: "26px", weight: 400 }
    - { token: label, size: "24px", weight: 500 }
  weights_used: [300, 400, 500, 600, 700, 800]
  weights_absent: [100, 200, 900]

components:
  button-filled: { bg: "{colors.basic-600}", pressed: "{colors.basic-700}", fg: "{colors.basic-00}", radius: "20px", min-height: "88px" }
  button-basic: { bg: "{colors.basic-100}", pressed: "{colors.basic-200}", fg: "{colors.basic-900}", radius: "20px", min-height: "76px" }
  segmented: { container: "{colors.basic-100}", indicator: "{colors.basic-00}", radius: "20px", height: "76px" }
  card: { bg: "{colors.basic-00}", radius: "24px", selected-border: "{colors.pleos-blue-3}" }
  status-chip-ok: { bg: "{colors.pleos-green-1}", fg: "{colors.pleos-green-4}" }
  status-chip-no: { bg: "{colors.pleos-red-1}", fg: "{colors.pleos-red-4}" }
  status-chip-unknown: { bg: "{colors.basic-100}", fg: "{colors.basic-600}" }
---

# Pleos / Pleos Connect Design System

## 00. Visual Theme & Atmosphere
<!-- SOURCE: manual -->

Pleos는 두 얼굴을 가진다. 마케팅 사이트 pleos.ai는 순흑 `#000` 위에 초록, 파랑, 빨강 빛줄기가 교차하는 영상 히어로와 흰 워드마크로 "소프트웨어 정의 차량"의 에너지를 보여준다. 반면 차 안의 Pleos Connect는 정반대로 조용하다. 흰 카드와 옅은 회색 바탕, 짙은 회색 버튼, 그리고 꼭 필요할 때만 한 번 쓰는 파랑 하나. 운전자가 1~2초 안에 읽고 다시 도로를 보게 만드는 것이 이 시스템의 목적이다.

차량용 앱에 적용할 때 기준은 Connect 쪽이다. 마케팅의 빛줄기는 앱에 가져오지 않는다. 대신 pleos.ai 팔레트(`--color-pleos-*`)에서 상태 색 세 가지(파랑 선택, 초록 확인, 빨강 경고)를 빌려 오고, 나머지는 Connect grayscale(basic_00~900)로 채운다. 결과는 "조용한 조종석": 정보는 크게, 장식은 없게, 색은 의미가 있을 때만.

- 바탕은 `#F7F8FA` (`{colors.basic-50}`), 카드는 `#FFFFFF` (`{colors.basic-00}`), 본문은 `#131417` (`{colors.basic-900}`).
- 주 행동 버튼은 `#44464E` (`{colors.basic-600}`) 채움. 파랑이 아니다.
- 파랑 `#2350FF` (`{colors.pleos-blue-3}`)은 선택 표시, 추천 표시, 포커스에만 쓴다.
- 서체는 Asta Sans 하나. Pleos Connect 시스템 sans-serif가 이미 Asta Sans로 교체되어 있다.

> **BOLD Direction**: Calm Cockpit (조용한 조종석)
> **Aesthetic Category**: other (Automotive Monochrome + semantic accent)
> **Signature Element**: minimal_extreme, 회색 계열 8:2에 의미 있는 색 1개
> **Code Complexity**: medium, Compose 토큰 계층과 상태 칩, 경로 도식 컴포넌트

**Key Characteristics**
- 회색 단계(basic_00~900)가 화면의 90%를 만든다.
- 상태는 색과 기호를 함께 쓴다 (색맹, 야간, 눈부심 대응).
- 큰 터치 영역 76dp 이상, 버튼 사이 23dp 이상.
- 누르면 95%로 줄어드는 촉각형 피드백 외에는 움직임이 없다.

## 01. Quick Start
<!-- SOURCE: manual -->

```kotlin
// 1) 서체: 기기 sans-serif가 Asta Sans. 별도 번들 불필요.
val Asta = FontFamily.SansSerif
// 2) 바탕과 글자
val Bg = Color(0xFFF7F8FA)      // {colors.basic-50}
val Surface = Color(0xFFFFFFFF) // {colors.basic-00}
val Ink = Color(0xFF131417)     // {colors.basic-900}
// 3) 의미 색 하나
val Accent = Color(0xFF2350FF)  // {colors.pleos-blue-3}
```

세 가지만 지키면 80%가 맞는다: 회색 바탕 위 흰 카드, 짙은 회색 주 버튼, 파랑은 선택 표시에만.

## 02. Provenance
<!-- SOURCE: auto -->

| 항목 | 값 |
|---|---|
| 1차 소스 | https://pleos.ai/?locale=ko-KR (Next.js, CSS 3개 224KB, 2026-11-15 수집) |
| 2차 소스 | Pleos Connect 개발자 가이드 디자인 섹션 (document.pleos.ai/docs/connect/guide/docs-design/): Colors, Typography, Design Token, Theme, Basic button, Layout, Designing apps for driving, Shadow, Segmented menu, Interaction |
| 3차 소스 | Pleos Connect v2.1.0 에뮬레이터 실화면 (설정, 홈, 시스템 UI), /product/etc/fonts_customization.xml |
| 보조 기준 | Android Design for Driving (docs.partner.android.com/drivingux): 터치 76x76dp, 간격 23dp, 본문 최소 24dp, 대비 4.5:1, 2초 판독 |
| 추출 스크립트 | var_resolver (CSS 변수 197개 중 94개 해결), brand_candidates (후보 51개), typo_extractor, alias_layer |
| 스크린샷 | screenshots/hero-cropped.png, section-1.png, section-2.png |

`--color-pleos-*` 20개는 pleos.ai CSS에 실재한다. `basic-*` grayscale 11개는 CSS가 아니라 공식 가이드의 Grayscale 표(guide/grayscale.png)에서 읽은 값이다. 두 출처를 섞지 않도록 토큰 이름을 분리했다.

## 03. Tech Stack
<!-- SOURCE: auto+manual -->

- 마케팅 사이트: Next.js App Router, Tailwind v4 (`--tw-*`, `--radius-sm: .25rem`, `--ease-in-out: cubic-bezier(.4,0,.2,1)`), CSS Modules 해시 클래스(`.TMO4jG_button`, `._rx1Ma_eyebrow`), next/font preload로 `AstaSans_variable.woff2`.
- 차량 OS: Pleos Connect (Android Automotive 기반, API 34). 앱 창은 오른쪽 Application window. 2560x1440 디스플레이, density 160 (1dp = 1px).
- 시스템 서체: `fonts_customization.xml`이 `sans-serif` 패밀리를 AstaSansOTF Light/Regular/Medium/Bold/ExtraBold로 재정의. 600은 Bold 파일, 700 이상은 ExtraBold 파일로 매핑된다.
- 적용 대상(Naerio, 내리오): Jetpack Compose + Material3 (BOM 2024.12.01). CSS 대신 Kotlin 토큰 객체로 옮긴다.

## 04. Font Stack
<!-- SOURCE: auto+manual -->

| 용도 | 패밀리 | 비고 |
|---|---|---|
| 전체 | Asta Sans | pleos.ai `--font-asta-sans: "astaSans"`, 차량 시스템 sans-serif |
| 대체 | Noto Sans CJK | 기기 기본 CJK 대체 |
| 숫자 | Asta Sans (tabular 아님) | 거리, 시간은 굵기로 강조 |

Asta Sans는 42dot이 만든 한글/라틴 서체로, 캐릭터가 강하지 않고 균일하다. 가이드는 "캐릭터가 강조된 폰트를 지양"하라고 명시한다. 두 번째 서체를 섞지 않는다.

## 05. Typography Scale
<!-- SOURCE: css+manual -->

Pleos Connect 가이드 위계 (단위 px, density 160에서 dp와 같음).

| 토큰 | 크기 | 굵기 | Naerio 용도 |
|---|---|---|---|
| headline-l | 60 | ExtraBold, Bold | 사용 안 함 |
| headline-m | 56 | ExtraBold, Bold | 사용 안 함 |
| headline-s | 48 | ExtraBold, Bold | 동 이름 타일 (103동) |
| title-l | 40 | ExtraBold, Bold, Regular | 화면 제목 |
| title-m | 36 | ExtraBold, Bold, Regular | 섹션 제목, 앱 이름 |
| title-s | 32 | ExtraBold, Bold, Regular | 카드 제목, 결정용 1차 텍스트 |
| body-l | 30 | Strong, Normal | 핵심 이유 문장 |
| body-m | 28 | Strong, Normal | 본문 |
| body-s | 26 | Strong, Normal | 보조 설명 |
| label | 24 | Medium | 칩, 근거 출처, 메타 |

pleos.ai 웹은 별도 스케일(15/18/24/28/36px, line-height 22/26/32/44px, `letter-spacing: -.01em` 30회)을 쓴다. 웹 스케일은 앱에 쓰지 않는다. 다만 큰 제목의 `-0.01em` 자간 보정은 앱 제목에도 적용한다.

## 06. Colors
<!-- SOURCE: css -->

### 06-1. Pleos Brand Palette (pleos.ai CSS 실재)

| 토큰 | hex | 역할 |
|---|---|---|
| pleos-blue-1 | #CDDCFF | 선택 배경 틴트 |
| pleos-blue-2 | #5087FF | 야간 강조 |
| pleos-blue-3 | #2350FF | 주간 강조, 선택 테두리, eyebrow (`._rx1Ma_eyebrow{color:#2350ff}`) |
| pleos-blue-4 | #0F235A | 틴트 위 글자 |
| pleos-green-1 | #BEFFE1 | 확인됨 칩 배경 |
| pleos-green-2 | #69F5B9 | 야간 확인됨 글자 |
| pleos-green-3 | #00D791 | 아이콘 전용 (흰 바탕 글자 금지) |
| pleos-green-4 | #053C32 | 확인됨 칩 글자 |
| pleos-red-1 | #FFD7DC | 미충족 칩 배경 |
| pleos-red-2 | #FF96A0 | 야간 미충족 글자 |
| pleos-red-3 | #FA293C | 경고 아이콘, 경고 테두리 |
| pleos-red-4 | #5F0F14 | 미충족 칩 글자 |
| pleos-black | #111111 | 웹 버튼 (`.TMO4jG_button{background:#111}`) |

### 06-2. Pleos Connect Grayscale (공식 가이드)

| 토큰 | Light | Dark |
|---|---|---|
| basic_00 | #FFFFFF | #131417 |
| basic_50 | #F7F8FA | #1D1E21 |
| basic_100 | #EDEEF2 | #313236 |
| basic_200 | #E0E1E6 | #44464E |
| basic_300 | #C7C9CE | #686A72 |
| basic_400 | #A4A8AE | #A4A8AE |
| basic_500 | #686A72 | #C7C9CE |
| basic_600 | #44464E | #E0E1E6 |
| basic_700 | #313236 | #EDEEF2 |
| basic_800 | #1D1E21 | #F7F8FA |
| basic_900 | #131417 | #FFFFFF |

### 06-3. System Token 매핑 (가이드 표)

| System token | Light | Dark |
|---|---|---|
| background_primary | basic_50 | basic_50 |
| background_secondary | basic_00 | basic_00 |
| background_popup | basic_00 | basic_100 |
| button_filled_enabled | basic_600 | basic_600 |
| button_filled_pressed | basic_700 | basic_500 |
| button_basic_enabled | alpha_dark_50 | alpha_dark_50 |

### 06-4. 대비 검증 (WCAG)

| 조합 | 대비 |
|---|---|
| #131417 on #FFFFFF | 18.4:1 |
| #686A72 on #F7F8FA | 5.1:1 |
| #2350FF on #FFFFFF | 5.7:1 |
| #FFFFFF on #44464E | 9.4:1 |
| #053C32 on #BEFFE1 | 10.9:1 |
| #5F0F14 on #FFD7DC | 10.2:1 |
| #FA293C on #FFFFFF | 3.9:1 (글자 금지, 아이콘과 테두리만) |
| #A4A8AE on #FFFFFF | 2.4:1 (비활성 표시 전용) |
| #5087FF on #1D1E21 | 5.0:1 |

## 07. Spacing
<!-- SOURCE: manual -->

4dp 그리드. 앱 창(약 1706x1210dp)에 맞춘 단계.

| 토큰 | 값 | 용도 |
|---|---|---|
| space-1 | 8dp | 아이콘과 글자 사이 |
| space-2 | 12dp | 칩 내부 |
| space-3 | 16dp | 카드 내부 줄 간격 |
| space-4 | 24dp | 카드 사이, 터치 대상 사이(23dp 이상) |
| space-5 | 32dp | 카드 패딩, 화면 좌우 여백 |
| space-6 | 48dp | 2단 레이아웃 열 간격 |

## 08. Radius
<!-- SOURCE: css+manual -->

| 토큰 | 값 | 근거 |
|---|---|---|
| radius-card | 24dp | Connect 가이드 SVG 카드 모서리 24 |
| radius-control | 20dp | 버튼, 세그먼트 컨테이너 |
| radius-chip | 12dp | 상태 칩 |
| radius-pill | 999dp | 단계 표시점, pleos.ai `border-radius:2147483647px` |

pleos.ai 웹은 `border-radius:10px` (5회)와 `0` (6회)이 주류다. 차 안 화면은 더 둥글다.

## 09. Shadows
<!-- SOURCE: css+manual -->

Connect 가이드는 3단계 그림자를 둔다. 앱에서는 1단계만 쓴다.

| 단계 | 값 | 용도 |
|---|---|---|
| level-1 | 0 2dp 8dp rgba(19,20,23,0.06) | 흰 카드, 세그먼트 선택 표시 |
| level-2 | 0 8dp 24dp rgba(19,20,23,0.10) | 사용 안 함 |
| level-3 | 0 20dp 25dp -5dp #0000001A | 사용 안 함 (pleos.ai `--shadow-xl`) |

## 10. Motion
<!-- SOURCE: css+manual -->

- 누름: scale 1.0에서 0.95로 (가이드 Interaction). 100ms.
- pleos.ai 곡선 `cubic-bezier(.32,.72,0,1)` 9회, `cubic-bezier(.22,1,.36,1)` 9회. 앱에서는 누름 복귀에만 쓴다.
- 운전 중 시선을 끄는 애니메이션, 자동 스크롤, 진입 연출은 금지 (가이드 "Designing apps for driving").
- 0.25초 안에 반응. 오래 걸리면 진행 표시를 보여준다.

## 11. Layout Patterns
<!-- SOURCE: auto+manual -->

- 화면 구성: 상태 표시줄, 왼쪽 Driving view, 하단 GNB는 시스템 영역. 앱은 오른쪽 Application window 하나를 쓴다.
- 앱 상단 바: 앱 이름, 4단계 진행 표시, 오른쪽에 데이터 배지와 모드 세그먼트, 진단 버튼.
- 본문: 넓은 창(1200dp 이상)에서는 2단. 왼쪽은 "결정", 오른쪽은 "근거와 조건". 좁으면 1단.
- 하단 행동 바: 고정. 왼쪽 "이전", 오른쪽 주 버튼. 스크롤에 가려지지 않는다.
- 카드 목록은 균등 그리드보다 위계 우선: 추천 1개를 크게, 나머지는 작게.
- Landscape와 Portrait 모두 대응해야 한다 (가이드 Display Layout).

## 12. Responsive Behavior
<!-- SOURCE: manual -->

| 폭 | 레이아웃 |
|---|---|
| 1200dp 이상 (Connect-S 분할, Connect-W) | 2단, 열 간격 48dp |
| 1200dp 미만 (Portrait, 좁은 분할) | 1단, 조건 패널이 아래로 |

터치 대상 76x76dp 이상, 간격 23dp 이상. 화면 가장자리에서 24dp 이상.

## 13. Components
<!-- SOURCE: auto+manual -->

### 13-1. Core

| 컴포넌트 | 스펙 |
|---|---|
| button-filled | bg `{colors.basic-600}`, pressed `{colors.basic-700}`, 글자 `{colors.basic-00}` title-s, 높이 88dp, radius 20dp, 좌우 40dp |
| button-basic | bg `{colors.basic-100}`, pressed `{colors.basic-200}`, 글자 `{colors.basic-900}`, 높이 76dp |
| button-disabled | bg `{colors.basic-100}`, 글자 `{colors.basic-400}` |
| segmented | 컨테이너 `{colors.basic-100}` radius 20dp 패딩 6dp, 선택 pill `{colors.basic-00}` + level-1 그림자, 글자 선택 `{colors.basic-900}` Bold, 비선택 `{colors.basic-500}` |
| card | bg `{colors.basic-00}`, radius 24dp, 패딩 32dp, 테두리 없음 |
| card-selected | 테두리 3dp `{colors.pleos-blue-3}`, 오른쪽 위 체크 원 `{colors.pleos-blue-3}` |
| card-disabled | bg `{colors.basic-50}`, 테두리 1dp `{colors.basic-200}` |
| status-chip ok | bg `{colors.pleos-green-1}`, 글자 `{colors.pleos-green-4}`, 기호 ✓ |
| status-chip unknown | bg `{colors.basic-100}`, 글자 `{colors.basic-600}`, 기호 ? |
| status-chip no | bg `{colors.pleos-red-1}`, 글자 `{colors.pleos-red-4}`, 기호 ✕ |
| info-banner | bg `{colors.basic-100}`, 아이콘 `{colors.basic-500}`, body-s |
| warn-banner | bg `{colors.pleos-red-1}`, 왼쪽 6dp 막대 `{colors.pleos-red-3}`, 글자 `{colors.pleos-red-4}` |

### 13-2. Named Variants

| 이름 | 설명 |
|---|---|
| step-indicator | 4개 pill. 현재 단계 `{colors.basic-900}` 채움 + 흰 글자, 지난 단계 `{colors.basic-300}`, 다음 단계 `{colors.basic-100}` |
| data-badge | `{colors.basic-100}` 배경 label 크기. "시연용 가상 데이터" 같은 고지 |
| route-strip | 경로 도식. 노드 원 56dp, 주행 구간 6dp 실선 `{colors.basic-600}`, 도보 구간 점선 `{colors.pleos-blue-3}` |
| place-node | 하차점 원 `{colors.basic-900}` 흰 글자, 주차 진입점 각진 사각 `{colors.pleos-blue-3}` 흰 P, 목적지 원 테두리 `{colors.basic-900}` |

### 13-3. Signature Micro-Specs

```yaml
press-scale-feedback:
  description: "누르는 동안 버튼과 카드가 95%로 줄어든다. Pleos Connect Interaction 가이드."
  technique: "Modifier.graphicsLayer { scaleX = s; scaleY = s }, s = if (pressed) 0.95f else 1f, tween 100ms"
semantic-chip-triplet:
  description: "확인됨, 확인 필요, 미충족을 색 + 기호 + 단어 세 겹으로 표기"
  technique: "green-1/green-4, basic-100/basic-600, red-1/red-4. 기호는 ✓ ? ✕ 고정"
accent-only-on-selection:
  description: "파랑은 '선택됨'과 '추천'에만. 주 버튼은 회색 채움"
  technique: "selected border 3dp #2350FF; CTA bg #44464E"
pill-on-gray-segment:
  description: "회색 컨테이너 안에 흰 pill이 떠 있는 세그먼트"
  technique: "container basic-100 radius 20dp padding 6dp, indicator basic-00 radius 16dp + level-1 shadow"
route-strip-diagram:
  description: "차로 가는 구간과 걸어가는 구간을 선 종류로 구분하는 경로 도식"
  technique: "Canvas drawLine 6dp solid basic-600 / PathEffect.dashPathEffect(12dp,10dp) blue-3"
tight-display-tracking:
  description: "큰 제목 자간을 살짝 좁혀 단단하게"
  technique: "letterSpacing -0.01em on title-l, headline-s (pleos.ai letter-spacing:-.01em 30회)"
```

## 14. Content / Copy Voice
<!-- SOURCE: manual -->

- 짧은 평서문. "~합니다"보다 결론 먼저: "103동까지 계단 없는 연결이 확인되었습니다."
- 버튼은 동사로 끝낸다: "후보 보기", "이 계획을 내비에 적용".
- 확인되지 않은 것은 확인되지 않았다고 쓴다. 단정 금지.
- 120자 이내 (Design for Driving).

## 15. Drop-in CSS
<!-- SOURCE: auto+manual -->

Compose 대상이지만 웹 프로토타입용 토큰도 남긴다.

```css
:root {
  --font: "Asta Sans", "astaSans", sans-serif;
  --bg: #F7F8FA;            /* {colors.basic-50} */
  --surface: #FFFFFF;       /* {colors.basic-00} */
  --sunken: #EDEEF2;        /* {colors.basic-100} */
  --line: #E0E1E6;          /* {colors.basic-200} */
  --ink: #131417;           /* {colors.basic-900} */
  --ink-2: #44464E;         /* {colors.basic-600} */
  --ink-3: #686A72;         /* {colors.basic-500} */
  --muted: #A4A8AE;         /* {colors.basic-400} */
  --cta: #44464E;           /* {colors.basic-600} */
  --cta-pressed: #313236;   /* {colors.basic-700} */
  --accent: #2350FF;        /* {colors.pleos-blue-3} */
  --accent-tint: #CDDCFF;   /* {colors.pleos-blue-1} */
  --ok-bg: #BEFFE1;         /* {colors.pleos-green-1} */
  --ok-fg: #053C32;         /* {colors.pleos-green-4} */
  --no-bg: #FFD7DC;         /* {colors.pleos-red-1} */
  --no-fg: #5F0F14;         /* {colors.pleos-red-4} */
  --warn: #FA293C;          /* {colors.pleos-red-3} */
  --radius-card: 24px;
  --radius-control: 20px;
  --touch: 76px;
}
@media (prefers-color-scheme: dark) {
  :root {
    --bg: #131417;          /* {colors.basic-900} as dark basic_00 */
    --surface: #1D1E21;     /* {colors.basic-800} as dark basic_50 */
    --sunken: #313236;      /* {colors.basic-700} as dark basic_100 */
    --ink: #F7F8FA;         /* {colors.basic-50} as dark basic_800 */
    --accent: #5087FF;      /* {colors.pleos-blue-2} */
  }
}
.btn-filled { background: var(--cta); color: #FFFFFF; min-height: 88px; border-radius: var(--radius-control); }
.btn-filled:active { background: var(--cta-pressed); transform: scale(.95); }
.card { background: var(--surface); border-radius: var(--radius-card); padding: 32px; }
.card[aria-selected="true"] { outline: 3px solid var(--accent); }
```

## 16. Tailwind Config
<!-- SOURCE: auto -->

```js
colors: {
  pleos: { blue: { 1: "#CDDCFF", 2: "#5087FF", 3: "#2350FF", 4: "#0F235A" },
           green: { 1: "#BEFFE1", 2: "#69F5B9", 3: "#00D791", 4: "#053C32" },
           red: { 1: "#FFD7DC", 2: "#FF96A0", 3: "#FA293C", 4: "#5F0F14" } },
  basic: { 0: "#FFFFFF", 50: "#F7F8FA", 100: "#EDEEF2", 200: "#E0E1E6", 300: "#C7C9CE",
           400: "#A4A8AE", 500: "#686A72", 600: "#44464E", 700: "#313236", 800: "#1D1E21", 900: "#131417" }
}
```

## 17. Agent Prompt Guide
<!-- SOURCE: manual -->

- "Pleos Connect 차량 앱 화면. 바탕 #F7F8FA, 흰 카드 radius 24, Asta Sans. 주 버튼은 #44464E 채움 높이 88. 파랑 #2350FF는 선택 테두리에만."
- "상태 칩은 ✓ 확인됨(초록 틴트), ? 확인 필요(회색), ✕ 미충족(빨강 틴트). 색만으로 구분하지 말 것."
- "1200dp 이상이면 2단: 왼쪽 결정, 오른쪽 근거. 하단 행동 바 고정."
- "본문 최소 24dp, 결정용 텍스트 32dp, 터치 76dp, 간격 23dp. 애니메이션은 누름 95%만."

## 18. DO / DON'T
<!-- SOURCE: manual -->

### DO
- 바탕 `#F7F8FA`, 카드 `#FFFFFF`로 두 단계 면을 만든다.
- 주 버튼은 `#44464E`, 누르면 `#313236`.
- 선택된 카드에만 `#2350FF` 3dp 테두리.
- 상태는 색 + 기호 + 단어.
- 야간에는 basic 단계를 뒤집은 Dark 표를 쓴다.

### DON'T
- 주 버튼 배경을 `#2350FF` 파랑으로 두지 말 것. 대신 `#44464E` 사용.
- 흰 바탕 글자에 `#FA293C` 또는 `#00D791` 사용 금지. 대신 칩 글자 `#5F0F14`, `#053C32`.
- 본문 글자를 `#A4A8AE`로 두지 말 것 (2.4:1). 보조 글자는 `#686A72`.
- 카드에 1dp 회색 테두리를 기본으로 두지 말 것. 면 대비(`#F7F8FA` 위 `#FFFFFF`)로 구분.
- `fontSize` 24sp 미만 금지.
- 두 번째 서체(Roboto, Pretendard, Noto Sans 직접 지정) 금지. FontFamily.SansSerif = Asta Sans.
- 진입 애니메이션, 자동 스크롤, 깜빡임 금지.

### What This Site Doesn't Use (Negative-Space Identity)
- 그라디언트 면: 마케팅 히어로 영상 외에는 없다.
- 보라, 주황, 노랑 계열: 팔레트에 없다. 주의 상태도 빨강 틴트로 처리한다.
- 굵기 100, 200, 900: 시스템 폰트 매핑에 Light 아래 단계가 없다.
- 글래스모피즘 블러: pleos.ai 메뉴에만 있고 차량 화면에는 없다.
- 그림자 2단계 이상의 떠 있는 카드: 차량 앱은 평평하다.
- 장식용 일러스트, 이모지 아이콘.

## 19. Known Gaps & Assumptions
<!-- SOURCE: manual -->

- Label 위계 크기(24)는 가이드 이미지가 수집되지 않아 Android Design for Driving의 최소 본문 24dp로 가정했다.
- 그림자 level-1 수치는 가이드 Shadow 문서의 3단계 구조만 확인했고, 정확한 blur 값은 추정이다.
- 버튼 radius 20dp, 높이 88dp는 가이드 SVG 비율과 터치 76dp 기준에서 정한 값으로, 공식 수치가 아니다.
- alpha 계열(alpha_dark_50 등)의 정확한 투명도는 수집하지 못했다. basic_100으로 대체했다.
- pleos.ai 히어로는 영상이라 스크린샷 프레임마다 색 분포가 다르다. 브랜드 색 판정에는 CSS만 썼다.
