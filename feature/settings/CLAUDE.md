# feature:settings

설정 화면. 색상 테마, 배경 이미지, 피드 설정, 앱 서랍 설정, 프리셋 관리.

## 패키지 구조

```
org.comon.streamlauncher.settings
├── SettingsContract.kt     # SettingsState / SettingsIntent / SettingsSideEffect
├── SettingsViewModel.kt
├── model/
│   └── ImageType.kt        # 배경 이미지 타입 enum
├── navigation/
│   ├── SettingsRoute.kt    # 라우트 상수
│   └── SettingsMenu.kt     # 메뉴 enum (COLOR, IMAGE, FEED, APP_DRAWER, PRESET)
├── ui/
│   ├── SettingsScreen.kt       # 메인 설정 목록
│   ├── SettingsDetailScreen.kt # 공통 Scaffold+TopAppBar 래퍼
│   └── ...                     # 각 설정 세부 화면
└── upload/                 # 프리셋 마켓 업로드 관련 UI
```

## SettingsViewModel 책임

- `checkNotice(version: String)`: 공지 팝업 표시 여부 확인 (public 메서드)
- `UploadPresetToMarketUseCase` + `GetCurrentMarketUserUseCase` 주입
- `uploadPreset()`: 로그인 확인 → 마켓 업로드

## 네비게이션 구조

```
MainActivity NavHost
├── LAUNCHER (CrossPagerNavigation)
└── settings_detail/{menu}  (SettingsDetailScreen + 각 세부 화면)
```

- `SettingsMenu.valueOf(menu)` 로 라우트 파라미터 파싱
- 세부 화면 진입: `NavigateToMain` SideEffect → MainActivity에서 navController 처리
- 슬라이드 전환 애니메이션 (horizontal slide)

## SideEffect 목록

| SideEffect | 설명 |
|------------|------|
| `NavigateToMain` | 설정에서 메인으로 복귀 |
| `UploadSuccess` | 프리셋 마켓 업로드 성공 |
| `UploadError` | 업로드 실패 (에러 메시지 포함) |
| `RequireSignIn` | 로그인 필요 |

## 설정 세부 화면 규칙

- `SettingsDetailScreen` Scaffold 래퍼 공통 사용 (TopAppBar 뒤로가기 포함)
- `ColorSettingsContent`: 3열 프리셋 그리드
- `ImageSettingsContent`: 2×2 셀 + `PickVisualMedia` 런처
- `FeedSettingsContent`: `OutlinedTextField` 3개 + 배경이미지 + 실시간 검증 + 저장 버튼
- `PresetSettingsContent`: "프리셋 마켓" 버튼 + 공유 버튼 + `UploadToMarketDialog`

## feature:launcher 와의 관계

- `feature:launcher` 는 `feature:settings` 에 **의존하지 않음** (단방향)
- 설정 변경은 `DataStore` 경유 → `HomeViewModel` 이 collect 해서 반영
- `SettingsViewModel` 과 `HomeViewModel` 은 `MainActivity` 에서 각각 `by viewModels()` 로 생성

## 테스트

- `SettingsViewModelTest`: 13개 테스트
- `ResetTab` intent → `NavigateToMain` SideEffect 발행 검증 포함
