# StreamLauncher 개발 로그

## [2026-02-20] Step 9 — 동적 2x2 그리드 애니메이션 구현

### 목표
중앙 홈 페이지의 `Text("Home")` 플레이스홀더를 실제 2x2 동적 그리드로 교체한다. 셀 클릭 시 해당 셀이 확장(0.8f)되고 나머지가 축소(0.2f)되는 `animateFloatAsState` 기반 애니메이션을 구현한다. ViewModel/State 레이어(`expandedCell`, `ClickGrid`, `toggleCell`)는 Step 4에서 완성된 상태이므로 **Compose UI 작업만** 필요하다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `feature/launcher/build.gradle.kts` | `implementation(libs.androidx.compose.foundation)` 추가 (`LazyColumn` 사용) |
| 2 | `feature/launcher/.../launcher/ui/HomeScreen.kt` | **신규** — 2x2 그리드 + 애니메이션 구현 |
| 3 | `app/.../MainActivity.kt` | `val uiState by viewModel.uiState.collectAsStateWithLifecycle()` 바인딩, `Box(Text("Home"))` → `HomeScreen(state=uiState, onIntent=viewModel::handleIntent)` 교체, 미사용 import (`Box`, `Text`, `Alignment`, `Modifier`, `MaterialTheme`, `fillMaxSize`) 제거 |

#### HomeScreen.kt 구조

```
HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit, modifier: Modifier)
└─ Column(fillMaxSize, padding=8.dp)
   ├─ Row(weight=topRowWeight, fillMaxWidth, paddingBottom=4.dp)
   │   ├─ GridCellContent(TOP_LEFT,  weight=leftColWeight)
   │   └─ GridCellContent(TOP_RIGHT, weight=rightColWeight)
   └─ Row(weight=bottomRowWeight, fillMaxWidth, paddingTop=4.dp)
       ├─ GridCellContent(BOTTOM_LEFT,  weight=leftColWeight)
       └─ GridCellContent(BOTTOM_RIGHT, weight=rightColWeight)
```

#### 애니메이션 로직

| 변수 | 미확장 | 해당 축 확장 | 반대 축 확장 |
|------|--------|-------------|-------------|
| `topRowWeight` | 0.5f | 0.8f (TOP_*) | 0.2f (BOTTOM_*) |
| `bottomRowWeight` | 0.5f | 0.2f | 0.8f |
| `leftColWeight` | 0.5f | 0.8f (*_LEFT) | 0.2f (*_RIGHT) |
| `rightColWeight` | 0.5f | 0.2f | 0.8f |

- `animateFloatAsState` × 2개 (`topRowWeight`, `leftColWeight`), 나머지 2개는 `1f - animated`로 파생
- `tween(400ms, FastOutSlowInEasing)`

#### GridCellContent (private RowScope 확장)

- `Surface(onClick, RoundedCornerShape(12.dp), surfaceVariant)`
- 확장 시: `LazyColumn` 앱 목록 + `alpha = ((weight - 0.6f) / 0.2f).coerceIn(0f, 1f)` 로 페이드인
- 축소 시: 셀 이름(`cell.name`) 중앙 정렬

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL (162 tasks, 42s)
./gradlew test           →  BUILD SUCCESSFUL (245 tasks, 22s, failures=0)
기존 테스트 전원 통과 (회귀 없음)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `animateFloatAsState` 2개 + 파생값 2개 | 4개 독립 애니메이션 대신 Row·Col 각 축당 1개만 사용해 weights가 항상 정확히 1.0f로 합산됨. 불일치 레이아웃 오류 원천 차단 |
| `tween(400ms, FastOutSlowInEasing)` | 400ms는 확장 전환에 충분히 느껴지지 않으면서도 응답성 손실 없는 경계값. FastOutSlowIn은 머테리얼 권장 강조 이징 |
| `contentAlpha = ((weight - 0.6f) / 0.2f).coerceIn(0f, 1f)` | 확장 애니메이션 후반부(weight 0.6→0.8 구간)에서만 앱 목록을 페이드인. 셀이 충분히 커지기 전에 텍스트가 노출되는 UX 문제 방지 |
| `RowScope` 확장 함수로 `GridCellContent` 정의 | `Modifier.weight()` 를 컴포저블 내부에서 직접 사용 가능 → 호출부가 weight 계산에 관여하지 않아 관심사 분리 |
| `isExpanded` 즉시 전환 + weight 지연 전환 병행 | Boolean 전환(즉시)으로 표시 콘텐츠를 결정하고, float 전환(tween)으로 크기를 결정. 앱 목록은 alpha로 소프트하게 등장하므로 두 전환 타이밍 차이가 사용자에게 자연스럽게 보임 |
| `bottomRowWeight = 1f - topRowWeight` 파생 | 단순 산술. 두 weight가 항상 합산 1.0f → Compose Column이 의도한 비율로 정확히 분배 |

