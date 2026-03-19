# feature:apps-drawer

앱 서랍 화면. 설치된 앱 목록 표시, 초성/완성 검색, 드래그 앤 드롭 연동.

## 패키지 구조

```
org.comon.streamlauncher.apps_drawer
└── ui/
    └── AppDrawerScreen.kt   # 전체 앱 서랍 화면 (AppIcon 은 core:ui 에 위치)
```

## 주요 컴포넌트

### AppDrawerScreen

- `FocusRequester` 로 화면 진입 시 검색창 자동 포커스
- `LazyColumn` 으로 앱 목록 렌더링
- `derivedStateOf` 로 filteredApps 구독 최적화
- 페이지 이탈 시 소프트 키보드 자동 숨김

### AppIcon (core:ui/component/AppIcon.kt)

- `produceState` + `Dispatchers.IO` 로 비동기 아이콘 로드 (UI 블로킹 방지)
- Coil `AsyncImage` 사용
- feature:apps-drawer 와 feature:launcher 에서 공용으로 사용 → `core:ui` 에 배치됨

### AppDrawerItem (드래그 앤 드롭)

- `detectDragGesturesAfterLongPress` 로 드래그 시작
- `LocalDragDropState` (core:ui) 통해 DragDropState 접근
- 드래그 중 셀 오버레이로 배치 위치 표시

## 검색 규칙

- 검색은 `HomeViewModel` (feature:launcher) 에서 처리
- `ChosungMatcher` (core:domain/util) 로 초성/완성 검색
- debounce 100ms 적용 (`searchQuery` MutableStateFlow)

## 주의사항

- `AppDrawerScreen` 은 `HomeViewModel` 의 state/intent 를 직접 사용 (별도 ViewModel 없음)
- 앱 서랍 화면 자체의 상태는 `HomeState.searchQuery`, `HomeState.filteredApps` 에 포함됨
- 패키지명 언더스코어: `org.comon.streamlauncher.apps_drawer` (하이픈 모듈명 → 언더스코어 패키지)
