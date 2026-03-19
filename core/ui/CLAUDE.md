# core:ui

공유 Composable 컴포넌트, 테마, MVI BaseViewModel. feature 모듈들이 공통으로 사용.

## 패키지 구조

```
org.comon.streamlauncher.ui
├── BaseViewModel.kt        # MVI 기반 추상 ViewModel
├── component/
│   ├── AppIcon.kt              # 앱 아이콘 Composable (produceState + Dispatchers.IO)
│   ├── AppIconFetcher.kt       # Coil 커스텀 Fetcher (PackageManager 아이콘 로드)
│   ├── AppGridSizing.kt        # 그리드 셀 크기 계산 유틸리티
│   └── GoogleSignInRequiredDialog.kt  # 로그인 요구 공통 다이얼로그
├── dragdrop/              # DragDropState, LocalDragDropState
├── modifier/              # Compose Modifier 확장 (glassEffect, neonGlow)
├── theme/                 # StreamLauncherTheme, 색상, 타이포그래피
└── util/                  # Window 유틸리티 (insets 등)
```

## BaseViewModel

```kotlin
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiSideEffect> : ViewModel() {
    // state: StateFlow<S> (updateState { copy(...) } 로 갱신)
    // sideEffect: Flow<E> (Channel.BUFFERED 기반)
    // abstract fun handleIntent(intent: I)
}
```

- 상태 변경: `updateState { copy(field = value) }`
- 사이드 이펙트 발행: `sendEffect(effect)`
- 외부 노출: `state: StateFlow`, `sideEffect: Flow` (읽기 전용)

## StreamLauncherTheme

- `accentPrimaryOverride` / `accentSecondaryOverride` 파라미터로 동적 색상 지원
- `MaterialTheme.colorScheme.*` 에서 accent 색상을 override 값으로 교체
- 기본 폰트 포함 (StreamLauncherTheme 내에서 Typography 정의)

## 컴포넌트 규칙

- **배치 기준**: 2개 이상의 feature 모듈에서 필요하면 `core:ui` 에 배치
- **단일 feature 전용 UI** → 해당 feature 모듈 내에 위치
- 크로스 feature 공유 상태 (예: `DragDropState`) → `core:ui/dragdrop/` 에 배치
- `LocalDragDropState` → `CompositionLocalProvider` 패턴으로 하위 트리에 전달

### 주요 공유 컴포넌트

- `AppIcon`: `produceState` + `Dispatchers.IO` 비동기 아이콘 로드 (feature:apps-drawer, feature:launcher 공용)
- `AppIconFetcher`: Coil 커스텀 Fetcher — `StreamLauncherApplication` 의 `ImageLoaderFactory` 에 등록
- `AppGridSizing`: 그리드 셀 크기 계산 (화면 크기 기반)
- `GoogleSignInRequiredDialog`: 마켓 기능 사용 전 로그인 요구 다이얼로그

## Modifier 확장

- `glassEffect()`: 반투명 유리 효과 (BackdropFilter)
- `neonGlow()`: 네온 글로우 효과

## 주의사항

- `combinedClickable` → `@OptIn(ExperimentalFoundationApi::class)` 필요
- `Icons.Default.*` 에 없는 아이콘 → `material-icons-extended` 의존성 필요
  - 대체 예: `Icons.Default.PushPin` X → `Icons.Default.Star` O (기본 세트)
- 좌표 비교 시 `positionInRoot()` 사용 (Pager 등 다른 레이아웃 컨텍스트 간)