---

## [2026-02-20] Step 8 — 시스템 홈 버튼 복귀 로직 구현

### 목표
다른 앱 사용 중 홈 버튼을 누르면 런처로 복귀하고, 이미 런처가 표시된 상태에서 홈 버튼을 누르면 `CrossPagerNavigation`이 중앙(1,1)으로 애니메이션 복귀하도록 구현한다. 아울러 `HomeState.expandedCell`도 `null`로 초기화한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `app/src/main/AndroidManifest.xml` | `<activity>` 에 `android:launchMode="singleTask"` 추가, HOME/DEFAULT 카테고리 인텐트 필터 추가 |
| 2 | `feature/launcher/.../HomeContract.kt` | `HomeIntent.ResetHome` sealed object 추가 |
| 3 | `feature/launcher/.../HomeViewModel.kt` | `handleIntent`에 `ResetHome → copy(expandedCell = null)` 분기 추가 |
| 4 | `app/.../navigation/CrossPagerNavigation.kt` | `resetTrigger: Int = 0` 파라미터 추가, `LaunchedEffect(resetTrigger)` 로 양쪽 pager를 `animateScrollToPage(1, tween(300))` 호출 |
| 5 | `app/.../MainActivity.kt` | `private val viewModel by viewModels<HomeViewModel>()`, `private var resetTrigger by mutableIntStateOf(0)`, `onNewIntent` 오버라이드 — `setIntent(intent)` 호출, HOME 카테고리 + `isAtLeast(STARTED)` 조건 시 `resetTrigger++` + `viewModel.handleIntent(ResetHome)` |

