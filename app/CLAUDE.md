# app

앱 진입점. MainActivity, Application, 전역 네비게이션, 서비스.

## 주요 파일

```
org.comon.streamlauncher
├── StreamLauncherApplication.kt   # Application, Coil, WorkManager, AdMob
├── MainActivity.kt                # 단일 Activity, NavHost, ViewModels
├── navigation/
│   └── CrossPagerNavigation.kt   # VerticalPager + HorizontalPager 중첩
└── service/
    ├── PresetDownloadService.kt   # .slp 다운로드 Foreground Service
    └── PresetUploadService.kt     # 마켓 업로드 Foreground Service
```

## StreamLauncherApplication

- `@HiltAndroidApp`
- `ImageLoaderFactory`: 커스텀 Coil 설정 (AppIconFetcher, 15% 메모리 캐시, 50MB 디스크 캐시)
- `Configuration.Provider`: `HiltWorkerFactory` 로 WorkManager 초기화
- `MobileAds.initialize()`: AdMob 최초 초기화
- 알림 채널 생성: `preset_upload`, `preset_download`
- `FeedSyncWorker` 주기적 스케줄 (6시간)

## MainActivity

- `@AndroidEntryPoint`, `ComponentActivity`
- ViewModels (by viewModels()): `HomeViewModel`, `WidgetViewModel`, `FeedViewModel`, `SettingsViewModel`
- `AppWidgetHost` 수명주기: `onStart` → startListening, `onStop` → stopListening
- 배경화면 색상 감지 → 시스템 바 스타일 동적 변경
- `onNewIntent`: HOME intent 감지 → `resetTrigger` 증가 + `SettingsIntent.ResetTab`

### NavHost 라우트

| 라우트 | 화면 |
|--------|------|
| `LAUNCHER` | CrossPagerNavigation (기본) |
| `settings_detail/{menu}` | SettingsDetailScreen |
| `preset_market/...` | 프리셋 마켓 (내부 NavHost) |

## CrossPagerNavigation

- **구조**: `VerticalPager(3p)` 안에 `HorizontalPager(3p)` 중첩
  - 중앙(1,1): 홈 그리드
  - 왼쪽(1,0): 피드
  - 오른쪽(1,2): 앱 서랍
  - 위(0,1): 위젯
- **슬롯**: `homeContent`, `feedContent`, `appDrawerContent`, `widgetContent`
- `resetTrigger` Int → `LaunchedEffect` → `animateScrollToPage(1, tween(300))` 홈 복귀
- 드래그 중 Pager 스크롤 차단
- 드래그 오버레이 레이어 포함
- `BackHandler`: 뒤로가기 시 홈으로 복귀

## 릴리스 빌드

- R8 활성화: `isMinifyEnabled = true`, `isShrinkResources = true`
- ProGuard 규칙: `proguard-rules.pro`
- Signing: `release` 빌드 타입에 keystore 설정 필요

## 주요 AndroidManifest 설정

- `android:launchMode="singleTask"` (홈 버튼 복귀)
- `<intent-filter>` HOME + DEFAULT category (기본 런처 등록)
- `<queries>`: 설치된 앱 목록 조회 (Android 11+)
- 권한: `INTERNET`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `VIBRATE`

## BuildConfig

- `buildFeatures { buildConfig = true }` 활성화
- `BuildConfig.VERSION_NAME` 사용 가능
