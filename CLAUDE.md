# StreamLauncher — 프로젝트 공통 규칙

## 프로젝트 개요

Android 홈 런처 앱. 2×2 그리드, 앱 서랍(초성 검색), 스트리밍 피드(Chzzk/YouTube), 위젯, 프리셋 마켓(Firebase) 기능 포함.

- **패키지**: `org.comon.streamlauncher`
- **SDK**: minSdk 28 / compileSdk 36 / targetSdk 36
- **언어**: Kotlin 2.x + Jetpack Compose
- **버전 관리**: `gradle/libs.versions.toml` (Version Catalog)

## 모듈 구조

```
:app
:core:domain   ← 순수 JVM (Android 의존성 없음)
:core:data     ← 저장소 구현, Hilt DI, Room, DataStore, Firebase
:core:ui       ← 공유 Composable, 테마, BaseViewModel
:core:network  ← Retrofit, OkHttp, API 서비스
:feature:launcher
:feature:apps-drawer
:feature:widget
:feature:settings
:feature:preset-market
```

의존 방향은 **단방향**: feature → core:ui/domain, core:data → core:domain/network. feature 간 직접 의존 금지.

## 아키텍처: Clean Architecture + MVI

- **Domain**: 모델, Repository 인터페이스, UseCase (순수 Kotlin)
- **Data**: Repository 구현, DI 모듈
- **Presentation**: Contract(State/Intent/SideEffect) + ViewModel + Composable Screen

### UseCase 규칙

- 클래스 하나에 책임 하나
- `@Inject constructor` + `operator fun invoke()` 패턴 필수
- Domain 레이어에만 위치 (`core:domain/usecase/`)

### MVI Contract 규칙

- `*Contract.kt` 한 파일에 `*State`, `*Intent`, `*SideEffect` 모두 정의
- State는 data class + `UiState` 구현
- Intent는 sealed interface + `UiIntent` 구현
- SideEffect는 sealed interface + `UiSideEffect` 구현
- ViewModel은 `BaseViewModel<S, I, E>` 상속, `@HiltViewModel`

## Gradle / 빌드

- Gradle 실행 시 반드시 `JAVA_HOME="C:/Program Files/Java/jdk-17"` 설정 (Windows bash)
- 라이브러리 모듈에서 `BuildConfig` 사용 시 `buildFeatures { buildConfig = true }` 명시 필요
- Firebase BOM 34.x: `-ktx` 접미사 제거됨 → `firebase-firestore` (not `-ktx`)
- 새 의존성 추가 시 반드시 `libs.versions.toml`에 먼저 등록 후 사용

## Hilt DI 규칙

- 모든 DI 모듈: `@InstallIn(SingletonComponent::class)` + `@Singleton`
- 바인딩: `@Binds` (인터페이스 → 구현체), `@Provides` (외부 객체)
- `@Binds` 와 `@Provides` 를 한 모듈에 혼용 시 abstract class + companion object 패턴 사용

## 테스트 규칙

- **프레임워크**: JUnit 4 + MockK + Turbine + kotlinx-coroutines-test
- **코루틴**: `StandardTestDispatcher` + `Dispatchers.setMain/resetMain`
- **테스트 이름**: 한국어 backtick 함수명 (예: `` `초기 상태 - isLoading false` ``)
- **ViewModel 테스트**: `makeViewModel()` 헬퍼로 생성
- **Flow 검증**: Turbine `flow.test { awaitItem(); cancelAndIgnoreRemainingEvents() }`
- nullable 파라미터를 가진 UseCase mock → `mockk(relaxed = true)` 사용 (`any<String?>()` 컴파일 오류)

## Compose 규칙

- `combinedClickable` → `@OptIn(ExperimentalFoundationApi::class)` 필요
- `Icons.Default.*` 에 없는 아이콘 → `material-icons-extended` 의존성 확인
- 크로스 feature 공유 상태/컴포넌트 → `core:ui` 에 배치
- 좌표 비교 시 `positionInRoot()` 사용해 좌표계 통일

## 개발 로그

- 파일: `docs/devlog.md`
- 최신 항목이 상단 (newest first)
- 새 항목은 기존 첫 번째 `---` 위에 삽입
- 구성: 날짜+제목 / 목표 / 변경사항 / 검증결과 / 설계결정 및 근거