### 검증 결과
```
./gradlew assembleDebug  →  BUILD SUCCESSFUL (162 tasks, 22s)
./gradlew test           →  BUILD SUCCESSFUL (245 tasks, 24s, failures=0)
기존 테스트 전원 통과 (회귀 없음)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `launchMode="singleTask"` | HOME 인텐트는 항상 기존 Activity 인스턴스를 재사용해야 함. `standard`면 새 인스턴스가 생성되어 `onNewIntent`가 호출되지 않음 |
| `resetTrigger: Int` counter 방식 | Boolean flag는 `false→true→false`로 두 번 연속 눌렀을 때 두 번째 변경을 Compose가 감지 못할 수 있음. Int 증가 방식은 매번 새 값이므로 `LaunchedEffect` key가 항상 변경됨 |
| Pager 리셋을 ViewModel 거치지 않고 직접 제어 | Pager 위치는 UI 내비게이션 상태이지 도메인 상태가 아님. `HomeViewModel`의 책임은 앱 목록 관리이며, `MVI SideEffect` 라운드트립은 불필요하게 복잡 |
| `by viewModels()` 로 Activity 수준에서 ViewModel 획득 | `hiltViewModel()`은 Compose 컴포지션 내부에서만 참조 가능. `onNewIntent`는 Activity 생명주기 콜백이므로 `viewModels()` 로 같은 인스턴스를 Activity 수준에서 직접 접근 |
| `animateScrollToPage(1, tween(300))` | 홈 버튼은 즉각 반응이 중요하므로 기본 spring 애니메이션보다 짧고 예측 가능한 300ms tween 선택 |
| `setIntent(intent)` 명시 호출 | `onNewIntent` 호출 시 `getIntent()`는 자동 갱신되지 않음. 설정 변경(화면 회전 등) 후 재생성 시 최신 인텐트 유지 필요 |
| `isAtLeast(STARTED)` (RESUMED 아님) | 홈 버튼 압력 시 Android는 `onPause()` 후 `onNewIntent()`를 호출하므로, 이 시점 lifecycle은 이미 STARTED. `isAtLeast(RESUMED)` 조건은 포그라운드 케이스를 차단함. STARTED 체크로 포그라운드(STARTED/RESUMED)와 완전 백그라운드(CREATED, Activity가 STOPPED됐던 경우)를 정확히 구분 |

---

## [2026-02-20] Step 7 — 4방향 스와이프 내비게이션 프레임워크 구현

### 목표
런처의 핵심 UX인 십자형(상/하/좌/우) 스와이프 내비게이션을 구현한다. `VerticalPager(3페이지)` 안에 `HorizontalPager(3페이지)`를 중첩하여 홈을 중심으로 4방향 이동이 가능한 구조를 만들고, 뒤로가기·Alpha 전환·대각선 제스처 차단을 함께 적용한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `gradle/libs.versions.toml` | `androidx-compose-foundation` 라이브러리 alias 추가 |
| 2 | `app/build.gradle.kts` | `implementation(libs.androidx.compose.foundation)` 의존성 추가 |
| 3 | `app/.../navigation/CrossPagerNavigation.kt` | **신규** — 십자형 Pager 컴포넌트 |
| 4 | `app/.../MainActivity.kt` | `Scaffold { TempAppList }` → `CrossPagerNavigation { TempAppList }` 교체, 불필요 import 제거 |

#### CrossPagerNavigation 내부 구조
```
CrossPagerNavigation(homeContent)
├─ VerticalPager (3페이지, initialPage=1, beyondViewportPageCount=1)
│   ├─ [0] UpPage     — "Notifications & Settings" (Surface + statusBarsPadding)
│   ├─ [1] CenterRow
│   │   └─ HorizontalPager (3페이지, initialPage=1, beyondViewportPageCount=1)
│   │       ├─ [0] LeftPage  — "Feed & Announcements" (Surface + safeDrawingPadding)
│   │       ├─ [1] homeContent() (Box + safeDrawingPadding)
│   │       └─ [2] RightPage — "Widget Area" (Surface + safeDrawingPadding)
│   └─ [2] DownPage   — "App Drawer" (Surface + navigationBarsPadding)
└─ BackHandler (중앙 아닐 때 활성화 → animateScrollToPage(1) 복귀)
```

#### 주요 구현 포인트
- **Alpha 효과**: `graphicsLayer { alpha = pageAlpha(pagerState, page) }` — `lerp(0.5f, 1f, 1f - offset)`으로 중앙 1.0f, 인접 0.5f
- **대각선 차단**: HorizontalPager에 `userScrollEnabled = !verticalPagerState.isScrollInProgress`, VerticalPager에 역방향 동일 적용
- **BackHandler**: `verticalPage != 1 || horizontalPage != 1`일 때 활성화, 수직 먼저 복귀 후 수평 복귀
- **Edge-to-Edge**: 페이지 배경은 전체화면, 콘텐츠는 각 페이지별 safe area padding으로 시스템 바 침범 방지

### 검증 결과
```
./gradlew assembleDebug  →  BUILD SUCCESSFUL (162 tasks, 1m 23s)
./gradlew test           →  BUILD SUCCESSFUL (245 tasks, failures=0)
총 32개 테스트 통과 (기존 27개 + placeholder 5개 포함, 신규 실패 0건)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `horizontalPagerState`를 `CrossPagerNavigation` 수준으로 호이스팅 | `BackHandler`가 수직·수평 양쪽 state 모두 참조해야 하므로 공통 스코프에서 선언 |
| `beyondViewportPageCount = 1` | 인접 페이지 프리로드로 스와이프 반응성 향상, 메모리 영향 최소 (±1 페이지만) |
| `userScrollEnabled` 교차 잠금 | Compose Foundation은 직교 축 충돌을 기본 처리하지만, 빠른 대각선 플릭 시 양쪽 pager가 동시에 반응할 수 있어 추가 안전장치로 적용 |
| Alpha `lerp(0.5f, 1f, ...)` | 완전 투명(0f)은 페이지 존재감 소실, 0.5f 하한선으로 인접 페이지가 살짝 보이는 고급 전환 효과 연출 |
| `homeContent` 람다 주입 | CrossPagerNavigation이 홈 콘텐츠 구현에 무관하게 재사용 가능하도록 분리 |
| 각 페이지 `Surface` + 내부 Box에 padding | `graphicsLayer` alpha가 Surface 배경 전체에 적용되고, 콘텐츠만 safe area 안에 위치하여 배경이 시스템 바 뒤까지 자연스럽게 채워짐 |

