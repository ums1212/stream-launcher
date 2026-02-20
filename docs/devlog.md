# StreamLauncher 개발 로그

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
