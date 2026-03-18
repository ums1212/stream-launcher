# StreamLauncher

**스트리머 팬을 위한 미니멀 Android 홈 런처**

스트리밍 콘텐츠(치지직/YouTube)와 앱 관리를 하나의 런처에서 — 4방향 스와이프 내비게이션, 커스텀 그리드, 드래그 앤 드롭 배치, 프리셋 마켓까지.

---

## 목차

- [스크린샷](#스크린샷)
- [주요 기능](#주요-기능)
- [아키텍처](#아키텍처)
- [모듈 구조](#모듈-구조)
- [기술 스택](#기술-스택)
- [개발 환경 설정](#개발-환경-설정)
- [테스트](#테스트)
- [Q&A — 채용 담당자를 위한 기술 FAQ](#qa--채용-담당자를-위한-기술-faq)

---

## 스크린샷

> 아래 각 섹션의 `[스크린샷 첨부]` 위치에 캡처 이미지를 추가하세요.

---

## 주요 기능

### 1. 4방향 스와이프 내비게이션

`VerticalPager(3페이지)` + `HorizontalPager(3페이지)`를 중첩해 상하좌우 4방향으로 화면을 전환합니다.
각 방향은 독립된 콘텐츠 영역을 가지며, 페이지 전환 시 Alpha 애니메이션이 적용됩니다.

| 방향 | 화면 |
|------|------|
| 중앙 (홈) | 2×2 다이나믹 그리드 |
| 위 | 시스템 알림 바 / 런처 설정 바 |
| 아래 | 앱 서랍 (초성 검색) |
| 왼쪽 | 스트리밍 피드 (치지직/YouTube) |
| 오른쪽 | Android 위젯 배치 공간 |

<!-- 스크린샷 필요: 홈 → 왼쪽/아래쪽 스와이프 전환 모습을 나란히 보여주는 GIF 또는 3장 연속 이미지.
     4방향 내비게이션 구조가 한눈에 들어오는 그림이 가장 효과적입니다. -->
[스크린샷 첨부]

---

### 2. 다이나믹 그리드 (홈 화면)

2×2 그리드에서 셀을 탭하면 해당 영역이 확장되고 나머지는 축소됩니다.
확장된 영역 안에 배치된 앱 아이콘이 리스트 형태로 노출되며, 탭으로 앱을 실행합니다.

<!-- 스크린샷 필요: (좌) 셀 미선택 기본 2×2 그리드 / (우) 특정 셀 확장 후 앱 아이콘이 보이는 상태.
     셀 확장 전후를 나란히 비교할 수 있으면 좋습니다. -->
[스크린샷 첨부]

---

### 3. 드래그 앤 드롭 앱 배치

앱 서랍에서 앱 아이콘을 길게 누른 채 홈 그리드 셀로 드래그해 원하는 위치에 고정할 수 있습니다.

- 드래그 시작: `detectDragGesturesAfterLongPress` 제스처 감지
- 홈 자동 스크롤: 앱 서랍 → 홈으로 페이지 자동 전환
- 셀 하이라이트: 3단계 네온 글로우 + 자력 햅틱 피드백
- 드롭 타이밍 보정: 셀 bounds 늦은 등록 문제를 재시도 루프로 해결

<!-- 스크린샷 필요: 앱 아이콘을 드래그 중인 상태 (드래그 오버레이와 글로우 하이라이트가 보이는 순간).
     동적 효과라 GIF 캡처가 가장 효과적입니다. -->
[스크린샷 첨부]

---

### 4. 앱 서랍 & 한글 초성 검색

설치된 앱 전체를 가나다순으로 표시하며, 한글 초성(예: `ㄴㅂ` → `네이버`)으로 빠르게 검색합니다.

- `PackageManager` API (Android 11+ `queryIntentActivities` 분기 처리)
- 검색 디바운스: `Flow.debounce(100ms)` + `StateFlow`
- 화면 진입 시 키보드 자동 포커스 (`FocusRequester`)
- 페이지 이탈 시 키보드 자동 숨김

<!-- 스크린샷 필요: (좌) 앱 서랍 전체 목록 화면 / (우) 초성 입력('ㄴㅂ' 등) 후 필터링된 결과 화면.
     검색 기능의 실용성을 강조할 수 있는 화면입니다. -->
[스크린샷 첨부]

---

### 5. 스트리밍 피드 (치지직 / YouTube)

설정한 채널의 실시간 방송 여부, 최신 영상, RSS 공지사항을 한 화면에서 확인합니다.

- **라이브 상태**: 치지직 API → 네온 Breathing 애니메이션으로 방송 중 표시
- **채널 프로필 카드**: YouTube API → 원형 아바타 + 구독자 수 (만/천 단위 포맷)
- **피드 목록**: 치지직 공지 + YouTube 영상 + RSS 항목 통합 (`FeedItem` sealed interface)
- **캐시 전략**: DataStore 7일 TTL, stale-while-revalidate 패턴
- **60초 쿨다운**: `loadJob` 중복 방지 + 수동 새로고침 제어

<!-- 스크린샷 필요: (좌) 방송 중 상태 카드 (네온 글로우) / (우) 채널 프로필 카드 + 피드 목록 스크롤 화면.
     시각적으로 독특한 네온 효과가 잘 드러나는 캡처를 권장합니다. -->
[스크린샷 첨부]

---

### 6. 프리셋 마켓

다른 사용자가 공유한 런처 설정(프리셋)을 Firebase 기반 마켓에서 다운로드하거나 직접 업로드할 수 있습니다.

- **인증**: Google Sign-In (`Credentials API`)
- **목록/검색**: Paging 3 (`PagingSource` × 2) → 무한 스크롤
- **업로드**: `UploadToMarketDialog` + Firebase Storage/Firestore
- **광고**: AdMob 배너 (`AdmobBanner` Composable)
- **네비게이션**: Type-safe `@Serializable` route (`MarketHome` / `MarketDetail` / `MarketSearch`)

<!-- 스크린샷 필요: 프리셋 마켓 홈 화면 (카드 목록 + AdMob 배너) 및 프리셋 상세 화면.
     Firebase 연동 기능이므로 실제 데이터가 채워진 상태의 화면이 설득력 있습니다. -->
[스크린샷 첨부]

---

### 7. 런처 설정

| 탭 | 내용 |
|----|------|
| 색상 | 3열 컬러 프리셋 그리드 → 테마 Accent 색상 즉시 반영 |
| 이미지 | 2×2 셀별 배경 이미지 (`PickVisualMedia`) |
| 피드 | 치지직 핸들, YouTube 채널 ID, RSS URL, 피드 배경 이미지 |
| 앱 서랍 | 앱 서랍 관련 옵션 |
| 프리셋 | 프리셋 마켓 진입 / 내 프리셋 공유 |

설정은 `DataStore` + `kotlinx-serialization`으로 영속화되며, 런처 재시작 없이 즉시 적용됩니다.

<!-- 스크린샷 필요: 설정 메인 화면 및 색상/피드 탭 화면 최소 2장.
     컬러 프리셋 선택 전후 테마 변화가 보이면 더욱 좋습니다. -->
[스크린샷 첨부]

---

## 아키텍처

**Clean Architecture + MVI (Model-View-Intent)**

```
Composable Screen
     │  UI Event (Intent)
     ▼
  ViewModel  ◄──────────────── SideEffect (one-shot)
     │  State (StateFlow)
     ▼
  UseCase(s)
     │  depends on (interface)
     ▼
Repository Interface  (core:domain)
     ▲  implements
     │
RepositoryImpl (core:data)
     │  accesses
     ▼
Data Sources (PackageManager / DataStore / Retrofit / Firebase)
```

### MVI Contract 패턴

각 화면은 `*Contract.kt` 한 파일에 세 가지 타입을 정의합니다.

```kotlin
// 예시: HomeContract.kt
data class HomeState(...) : UiState
sealed interface HomeIntent : UiIntent {
    data class Search(val query: String) : HomeIntent
    data object ResetHome : HomeIntent
    // ...
}
sealed interface HomeSideEffect : UiSideEffect {
    data class LaunchApp(val packageName: String) : HomeSideEffect
}
```

ViewModel은 `BaseViewModel<S, I, E>`를 상속하며 `@HiltViewModel`로 주입됩니다.

---

## 모듈 구조

```
:app
├── :core:domain     순수 JVM — 모델, Repository 인터페이스, UseCase
├── :core:data       Repository 구현, Room, DataStore, Firebase, Hilt DI
├── :core:ui         공유 Composable, 테마 (StreamLauncherTheme), DragDropState
├── :core:network    Retrofit, OkHttp, RSS XmlConverter, Chzzk/YouTube 서비스
├── :feature:launcher          홈 그리드, 4방향 내비게이션, 피드 화면
├── :feature:apps-drawer       앱 서랍, AppIcon, 초성 검색
├── :feature:widget            Android 위젯 호스팅
├── :feature:settings          설정 화면, 컬러/이미지/피드 설정
└── :feature:preset-market     마켓 홈/상세/검색, Google 로그인, AdMob
```

의존 방향은 단방향입니다: `feature → core:ui/domain`, `core:data → core:domain/network`.
feature 모듈 간 직접 의존은 금지합니다.

---

## 기술 스택

| 분류 | 라이브러리 |
|------|-----------|
| 언어 | Kotlin 2.0.21 |
| UI | Jetpack Compose, Material 3 |
| DI | Hilt 2.51.1 |
| 비동기 | Kotlin Coroutines 1.8.1, Flow |
| 네트워크 | Retrofit 2.11.0, OkHttp 4.12.0 |
| XML 파싱 | xmlutil 0.90.3 (RSS) |
| 직렬화 | kotlinx-serialization-json 1.7.3 |
| 영속화 | DataStore 1.1.4 |
| 이미지 로딩 | Coil 2.6.0 |
| 페이징 | Paging 3.3.6 |
| 백엔드 | Firebase Firestore, Storage, Auth (BOM 34.x) |
| 광고 | Google AdMob |
| 인증 | Google Credentials API |
| 내비게이션 | Navigation Compose 2.8.9 (Type-safe routes) |
| 테스트 | JUnit 4, MockK 1.13.13, Turbine 1.2.0, kotlinx-coroutines-test |
| 빌드 | AGP 8.12.3, Gradle Version Catalog |

---

## 개발 환경 설정

### 요구사항

- Android Studio Hedgehog 이상
- JDK 17
- Android SDK API 28+

### 빌드

```bash
# Windows (bash 환경)
JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew assembleDebug
```

> **주의**: Android Studio 번들 JBR의 `jvm.cfg`가 깨진 경우가 있어, JDK 17을 명시적으로 지정합니다.

### local.properties 설정

```properties
# YouTube Data API v3 키 (피드 기능용)
YOUTUBE_API_KEY=your_api_key_here
```

### Firebase 설정

`app/google-services.json` 파일을 Firebase 콘솔에서 다운로드해 배치합니다.

---

## 테스트

```bash
JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew test
```

- **프레임워크**: JUnit 4 + MockK + Turbine + kotlinx-coroutines-test
- **코루틴**: `StandardTestDispatcher` + `Dispatchers.setMain/resetMain`
- **Flow 검증**: Turbine `flow.test { awaitItem() }`
- **테스트 네이밍**: 한국어 backtick 함수명 (예: `` `초기 상태 - isLoading false` ``)
- **커버리지**: UseCase 단위 테스트, MVI State Reducer 검증, SideEffect 흐름 검증

주요 테스트 파일:

| 파일 | 테스트 수 |
|------|---------|
| `HomeViewModelTest` | 25+ |
| `SettingsViewModelTest` | 13 |
| `ChosungMatcherTest` | 11 |
| `AppRepositoryImplTest` | 7 |
| `ColorPresetsTest` | 7 |
| `FeedViewModelTest` | 5+ |

---

## Q&A — 채용 담당자를 위한 기술 FAQ

### Q1. 왜 MVI 패턴을 선택했나요? MVVM과 어떤 차이가 있나요?

**A.** 런처 앱은 홈 화면, 앱 서랍, 피드, 설정이 동시에 상태를 공유하는 복잡한 UI를 가집니다. MVVM의 양방향 데이터 바인딩은 상태 변화의 출처를 추적하기 어렵게 만들 수 있습니다. MVI는 **단방향 데이터 흐름** (`Intent → State → UI`)을 강제하기 때문에:

- **상태 변화의 출처가 항상 명확**합니다 (`HomeIntent`의 어떤 케이스가 실행됐는지 로그 한 줄로 추적 가능).
- **State는 불변 data class**이므로 `copy()`로 변경 이력을 명시적으로 관리합니다.
- `SideEffect`로 일회성 이벤트(앱 실행, 토스트 등)를 State와 분리해 **재구성(recomposition) 시 중복 실행**을 방지합니다.

---

### Q2. 멀티모듈 구조를 선택한 이유는 무엇인가요?

**A.** 세 가지 실용적인 이유가 있습니다.

1. **점진적 빌드**: Gradle 증분 빌드와 병렬 컴파일 덕분에 feature 하나를 수정해도 전체 recompile이 발생하지 않습니다.
2. **의존성 경계 강제**: `core:domain`은 순수 JVM 모듈로 Android 의존성이 전혀 없어, Domain 로직이 프레임워크에 오염되는 것을 컴파일 수준에서 방지합니다.
3. **팀 확장 대비**: feature 모듈 간 직접 의존을 금지해, 향후 팀원이 늘어도 각자 담당 모듈을 독립적으로 작업할 수 있습니다.

---

### Q3. 한글 초성 검색은 어떻게 구현했나요?

**A.** `ChosungMatcher` 유틸 클래스(`core:domain/util`)를 직접 구현했습니다.

- 유니코드 한글 자모 분리 공식(`(char - 0xAC00) / 28 / 21`)으로 초성을 추출합니다.
- 검색어가 자음(초성)으로만 이루어진 경우, 앱 이름의 각 글자에서 초성을 추출해 순서대로 비교합니다.
- 자음·모음 혼합 검색어는 일반 `contains()` 검색으로 자동 폴백합니다.
- 검색 결과는 `Flow.debounce(100ms)`로 디바운스해 타이핑 중 과도한 필터링을 방지합니다.

---

### Q4. 드래그 앤 드롭을 Compose에서 구현할 때 어떤 어려움이 있었나요?

**A.** 크게 세 가지 문제를 해결했습니다.

1. **좌표계 불일치**: `VerticalPager` 안의 `HorizontalPager` 안에 셀이 중첩되어 있어, 각 Composable의 `offset()`이 서로 다른 좌표계를 기준으로 했습니다. `positionInRoot()`로 모든 좌표를 루트 기준으로 통일해 해결했습니다.
2. **셀 bounds 등록 타이밍**: 앱 서랍 → 홈으로 페이지가 전환되는 중에 드롭하면 셀 bounds가 아직 등록되지 않아 드롭이 취소됐습니다. 홈 전환 완료 후 짧은 재시도 루프로 bounds 등록을 기다리도록 보완했습니다.
3. **확장 셀 변경 시 드래그 중단**: 드래그 중 다른 셀 위로 이동하면 확장 셀이 바뀌고, 기존 셀의 Composable이 dispose되면서 드래그 제스처가 끊겼습니다. `DragDropState`를 `core:ui`의 `CompositionLocal`로 분리해 Composable 수명주기에 독립적으로 드래그 상태를 유지했습니다.

---

### Q5. Firebase 없이도 앱이 동작하나요? 환경 분리는 어떻게 하셨나요?

**A.** Firebase 기능(프리셋 마켓, 공지사항)은 `feature:preset-market`과 `core:data`의 `FirebaseModule`에 격리되어 있습니다. `google-services.json`이 없으면 해당 기능만 비활성화되고, 홈/피드/설정 등 나머지 기능은 독립적으로 동작합니다. YouTube API 키는 `local.properties`에서 `BuildConfig`로 주입해 소스 코드에 하드코딩되지 않도록 처리했습니다.

---

### Q6. 테스트 전략은 어떻게 세웠나요? UI 테스트는 없나요?

**A.** 현재는 **Domain·Presentation 레이어의 단위 테스트**를 우선했습니다.

- `UseCase`는 순수 Kotlin이므로 Robolectric 없이 JVM에서 바로 실행합니다.
- `ViewModel`은 Turbine으로 `StateFlow`·`SharedFlow` 방출 순서를 검증합니다.
- MockK의 `relaxed = true` 패턴으로 nullable 파라미터를 가진 UseCase mock 시 컴파일 오류를 회피합니다.

Compose UI 테스트는 다음 단계로 주요 인터랙션(그리드 확장, 드래그 드롭)에 대해 추가할 예정입니다.

---

### Q7. 메모리 누수는 어떻게 탐지하고 해결했나요?

**A.** LeakCanary를 통해 `static AndroidComposeView.composeViews → 파괴된 MainActivity` 누수를 탐지했습니다. Compose 내부 static 리스트를 앱 코드에서 직접 제어할 수 없어, **`configChanges` 선언으로 Activity 재생성 자체를 억제**하는 방향으로 해결했습니다. `LocalConfiguration.current`를 이미 사용 중이라 Activity 재생성 없이도 가로/세로 레이아웃이 정상 전환됩니다. 추가로 `AppWidgetHost`의 context를 `applicationContext`로 교체해 별도 누수 경로도 차단했습니다.

---

### Q8. Type-safe Navigation으로 전환한 이유는 무엇인가요?

**A.** 기존 문자열 기반 route(`"settings/detail/{menu}"`)는 오타가 런타임에야 발견되고, 파라미터 추출 시 `Uri.decode` 수동 파싱이 필요했습니다. Navigation Compose 2.8+의 `@Serializable` route 방식으로 전환하면서:

- 잘못된 route 객체 전달이 **컴파일 타임에 차단**됩니다.
- `backStackEntry.toRoute<T>()`로 파라미터를 타입 안전하게 추출합니다.
- `sealed interface LauncherRoute`로 route 그룹을 묶어 콜백 시그니처를 `(Any)` 대신 `(LauncherRoute)`로 좁혔습니다.

---

### Q9. 이 프로젝트에서 가장 기술적으로 도전적이었던 부분은 무엇인가요?

**A.** **중첩 Pager 안에서의 드래그 앤 드롭 구현**이 가장 복잡했습니다. `VerticalPager` × `HorizontalPager` 중첩 구조에서 제스처 이벤트 소유권 충돌, 좌표계 불일치, Composable 수명주기에 따른 상태 소멸, 페이지 전환 타이밍 문제가 동시에 얽혀 있었습니다. 각 문제를 독립적으로 분리해 `positionInRoot()` 통일 → `CompositionLocal` 상태 분리 → bounds 재시도 루프 순서로 해결했습니다. 이 과정에서 Compose의 레이아웃 좌표 시스템과 제스처 이벤트 전파 메커니즘을 깊이 이해하게 됐습니다.

---

### Q10. 향후 개발 계획이 있나요?

**A.** 다음 기능들을 검토하고 있습니다.

- [ ] Compose UI Test — 그리드 확장, 드래그 앤 드롭 E2E 시나리오
- [ ] Room DB 도입 — 앱 사용 빈도 기반 자동 배치 추천
- [ ] Wear OS 연동 — 스마트워치에서 방송 알림 수신
- [ ] 공지사항 푸시 알림 — Firebase Cloud Messaging 연동

---

## 라이선스

```
Copyright 2025 comon

Licensed under the Apache License, Version 2.0
```