---

## [2026-02-20] Step 6 — Hilt 최종 연결 및 실데이터 확인

### 목표
Step 1~5에서 구현된 멀티모듈 DI 그래프를 `:app` 진입점에 연결하고, 임시 UI로 설치 앱 목록이 실제로 표시되는지 E2E 흐름을 검증한다.

### 변경 사항

| 파일 | 변경 내용 |
|------|----------|
| `gradle/libs.versions.toml` | `lifecycle-runtime-compose`, `lifecycle-viewmodel-ktx` 라이브러리 항목 추가 (`version.ref = lifecycleRuntimeKtx`) |
| `app/build.gradle.kts` | `lifecycle-runtime-compose` 의존성 추가 (`collectAsStateWithLifecycle` 사용) |
| `core/ui/build.gradle.kts` | `lifecycle-viewmodel-ktx` 추가 (BaseViewModel의 `ViewModel`/`viewModelScope` 명시적 선언) |
| `app/.../StreamLauncherApplication.kt` | **신규** — `@HiltAndroidApp class StreamLauncherApplication : Application()` |
| `app/src/main/AndroidManifest.xml` | `<application android:name=".StreamLauncherApplication">` 추가 |
| `app/.../MainActivity.kt` | `@AndroidEntryPoint`, `hiltViewModel<HomeViewModel>()`, `collectAsStateWithLifecycle()`, `LaunchedEffect`로 SideEffect 로그, `TempAppList` Composable 추가, 기존 `Greeting`/Preview 제거 |

#### TempAppList 임시 UI 구조
```
LazyColumn
├── [isLoading] → Text("Loading...")
└── forEach GridCell (TOP_LEFT / TOP_RIGHT / BOTTOM_LEFT / BOTTOM_RIGHT)
    ├── Section header: "── {cell.name} ({size}개) ──"
    └── items: "{app.label} ({app.packageName})"
```

### 검증 결과
```
./gradlew :app:assembleDebug  →  BUILD SUCCESSFUL (122 tasks)
./gradlew test                →  BUILD SUCCESSFUL (245 tasks, failures=0)
```
- `core:domain`: AppEntityTest(5) + AppRepositoryTest(3) + UseCaseTest(2) = 10개
- `core:data`: AppRepositoryImplTest(7) = 7개
- `feature:launcher`: HomeViewModelTest(12) = 12개
- 기타 placeholder: app(1) + core:ui(1) + feature:apps-drawer(1) = 3개

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `TempAppList`를 별도 Composable로 분리 | 추후 실제 Launcher UI로 교체할 때 MainActivity 변경 최소화 |
| `collectAsStateWithLifecycle()` 사용 | `collectAsState()` 대비 Lifecycle STARTED 이하에서 Flow 수집 중단 → 배터리/자원 절약 |
| SideEffect를 `LaunchedEffect(Unit)` 단일 블록으로 수집 | `effect`는 Channel 기반이므로 한 곳에서 collect하면 중복 소비 없음 |
| `core:ui`에 `lifecycle-viewmodel-ktx` 직접 선언 | Release variant 컴파일 시 transitive dep 미포함 버그 방지 (`compileReleaseKotlin FAILED` 재현 방지) |

---

## [2026-02-20] Step 5 — core:data AppRepositoryImpl + Hilt DI (TDD)

### 목표
`core:data` 레이어에서 PackageManager로 설치된 앱 목록을 가져오는 `AppRepositoryImpl`을 TDD로 구현하고, Hilt DI 모듈로 바인딩한다.

### 변경 사항

#### build.gradle.kts
- `core/data`: 테스트 의존성 추가 (`coroutines-test`, `turbine`, `mockk`), `testOptions.unitTests.isReturnDefaultValues = true`
- `core/domain`: `javax.inject:javax.inject:1` 추가

