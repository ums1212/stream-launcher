# feature:launcher

홈 런처 메인 화면. 2×2 그리드, 피드, 드래그 앤 드롭 배치.

## 패키지 구조

```
org.comon.streamlauncher.launcher
├── HomeContract.kt        # HomeState / HomeIntent / HomeSideEffect
├── FeedContract.kt        # FeedState / FeedIntent / FeedSideEffect
├── HomeViewModel.kt
├── FeedViewModel.kt
└── ui/
    ├── HomeScreen.kt      # 그리드 화면
    ├── FeedScreen.kt      # 피드 화면 (Chzzk/YouTube)
    └── ...
```

## HomeViewModel 책임

- 앱 목록 로드 (`GetInstalledAppsUseCase`) 및 4등분 배분 (`distributeApps`)
- 셀 고정 배치 (`cellAssignments`) — 핀 고정 앱 우선 배치 + 자동 4등분
- 검색 쿼리 debounce 100ms → `filterApps` (초성/완성 검색)
- 드래그 앤 드롭: `AssignAppToCell`, `UnassignApp` intent 처리
- 설정 로드: `GetLauncherSettingsUseCase` collect (초기화 시)

## FeedViewModel 책임

- Chzzk 라이브 상태, YouTube/RSS 통합 피드 로드
- 60초 쿨다운 (중복 새로고침 방지), `loadJob` 중복 방지
- 채널 프로필 병렬 로드 (`profileJob`) — 실패 시 silent

## HomeState 주요 필드

- `gridCells: Map<GridCell, List<AppEntity>>` — 4개 셀의 앱 목록
- `expandedCell: GridCell?` — 현재 확장된 셀
- `searchQuery: String`, `filteredApps: List<AppEntity>` — 검색
- `colorPresetIndex: Int` — 현재 색상 테마 인덱스
- `cellAssignments: Map<GridCell, String>` — 핀 고정 (packageName)

## 주요 패턴

### 중복 Job 방지
```kotlin
private var loadJob: Job? = null
// ...
loadJob?.cancel()
loadJob = viewModelScope.launch { ... }
```

### distributeApps
핀 고정 앱 먼저 지정 셀에 배치 → 나머지 가나다순 4등분.
`pinnedPackages` computed property로 핀 목록 관리.

### CrossPagerNavigation 연동

- `resetTrigger: Int` + `LaunchedEffect` → `animateScrollToPage(1, tween(300))`
- 드래그 중 Pager 스크롤 차단
- `onScrollToHome` 콜백으로 홈 복귀

## 피드 화면 규칙

- `LiveStatusCard`: Breathing 네온 글로우 애니메이션
- `ChannelProfileCard`: CircleCropTransformation 원형 아바타, AnimatedContent 구독자 수
- `formatSubscriberCount()`: 만/천 단위 포맷
- 피드 배경 이미지: `alpha=0.4f` + `blur=8.dp` (레이어 합성)

## 테스트

- `HomeViewModelTest`: 25개 테스트
- `FeedViewModelTest`: 피드 로드, 쿨다운, 채널 프로필 테스트
- `makeViewModel()` 헬퍼 패턴 사용