#### core:data 신규 파일
- `AppRepositoryImpl.kt`: PackageManager 주입, flow { emit } + flowOn(IO), API 33+ 분기, activityInfo null 필터
- `RepositoryModule.kt`: `@Binds AppRepositoryImpl → AppRepository`, `@Provides PackageManager`
- `AndroidManifest.xml`: `<queries>` 블록 추가 (Android 11+ 패키지 가시성)

#### core:domain 수정
- `GetInstalledAppsUseCase`: `@Inject constructor` 추가

#### 테스트 7개 (AppRepositoryImplTest)
- 빈 목록 emit, ResolveInfo→AppEntity 매핑, 여러 앱 매핑, 단일 emission 완료
- Intent 전달 검증 (`mockkConstructor`), loadLabel 호출 확인, null activityInfo 필터링

#### 기술적 해결
- `ResolveInfo.activityInfo`는 Java public 필드 → `spyk(ResolveInfo())`로 spy 후 직접 할당
- `Intent` 생성자 stub: `isReturnDefaultValues = true` + `mockkConstructor(Intent::class)`
- MockK matcher import: `import io.mockk.*` (와일드카드로 extension function 해결)

### 검증 결과
- `core:data` 신규 7개 테스트 통과 (failures=0, skipped=0)
- `core:domain` 기존 10개 테스트 통과 (UP-TO-DATE)
- `feature:launcher` 기존 10개 테스트 통과 (UP-TO-DATE)

### 설계 결정
- `PackageManager`를 Context가 아닌 직접 주입: DI 그래프를 단순하게 유지하고 테스트 용이성 확보
- `flow { emit }` 단일 emission: PM은 동기 API이므로 복잡한 스트림 불필요
- `flowOn(Dispatchers.IO)`: 메인 스레드 블로킹 방지 (PM 쿼리는 I/O 작업)

---

## [2026-02-20] Step 4 — HomeViewModel MVI 구현 (TDD)

### 목표
`AppInfo` → `AppEntity` 리네이밍 후 `core:domain` UseCase 추가, `feature:launcher`에 MVI 기반 `HomeViewModel`을 TDD로 구현한다.

### 변경 사항

#### Phase A: core:domain 리팩터링
- `AppInfo` → `AppEntity` 리네이밍 (`appName` → `label`)
- `AppRepository` 인터페이스: `Flow<List<AppInfo>>` → `Flow<List<AppEntity>>`
- `AppInfoTest` → `AppEntityTest` (필드명 반영)
- `AppRepositoryTest` 업데이트 (AppEntity + label)
- `GetInstalledAppsUseCase` 신규 추가 (repository 위임 패턴)
- `GetInstalledAppsUseCaseTest` 2개 테스트

#### Phase B: feature:launcher HomeViewModel
- `GridCell` enum: TOP_LEFT / TOP_RIGHT / BOTTOM_LEFT / BOTTOM_RIGHT
- `HomeContract` (HomeState, HomeIntent, HomeSideEffect)
- `HomeViewModel` (@HiltViewModel, init에서 LoadApps 자동 호출)
  - `distributeApps()`: 가나다순 정렬 후 4등분 배분
  - `loadJob` 중복 방지 처리
  - `loadApps()` 예외 처리: `CancellationException` 재전파, 그 외 `Exception` → `isLoading = false` + `ShowError` SideEffect
- `HomeViewModelTest` 12개 테스트 (MockK + Turbine)

#### Phase C: 정리
- `feature:launcher` `ExampleUnitTest.kt` 삭제

### 검증 결과
```
총 22개 테스트 통과 (failures=0, errors=0)
- AppEntityTest:              5개
- AppRepositoryTest:          3개
- GetInstalledAppsUseCaseTest: 2개
- HomeViewModelTest:          12개
  - 기본 동작 10개 (초기 상태, 로딩, 셀 토글, 앱 배분, SideEffect 등)
  - 예외 처리 2개 (ShowError SideEffect 발생, isLoading 복구)
```

### 설계 결정 및 근거
- **애니메이션 제외**: expandRatio 등 UI 애니메이션 값은 ViewModel 상태에 미포함. Compose `animateFloatAsState` / `updateTransition`으로 UI 레이어에서 처리.
- **앱 분배**: 전체 앱 리스트를 label 기준 가나다순 정렬 후 4등분. 앱 수가 4의 배수가 아닐 때는 올림 청크 + 빈 리스트 패딩.
- **Flow 중복 수집 방지**: `loadJob?.cancel()` 패턴으로 재로드 시 이전 Job 취소.
- **예외 처리**: `CancellationException`은 반드시 재전파 — `catch (Exception)`이 코루틴 취소를 삼키면 `loadJob?.cancel()` 패턴이 오작동함. 그 외 예외는 `isLoading` 복구 후 `ShowError`로 UI에 위임.

---

## [2026-02-20] 멀티 모듈 구조 설계 및 core:domain TDD

### 목표
StreamFan Minimal Launcher 프로젝트의 멀티 모듈 초기 구조를 완성하고, `core:domain`에 첫 Entity/Repository를 TDD로 작성한다.

### 모듈 의존성 그래프
```
:app → :core:domain, :core:data, :core:ui, :feature:launcher, :feature:apps-drawer
:core:data      → :core:domain
:core:ui        → :core:domain
:feature:launcher    → :core:domain, :core:ui
:feature:apps-drawer → :core:domain, :core:ui
:core:domain    → (없음, 순수 JVM)
```

### 변경 사항

#### Step 1 — Version Catalog (`gradle/libs.versions.toml`)
추가된 버전 및 라이브러리:

| 항목 | 버전 |
|------|------|
| hilt | 2.51.1 |
| ksp | 2.0.21-1.0.28 |
| coroutines | 1.8.1 |
| datastore-preferences | 1.1.4 |
| navigation-compose | 2.8.9 |
| turbine | 1.2.0 |
| mockk | 1.13.13 |

추가된 플러그인: `hilt-android`, `ksp`

#### Step 2 — 빌드 파일 수정 (7개 모듈)

| 모듈 | 주요 변경 |
|------|----------|
| `build.gradle.kts` (root) | hilt.android, ksp `apply false` 추가 |
| `app` | hilt+ksp 플러그인, 모든 하위 모듈 `implementation`, Navigation/Hilt/테스트 라이브러리 |
| `core:domain` | coroutines-core 추가, 테스트에 turbine/mockk 추가 |
| `core:data` | hilt+ksp 플러그인, `:core:domain` 의존, DataStore/coroutines-android, appcompat/material 제거 |
| `core:ui` | kotlin.compose 플러그인, `buildFeatures.compose = true`, Compose BOM, `:core:domain` 의존, appcompat/material 제거 |
| `feature:launcher` | kotlin.compose+hilt+ksp, `:core:domain`+`:core:ui` 의존, Navigation/Hilt/테스트 |
| `feature:apps-drawer` | launcher와 동일 패턴 |

#### Step 3 — core:domain TDD

**Red (테스트 먼저)**
- `AppInfoTest.kt` — AppInfo가 `packageName`, `appName`, `activityName` 보유 검증 + data class equals/hashCode/toString (5개 테스트)
- `AppRepositoryTest.kt` — `FakeAppRepository`로 인터페이스 계약 검증, Turbine으로 `Flow` 방출 검증 (3개 테스트)

**Green (구현)**
- `AppInfo.kt` — `data class AppInfo(packageName, appName, activityName)`
  - Icon은 Android `Drawable`이므로 순수 JVM 도메인 계층에서 제외
- `AppRepository.kt` — `interface AppRepository { fun getInstalledApps(): Flow<List<AppInfo>> }`
  - `Flow` 반환으로 앱 설치/삭제 시 반응형 업데이트 지원

**정리**
- `core/domain/src/main/.../domain/MyClass.kt` 삭제 (기존 placeholder)

### 검증 결과
```
./gradlew :core:domain:test  →  BUILD SUCCESSFUL  (8개 테스트 전부 통과)
./gradlew assembleDebug      →  BUILD SUCCESSFUL  (150개 태스크)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `core:domain`을 순수 JVM(`java-library`) 유지 | Android SDK 없이 JVM 테스트만으로 빠른 피드백 루프 확보 |
| `Flow<List<AppInfo>>` 반환 | 패키지 브로드캐스트 변경을 반응형으로 처리 |
| `FakeAppRepository` 패턴 | 실제 `PackageManager` 없이 인터페이스 계약만 검증 |
| Icon을 도메인에서 제외 | `Drawable`은 Android 의존성 → data/ui 레이어에서 처리 |

---
