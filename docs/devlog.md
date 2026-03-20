# StreamLauncher 개발 로그

---

## [2026-03-20] security: 보안 취약점 수정 (SSRF·로그 유출·백업 노출·ZIP Bomb·Firebase)

### 목표

- HIGH 1건 / MEDIUM 7건 / LOW 3건으로 분류된 보안 감사 결과를 코드에 반영
- 사용자 입력이 URL에 직접 보간되는 SSRF 위험 제거
- 릴리스 빌드에서 API 키 메타데이터·채널 ID 등이 로그로 유출되는 경로 차단
- 백업·추출 규칙 미설정으로 DataStore·DB 전체가 클라우드 백업되던 문제 해결
- 악의적인 `.slp` 파일로 인한 ZIP Bomb(디스크/메모리 고갈) 방어
- Firebase Storage URL 검증 없이 임의 버킷 파일에 접근 가능하던 문제 수정
- UI 입력 필드의 형식 검증 미흡 및 길이 제한 없음 문제 해결
- Firestore/Storage 보안 규칙 버전 관리 체계화

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| **NEW** `core/domain/.../util/InputValidator.kt` | `isValidChzzkChannelId` (32자리 hex) + `isValidYoutubeChannelId` (UC… / @handle) 순수 Kotlin 검증 유틸 |
| **NEW** `core/domain/.../util/InputValidatorTest.kt` | 정상·빈값·경로 조작·특수문자 등 19개 단위 테스트 |
| `core/network/.../api/ChzzkService.kt` | `@GET @Url url: String` → `@GET("service/v3/channels/{channelId}/live-detail") @Path channelId` 변경으로 URL 보간 제거 |
| `core/data/.../repository/FeedRepositoryImpl.kt` | `InputValidator` import 추가; `getLiveStatus` / `getYoutubeLiveStatus` / `fetchYoutubeItems` / `getChannelProfile` 진입 시 검증 호출; `YOUTUBE_API_KEY.length` 로그 삭제; 수동 URL 조립 제거 |
| `app/proguard-rules.pro` | `-assumenosideeffects class android.util.Log { v(…); d(…); }` 추가 — 릴리스 빌드에서 Log.v/d 제거 |
| `app/src/main/res/xml/backup_rules.xml` | `datastore/`, `market_presets/`, `database/.` 제외 규칙 명시 (API < 31) |
| `app/src/main/res/xml/data_extraction_rules.xml` | `cloud-backup` + `device-transfer` 양쪽에 동일 제외 규칙 명시 (API 31+) |
| **NEW** `app/src/main/res/xml/network_security_config.xml` | `cleartextTrafficPermitted="false"` — HTTP 평문 통신 전면 차단 |
| `app/src/main/AndroidManifest.xml` | `android:networkSecurityConfig="@xml/network_security_config"` 추가 |
| `core/data/.../slp/SlpUnpacker.kt` | `MAX_ENTRIES=50` / `MAX_TOTAL_SIZE=50MB` 상수 추가; 엔트리 수 초과 시 `SecurityException`; 누적 바이트 추적으로 크기 초과 시 `SecurityException` + 기추출 파일 정리; `readBytes()` → 청크 단위 읽기로 교체 |
| `core/data/.../datasource/FirebaseMarketStorageDataSource.kt` | `validateStorageUrl()` private 메서드 추가; `downloadToFile()` 진입 시 `gs://버킷/` 또는 `https://firebasestorage…/버킷/` prefix 검증 |
| `feature/settings/.../ui/FeedSettingsContent.kt` | 공백 포함 여부 → `InputValidator` 형식 검증으로 교체; 빈 값은 허용(선택 필드); Chzzk/YouTube 에러 메시지 문자열 리소스 참조 |
| `feature/settings/.../ui/UploadToMarketDialog.kt` | description `onValueChange`에 500자 상한 추가; `supportingText`로 현재 글자 수 표시 |
| `feature/settings/.../res/values/strings.xml` | `settings_chzzk_id_format_error` / `settings_youtube_id_format_error` 문자열 추가 |
| **NEW** `firestore.rules` | `market_presets` 읽기 공개; 쓰기 인증+authorUid 검증+필드 길이 제한; 삭제 소유자만 |
| **NEW** `storage.rules` | `market_presets/` 읽기 공개; 쓰기 인증+10MB+content-type 검증; 삭제 경로 uid 일치 |

### 검증결과

- `./gradlew assembleDebug` BUILD SUCCESSFUL (deprecation 경고만, 에러 없음)
- `./gradlew testDebugUnitTest` BUILD SUCCESSFUL — 전체 테스트 통과 (InputValidatorTest 19개 신규 포함)

### 설계결정 및 근거

- **InputValidator를 core:domain에 배치**: 순수 JVM 유틸이므로 Android 의존 없이 여러 모듈(core:data, feature:settings)에서 재사용 가능. feature → core:domain 단방향 의존 규칙 준수.
- **ChzzkService @Path 전환**: Chzzk Retrofit은 이미 `https://api.chzzk.naver.com/` baseUrl을 사용하므로 `@Path`로 전환하면 URL 전체를 사용자 입력으로 조립할 필요가 없어 SSRF 경로 차단. `@Url` 방식은 임의 호스트 지정이 가능해 취약.
- **Log.d/v는 -assumenosideeffects로 제거**: 소스 레벨 삭제 대신 ProGuard 규칙으로 처리 — 디버그 빌드의 로그 가독성은 유지하면서 릴리스에서만 제거되어 개발 경험과 보안 요구 동시 충족.
- **ZIP Bomb 방어 전략**: 엔트리 수(50개)와 누적 압축 해제 크기(50MB) 두 축으로 방어. 크기 초과 시 이미 추출된 파일까지 정리하여 부분 추출 잔여물 없음. 청크 읽기로 단일 엔트리 크기도 제어.
- **FeedSettingsContent 검증 교체**: 공백 포함 여부 → 형식 검증으로 강화. 빈 값은 선택 필드이므로 허용 (사용자가 채널 설정을 안 하는 경우 정상 상태).

---

## [2026-03-20] fix(error-handling): ANR 위험 코드 점검 + 사용자 에러 스낵바 구현

### 목표

- `ShowError` SideEffect가 `Log.e()`만 호출하고 사용자에게 표시되지 않던 문제 해결
- Home/Feed 영역에 SnackbarHost가 없어 스낵바를 표시할 수 없던 문제 해결
- Raw exception 메시지(`e.message`)가 사용자에게 그대로 노출되던 케이스 제거
- 누락된 try-catch로 인한 미처리 예외 크래시 위험 제거
- `GoogleSignInHandler`에서 Credential 예외 메시지가 스낵바에 붙어 출력되던 문제 해결
- State의 `errorMessage`/`error` 필드에 raw 예외 메시지가 저장되어 인라인 노출되던 문제 해결

### 변경사항

- `SideEffectHandlers.kt`: `HomeSideEffectHandler`, `FeedSideEffectHandler`에 `snackbarHostState` 파라미터 추가, `ShowError` 분기에 `dismiss()`+`showSnackbar()` 추가
- `MainActivity.kt`: `settingsSnackbarHostState` → `snackbarHostState`로 통합, `Box` 래퍼 + 전역 `SnackbarHost`(SwipeToDismissBox) 추가, Home/Feed 핸들러에 snackbarHostState 전달
- `HomeViewModel.kt`: `e.message` → `"앱 목록을 불러올 수 없습니다"`
- `FeedViewModel.kt`: `e.message` → `"피드를 불러올 수 없습니다"` + `errorMessage = e.message` state 저장 제거
- `FeedContract.kt`: `errorMessage: String?` 필드 삭제
- `FeedScreen.kt`: `state.errorMessage ?: feed_no_items` → `feed_no_items` 고정 (인라인 에러 표시 제거)
- `SettingsViewModel.kt`: 에러 메시지 3곳 교체 + `loadPreset`/`deletePreset`/`saveFeedSettings`/`saveAppDrawerSettings`에 `runCatching` 래핑
- `PresetDetailViewModel.kt`: 에러 메시지 4곳 교체 (다운로드/로드/좋아요/로그인) + `error = e.message` state 저장 제거
- `PresetMarketViewModel.kt`: 에러 메시지 교체 + `loadTopPresets()` 실패 시 `ShowError` SideEffect 발송 + `error = error?.message` → `null`
- `PresetMarketUserInfoViewModel.kt`: `e.message` → `"프리셋 목록을 불러올 수 없습니다"`
- `GoogleSignInHandler.kt`: `signInErrorText.format(e.message)` (포맷 문자열 + raw 예외) → `signInFailedText` 고정 메시지
- `PresetDetailScreen.kt`, `MarketHomeScreen.kt`, `PresetMarketUserInfoScreen.kt`: `SnackbarHost`에 `SwipeToDismissBox` 적용

### 검증결과

- `assembleDebug` 빌드 성공 (deprecation 경고만, 에러 없음)
- `./gradlew test` 전체 통과 (HomeViewModelTest/FeedViewModelTest 기댓값 2건 업데이트 포함)

### 설계결정 및 근거

- 단일 `snackbarHostState`를 `MainActivity`에서 생성하여 Home/Feed/Settings 핸들러 모두에 전달 — 동시 스낵바 충돌 방지, `currentSnackbarData?.dismiss()` 연쇄 패턴으로 즉시 교체
- 에러 노출 경로를 SideEffect → 스낵바 단일 채널로 통일 — state의 `error`/`errorMessage` 필드는 UI 렌더링에서 완전히 분리
- ANR 위험 없음 확인 — 모든 I/O 작업이 적절한 디스패처에서 실행되고 있었음
- `SwipeToDismissBox(confirmValueChange)` API는 deprecated이나 Material3 최신 대안이 아직 불안정하여 현재 방식 유지 (기능 정상 동작, deprecation 경고만)

---

## [2026-03-19] design(theme): 컬러 프리셋을 전체 ColorScheme에 반영

### 목표

- 컬러 프리셋 선택 시 `primary`/`tertiary` 7개 속성만 반영되던 한계 해소
- `onSurfaceVariant`(22곳), `onSurface`(8곳), `outlineVariant`(3곳), `secondaryContainer`(2곳) 등 나머지 속성이 기본값을 유지하여 발생하던 시각적 불일치 해결
- `glassSurface`/`glassOnSurface`/`gridBorder` 등 `StreamLauncherColors`도 자동 갱신

### 변경사항

| 파일 | 내용 |
|------|------|
| `core/ui/.../theme/Theme.kt` | `baseColorScheme.copy()` 블록을 ~25개 속성으로 확장; `StreamLauncherColors` 베이스 계산을 Dynamic Color 분기 없이 단일 `colorScheme` 파생으로 단순화; override 블록에 `gridBorder`/`glassSurface`/`glassOnSurface` 추가 |

**확장된 colorScheme 속성 그룹**

- **Primary/Tertiary container**: `primaryContainer`, `onPrimaryContainer`, `tertiaryContainer`, `onTertiaryContainer`
- **Secondary** (tertiary에서 파생): `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`
- **On-surface**: `onSurface`, `onSurfaceVariant`, `onBackground` — MD3 기본값에서 accent를 5~10% lerp
- **Surface family**: `surfaceDim`, `surfaceBright`, `surfaceContainerLowest/Low/Container/High/Highest`, `surfaceTint`
- **Outline**: `outline`, `outlineVariant`
- **Inverse**: `inverseSurface`, `inverseOnSurface`, `inversePrimary`
- **Error 색상**: 변경 없음 (시맨틱 의미 보존)

### 검증결과

- `:core:ui:compileDebugKotlin` BUILD SUCCESSFUL
- `:app:assembleDebug` BUILD SUCCESSFUL
- 전체 테스트 `./gradlew test` BUILD SUCCESSFUL (실패 0건)

### 설계결정 및 근거

- **lerp 비율 최소화**: on-surface 계열은 5~10%, surface 계열은 1~16%로 낮게 설정 — 가독성(대비비 4.5:1) 유지를 우선함
- **Error 계열 불변**: `error`/`onError`/`errorContainer`/`onErrorContainer`는 "삭제·취소" 시맨틱을 갖는 예약 색상이므로 accent 영향을 주지 않음
- **StreamLauncherColors 단순화**: Dynamic Color 분기를 제거하고 항상 `colorScheme`에서 파생 → accent override 시 `glassSurface`, `glassOnSurface`, `gridBorder`도 자동으로 accent 반영, 분기 중복 제거
- **secondary = tertiary**: MD3 권장 패턴으로, accentSecondary가 secondary 계열에도 일관되게 표현됨

---

## [2026-03-19] feat(preset-market): MarketHomeScreen landscape/portrait 레이아웃 분리

### 목표

- MarketHomeScreen이 portrait 전용 레이아웃으로만 구성되어 landscape에서 사용성이 떨어지던 문제 해결
- Settings/UserInfo 화면과 동일한 `calculateIsCompactHeight()` 분기 패턴으로 통일

### 변경사항

**신규 생성 (4개 파일)**
- `ui/MarketHomePortraitContent.kt` — PrimaryTabRow, TopPresetPager(3초 auto-scroll HorizontalPager), Paging LazyColumn
- `ui/MarketHomeLandscapeContent.kt` — 커스텀 사이드탭(Column + SideTabItem) + 오른쪽 콘텐츠 영역. `LandscapeSideTab` enum (UI-only 로컬 상태). Top10 탭은 `TopPresetPager` 재사용, Recent 탭은 `LazyRow { MarketPresetLandscapeCard }`
- `ui/component/MarketPresetLandscapeCard.kt` — LazyRow용 세로형 카드(width=200.dp, thumbnail 120.dp). `fromCard` 파라미터로 sharedBounds key 분기
- `res/values/strings.xml` — `preset_market_side_tab_top10` 추가

**수정 (1개 파일)**
- `ui/MarketHomeScreen.kt` — Orchestrator: Scaffold content 내 `Column { SearchBar(portrait only), AdMobBanner, content(weight=1f) }` 구조. `TopPresetPager` → `MarketHomePortraitContent.kt`로 이동(internal), landscape 시 TopAppBar에 Search 아이콘 추가

### 검증결과

- `:feature:preset-market:assembleDebug` BUILD SUCCESSFUL
- `:app:assembleDebug` BUILD SUCCESSFUL

### 설계결정 및 근거

- **ViewModel/Contract 변경 없음**: `LandscapeSideTab`은 UI-only 로컬 상태이므로 ViewModel 수정·테스트 추가 불필요
- **SearchBar portrait only**: `!isCompactLandscape`일 때만 표시. landscape는 TopAppBar의 Search 아이콘으로 대체. sharedBounds 애니메이션은 SearchBar가 표시될 때만 동작하므로 문제없음
- **AdMob 공통 배치**: SearchBar 아래, content 위. portrait/landscape 구분 없이 항상 표시
- **SideTabItem 커스텀 구현**: `NavigationRailItem`의 indicator가 Material3 내부 토큰으로 고정 크기라 `fillMaxWidth()` 적용 불가 → `Surface + Column` 커스텀으로 교체. `width(IntrinsicSize.Max)`로 Column이 가장 넓은 아이템 너비에 맞추고 각 항목은 `fillMaxWidth()`로 채움
- **SideTabItem 색상**: 선택 시 `MaterialTheme.colorScheme.primary` / `onPrimary` 사용 → 설정에서 지정한 accent 컬러(`accentPrimaryOverride` → `primary` 매핑)가 자동 반영
- **TopPresetPager 재사용**: `private` → `internal`로 변경 + `modifier` 파라미터 추가로 landscape Top10 탭에서 동일 컴포넌트 공유

---

## [2026-03-19] refactor(app): MainActivity 분리 — 5개 파일로 역할별 추출, ~600줄 → ~160줄

### 목표

- MainActivity.kt가 ~600줄로 비대해진 문제 해결
- 월페이퍼 감지, 위젯 호스트, 사이드이펙트 핸들링, 네비게이션, Google 로그인을 역할별 파일로 분리
- ViewModel 4개는 `onNewIntent` / `ActivityResultLauncher` 콜백 접근 필요로 Activity에 유지

### 변경사항

| 파일 | 내용 |
|------|------|
| **NEW** `app/.../wallpaper/WallpaperSystemBarManager.kt` | 월페이퍼 색상 감지·리스너·시스템바 스타일 동적 전환. `@Composable fun ObserveDestinationChanges()` 포함 |
| **NEW** `app/.../widget/AppWidgetHostManager.kt` | `AppWidgetHost` 수명주기, `configureWidgetLauncher`·`pickWidgetLauncher`, `launchWidgetPicker()`·`deleteWidget()` 캡슐화 |
| **NEW** `app/.../effect/SideEffectHandlers.kt` | `HomeSideEffectHandler`, `FeedSideEffectHandler`, `SettingsSideEffectHandler` 3개 Composable. `this@MainActivity` → `LocalContext.current` 전환, suspend `showSnackbar` 직접 호출 |
| **NEW** `app/.../ui/GoogleSignInFlow.kt` | `showSignIn`·`showDialog` 상태 + `GoogleSignInRequiredDialog`·`GoogleSignInHandler` 조건 렌더링 캡슐화. 트리거 람다 반환 |
| **NEW** `app/.../navigation/MainNavHost.kt` | `NavHost` 블록 전체 추출. `launcherContent: @Composable () -> Unit` 슬롯으로 `CrossPagerNavigation` 배선은 MainActivity에 유지 |
| **MODIFY** `app/.../MainActivity.kt` | ~600줄 → ~160줄. lifecycle 콜백 각 1줄 위임, `setContent` 내부는 상태 수집 + 핸들러 호출 + NavHost 슬롯 구성 |
| **FIX** `AppWidgetHostManager.kt` | `ComponentActivity.RESULT_OK` → `Activity.RESULT_OK` (import android.app.Activity) |
| **FIX** `MainNavHost.kt` | `onStartDownloadService: () -> Unit` → `(String) -> Unit` (`PresetMarketHost` 실제 시그니처에 맞춤) |

### 검증결과

- `assembleDebug` BUILD SUCCESSFUL
- `test` BUILD SUCCESSFUL (실패 0건, 리팩토링이므로 신규 테스트 없음)

### 설계결정 및 근거

- **ViewModel Activity 유지**: `HomeViewModel`·`SettingsViewModel`은 `onNewIntent`에서, `WidgetViewModel`은 `ActivityResultLauncher` 콜백에서 인텐트를 디스패치 → Composition 외부 접근 필요 → `hiltViewModel()`로 이동 불가. `FeedViewModel`은 일관성 유지로 함께 유지
- **`AppWidgetHostManager` 초기화 위치**: `registerForActivityResult`는 `onStart` 이전 호출 필수 → `onCreate` 첫 줄에서 초기화 (widgetViewModel lazy delegate 접근 전). `by viewModels()`는 lazy이므로 필드 레벨에서 `widgetViewModel`을 캡처하면 Hilt 미초기화 상태에서 VM이 생성될 위험 있어 `lateinit var` 선택
- **`WallpaperSystemBarManager` 필드 레벨 초기화**: ViewModel 의존 없고 생성자에서 외부 호출 없음 → `val wallpaperManager = WallpaperSystemBarManager(this)` 필드 초기화 안전
- **`SideEffectHandlers` suspend 직접 호출**: `LaunchedEffect` 내 `collect` 람다는 코루틴 컨텍스트이므로 `settingsScope.launch {}` 없이 `snackbarHostState.showSnackbar()` 직접 호출 가능 → 코드 단순화
- **`launcherContent` 슬롯**: `CrossPagerNavigation` 배선(4개 ViewModel + navController)을 MainActivity에 남기고 슬롯으로 전달 — 4개 ViewModel 합류 지점이므로 추출해도 복잡도 불변
- **`onStartDownloadService: (String) -> Unit`**: `PresetDetailScreen`이 `effect.presetName`을 인자로 전달 → 실제 시그니처는 `(String) -> Unit`. 기존 코드에서 파라미터를 무시하던 람다(`{ startForegroundService(...) }`)는 Kotlin에서 `(String) -> Unit`으로 암묵적 수용 — 빌드는 통과했으나 presetName 미전달은 기존 버그 유지(별도 이슈)

---

## [2026-03-19] refactor(preset-market): 네비게이션 전환 애니메이션 — "홈 고정 + 새 화면 오른쪽 슬라이드인" 패턴 적용

### 목표

- 마켓 홈 → 검색/상세 이동 시 홈 화면이 fadeOut 되지 않고 고정된 채, 새 화면이 오른쪽에서 슬라이드인 되는 UX 구현
- 설정 상세 → 프리셋 마켓 이동도 동일 패턴 적용

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/preset-market/ui/PresetMarketHost.kt` | `MarketHome.exitTransition` → `ExitTransition.None`, `popEnterTransition` → `EnterTransition.None` |
| `feature/preset-market/ui/PresetMarketHost.kt` | `MarketSearch` / `MarketDetail` enterTransition → `slideInHorizontally(initialOffsetX = { it })`, popExitTransition → `slideOutHorizontally(targetOffsetX = { it })` |
| `app/src/main/java/.../MainActivity.kt` | `SettingsDetail.exitTransition` → `ExitTransition.None`, `popEnterTransition` → `EnterTransition.None` (PresetMarket 이동 시 SettingsDetail 고정) |

### 검증결과

- 코드 리뷰 수준 검토 (빌드 미실행)

### 설계결정 및 근거

- `ExitTransition.None` 패턴: 현재 화면이 스택에 남아 고정되고 새 화면의 enterTransition만 재생 → "모달 위에 새 레이어가 올라오는" 자연스러운 계층감 연출
- 뒤로가기 시 `popExitTransition`으로 새 화면이 오른쪽으로 슬라이드아웃, 기존 화면은 `EnterTransition.None`으로 자연스럽게 드러남
- `SettingsDetail → PresetMarket` 구간에서도 동일 원칙 적용: exitTransition/popEnterTransition만 None으로 교체, popExitTransition(Launcher 복귀 시 슬라이드아웃)은 유지

---

## [2026-03-19] refactor(core:data): Firebase DataSource 추상화 — MarketPresetRepositoryImpl 분리

### 목표

- `MarketPresetRepositoryImpl`(244줄)이 FirebaseAuth/Firestore/Storage를 직접 호출하는 구조 개선
- DataSource 계층 도입으로 Firebase SDK 의존성 격리
- Repository는 비즈니스 로직(오케스트레이션)만 담당

### 변경사항

- **NEW** `core/data/datasource/MarketAuthDataSource.kt` — 인증 DataSource 인터페이스 (5개 메서드)
- **NEW** `core/data/datasource/MarketPresetRemoteDataSource.kt` — Firestore DataSource 인터페이스 (9개 메서드)
- **NEW** `core/data/datasource/MarketStorageDataSource.kt` — Storage DataSource 인터페이스 (3개 메서드)
- **NEW** `core/data/datasource/FirebaseMarketAuthDataSource.kt` — FirebaseAuth 기반 구현체 (@Singleton)
- **NEW** `core/data/datasource/FirebaseMarketPresetRemoteDataSource.kt` — FirebaseFirestore 기반 구현체 (toggleLike 트랜잭션 포함)
- **NEW** `core/data/datasource/FirebaseMarketStorageDataSource.kt` — FirebaseStorage 기반 구현체
- **NEW** `core/data/di/DataSourceModule.kt` — @Binds 3개, 인터페이스 → 구현체 바인딩
- **MODIFY** `core/data/repository/MarketPresetRepositoryImpl.kt` — Firebase import 전부 제거, DataSource 위임 구조로 리팩토링
- **DELETE** `core/data/paging/MarketPresetPagingSourceFactory.kt` — 미사용 파일 삭제
- **MODIFY** `feature/preset-market/build.gradle.kts` — firebase-bom, firebase-firestore 의존성 제거 (실제 import 0개)

### 검증결과

- `:core:data:assembleDebug` BUILD SUCCESSFUL
- `assembleDebug` (전체) BUILD SUCCESSFUL
- `test` BUILD SUCCESSFUL (실패 0건)

### 설계결정 및 근거

- PagingSource 키 타입(`DocumentSnapshot`)을 Repository가 모르도록 `PagingSource<*, MarketPreset>` star projection 사용
- `toggleLike` Firestore 트랜잭션은 인프라 관심사이므로 DataSource로 완전 이동
- `ImageCompressor` + `Context`는 비즈니스 로직(이미지 압축)이므로 Repository에 유지
- `Pager` 생성은 Repository에 유지, `pagingSourceFactory` 람다만 DataSource에서 받음

---

## [2026-03-19] refactor(preset-market): 아키텍처 위반 3건 수정 — feature → core:data 직접 의존 제거 + :core:paging 모듈 신설

### 목표

- `feature:preset-market` ViewModel 3개에서 `core:data` 직접 import 제거
- Clean Architecture 규칙 복원: feature → core:domain / core:paging 인터페이스만 의존
- `core:domain`을 순수 JVM 모듈로 유지 (paging 의존 없음)
- Paging3 관련 인터페이스·UseCase를 `:core:paging`(Android library)으로 분리해 JVM/Android 변형 불일치 해소

### 변경사항

**이슈 1 — PresetDetailViewModel: Repository 직접 주입 → UseCase 경유**

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../usecase/IsLikedByCurrentUserUseCase.kt` | 신규 — `MarketPresetRepository.isLikedByCurrentUser()` 래핑 |
| `core/domain/.../usecase/IsDownloadedByMarketIdUseCase.kt` | 신규 — `PresetRepository.isDownloadedByMarketId()` 래핑 |
| `preset_market/PresetDetailViewModel.kt` | `MarketPresetRepository` / `PresetRepository` 직접 주입 제거 → 두 UseCase로 교체 |

**이슈 2·3 — MarketSearchViewModel / PresetMarketViewModel: core:data 직접 import 제거**

| 파일 | 변경 내용 |
|------|-----------|
| `core/data/.../di/RepositoryModule.kt` | `bindMarketPresetPagingSourceFactory` 바인딩 제거; Provider 바인딩 2개 추가 |
| `core/data/.../repository/MarketPresetRepositoryImpl.kt` | `MarketPresetPagingSourceFactory` 구현 제거; `RecentPresetsPagerProvider` / `SearchPresetsPagerProvider` 구현 추가 |
| `preset_market/MarketSearchViewModel.kt` | `FirebaseFirestore` + `SearchMarketPresetPagingSource` 직접 사용 → `SearchPresetsPagerUseCase` 주입으로 교체 |
| `preset_market/PresetMarketViewModel.kt` | `MarketPresetPagingSourceFactory` + `Pager` 수동 생성 → `GetRecentPresetsPagerUseCase` 주입으로 교체 |
| `feature/preset-market/build.gradle.kts` | `core:data` 제거, `core:paging` 추가 |

**:core:paging 모듈 신설 (Android library)**

| 파일 | 변경 내용 |
|------|-----------|
| `core/paging/build.gradle.kts` | `paging-runtime`, `core:domain`, `hilt` 설정 |
| `core/paging/.../RecentPresetsPagerProvider.kt` | 신규 — `provide(): Flow<PagingData<MarketPreset>>` 인터페이스 |
| `core/paging/.../SearchPresetsPagerProvider.kt` | 신규 — `provide(query): Flow<PagingData<MarketPreset>>` 인터페이스 |
| `core/paging/.../GetRecentPresetsPagerUseCase.kt` | 신규 — `RecentPresetsPagerProvider` 주입, `invoke()` |
| `core/paging/.../SearchPresetsPagerUseCase.kt` | 신규 — `SearchPresetsPagerProvider` 주입, `invoke(query)` |
| `core/data/build.gradle.kts` | `core:paging` 의존 추가 |
| `core/domain/build.gradle.kts` | `paging-common` 제거 (domain 순수 JVM 복원) |
| `core/domain/.../MarketPresetRepository.kt` | `getRecentPresetsPager` / `searchPresetsPager` 제거 |
| `gradle/libs.versions.toml` | `paging-common` alias 추가 (core:paging 빌드용) |

### 검증 결과

- `:core:domain:compileKotlin` BUILD SUCCESSFUL (paging 의존 없음 확인)
- `:core:paging:compileDebugKotlin` BUILD SUCCESSFUL
- `:core:data:compileDebugKotlin` BUILD SUCCESSFUL
- `:feature:preset-market:kspDebugKotlin` + `compileDebugKotlin` BUILD SUCCESSFUL (KSP 오류 해소)
- `test` (전체 단위 테스트) BUILD SUCCESSFUL (실패 0건)

### 설계 결정 및 근거

- **`:core:paging` 모듈 신설**: `paging-common`을 `core:domain`(java-library)에 추가하면 Gradle이 JVM 변형을 선택하는 반면, `feature:preset-market`(Android library)은 Android 변형을 사용해 IDE 타입 불일치 및 KSP `Symbol not found` 오류가 발생. `core:paging`을 Android library로 신설해 모든 모듈이 동일한 Android 변형의 `PagingData`를 사용하도록 통일.
- **Provider 인터페이스 도입 (`RecentPresetsPagerProvider` / `SearchPresetsPagerProvider`)**: `PagingSource`의 키 타입(`DocumentSnapshot`)은 Firebase 타입이므로 `core:paging`에 노출할 수 없음. `Flow<PagingData<T>>` 반환 인터페이스로 키 타입을 완전히 은닉하고, `Pager` 생성 로직을 `core:data` 구현체에 격리.
- **UseCase가 Provider를 래핑**: 프로젝트 컨벤션(ViewModel → UseCase 경유)을 paging에도 적용. UseCase는 Provider를 주입받아 단순 위임하며, ViewModel은 paging 인프라를 전혀 알지 못함.
- **`MarketPresetRepositoryImpl`이 Provider 인터페이스 구현**: 별도 구현 클래스 추가 없이 기존 Firestore 의존성을 재사용. `@Binds`로 두 Provider 인터페이스에 각각 바인딩.

---

## [2026-03-19] refactor(widget): WidgetViewModel — BaseViewModel MVI 패턴 마이그레이션

### 목표

- 프로젝트 내 유일한 예외였던 `WidgetViewModel`을 `BaseViewModel<S, I, E>` 패턴으로 통일
- `WidgetContract.kt`에 누락된 `WidgetSideEffect` sealed interface 추가

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/widget/.../WidgetContract.kt` | `WidgetState : UiState`, `WidgetIntent : UiIntent` 구현 추가; `WidgetSideEffect : UiSideEffect` sealed interface 신규 추가 |
| `feature/widget/.../WidgetViewModel.kt` | `ViewModel()` → `BaseViewModel<WidgetState, WidgetIntent, WidgetSideEffect>(WidgetState())` 상속 변경; `_state MutableStateFlow` / `stateIn` / `uiState` 필드 제거; `handleIntent()` → `override fun`; `_state.update { it.copy(...) }` → `updateState { copy(...) }`; `_state.value` → `currentState` |
| `feature/widget/.../ui/WidgetScreen.kt` | 변경 없음 — `state: WidgetState` 파라미터 방식 유지, `uiState` 프로퍼티명 동일 |

### 검증 결과

- `:feature:widget:compileDebugKotlin` BUILD SUCCESSFUL
- `assembleDebug` BUILD SUCCESSFUL (307 tasks)

### 설계 결정 및 근거

- **WidgetSideEffect 빈 sealed interface 허용**: 현재 위젯 화면에서 외부로 전달할 단발성 이벤트(Toast, Navigation 등)가 없으므로 빈 sealed interface로 정의. 향후 필요 시 변형 추가 가능.
- **WidgetScreen.kt 무변경**: `BaseViewModel`이 기존 `stateIn`과 동일한 `uiState: StateFlow<S>` API를 제공하므로 Screen 및 `MainActivity`의 `collectAsStateWithLifecycle()` 호출부 수정 불필요.

---

## [2026-03-19] refactor: DownloadProgress/UploadProgress typealias 제거 — PresetOperationProgress 일원화

### 목표

- 이전 리팩토링에서 임시 도입한 `DownloadProgress` / `UploadProgress` typealias를 완전히 제거
- 모든 참조를 `PresetOperationProgress`로 직접 교체해 불필요한 간접 계층 삭제

### 변경사항

| Phase | 파일 | 변경 내용 |
|-------|------|-----------|
| **1** | `core/domain/.../usecase/DownloadMarketPresetUseCase.kt` | import `DownloadProgress` → `PresetOperationProgress`, 반환 타입 및 생성자 호출 6곳 교체 |
| **1** | `core/domain/.../usecase/UploadPresetToMarketUseCase.kt` | import `UploadProgress` → `PresetOperationProgress`, 반환 타입 및 생성자 호출 4곳 교체 |
| **2** | `feature/preset-market/.../download/DownloadProgressTracker.kt` | `: ProgressTracker<DownloadProgress>` → `: ProgressTracker<PresetOperationProgress>` |
| **2** | `feature/settings/.../upload/UploadProgressTracker.kt` | `: ProgressTracker<UploadProgress>` → `: ProgressTracker<PresetOperationProgress>` |
| **2** | `feature/preset-market/.../PresetDetailContract.kt` | `downloadProgress: DownloadProgress?` → `PresetOperationProgress?` |
| **2** | `feature/settings/.../SettingsContract.kt` | `uploadProgress: UploadProgress?` → `PresetOperationProgress?` |
| **3** | `feature/settings/.../ui/PresetItemCard.kt` | 파라미터 타입 `UploadProgress?` → `PresetOperationProgress?` (계획서 미기재 누락 파일) |
| **4** | `core/domain/.../model/preset/DownloadProgress.kt` | typealias 파일 삭제 |
| **4** | `core/domain/.../model/preset/UploadProgress.kt` | typealias 파일 삭제 |

### 검증 결과

- Phase 1: `:core:domain:compileKotlin` BUILD SUCCESSFUL
- Phase 2: `assembleDebug` BUILD SUCCESSFUL
- Phase 4 (최종): `assembleDebug` BUILD SUCCESSFUL (307 tasks)

### 설계 결정 및 근거

- **Phase 3 내용 변경**: 계획서에는 ViewModel/Service 4개 파일의 import 제거로 기술됐으나, 실제 확인 결과 해당 파일들은 `DownloadProgress`/`UploadProgress`를 직접 import하지 않고 Tracker 클래스만 참조하고 있었음. 대신 계획서 미기재 파일인 `PresetItemCard.kt`에 직접 타입 참조가 있어 해당 파일을 처리함.
- **삭제 전 grep 확인**: `import.*\.(DownloadProgress|UploadProgress)$` 패턴으로 잔여 참조 0건 확인 후 typealias 파일 삭제.

---

## [2026-03-19] refactor: Progress 관련 중복 클래스 통합 (PresetOperationProgress / ProgressTracker / DataHolder)

### 목표

- Upload/Download에 각각 독립 구현된 동일 구조 클래스 3쌍을 통합해 유지보수 포인트 감소
- 기존 참조 코드(UseCase, ViewModel, Service, UI) 변경 최소화

### 변경사항

| 항목 | 파일 | 변경 내용 |
|------|------|-----------|
| **신규** | `core/domain/.../model/preset/PresetOperationProgress.kt` | `DownloadProgress` + `UploadProgress` 통합 단일 data class (`marketPresetId` 포함) |
| 교체 | `core/domain/.../model/preset/DownloadProgress.kt` | `data class` → `typealias DownloadProgress = PresetOperationProgress` |
| 교체 | `core/domain/.../model/preset/UploadProgress.kt` | `data class` → `typealias UploadProgress = PresetOperationProgress` |
| **신규** | `core/ui/.../tracker/ProgressTracker.kt` | `abstract class ProgressTracker<T>` — `update/pause/resume/awaitResume/clear` 공통 구현 |
| 교체 | `feature/settings/.../upload/UploadProgressTracker.kt` | 독립 구현 → `ProgressTracker<UploadProgress>` 상속으로 단순화 |
| 교체 | `feature/preset-market/.../download/DownloadProgressTracker.kt` | 독립 구현 → `ProgressTracker<DownloadProgress>` 상속 + `cancellationRequested` / `requestCancellation()` / `clear()` override 유지 |
| **신규** | `core/domain/.../model/DataHolder.kt` | `interface DataHolder { fun clear() }` |
| 교체 | `feature/settings/.../upload/UploadDataHolder.kt` | `DataHolder` 인터페이스 구현 추가 |
| 교체 | `feature/preset-market/.../download/DownloadDataHolder.kt` | `DataHolder` 인터페이스 구현 추가 |

### 검증 결과

- Step 1 (PresetOperationProgress + typealias): `clean assembleDebug` BUILD SUCCESSFUL
- Step 2 (ProgressTracker 추상 클래스): `assembleDebug` BUILD SUCCESSFUL
- Step 3 (DataHolder 인터페이스): `assembleDebug` BUILD SUCCESSFUL

### 설계 결정 및 근거

- **typealias 채택**: `DownloadProgress` / `UploadProgress`를 즉시 제거하면 UseCase, Contract, ViewModel, Service 전반에 걸친 타입 교체가 필요해 변경 범위가 커짐. typealias로 기존 이름을 유지해 참조 코드 무수정 보장. 이후 필요 시 단계적으로 `PresetOperationProgress`로 일원화 가능.
- **ProgressTracker 위치 (`core:ui`)**: feature:settings와 feature:preset-market 양쪽이 의존하는 공통 위치. `core:domain`은 순수 JVM 모듈(Android 의존성 금지)이라 `MutableStateFlow` 사용이 제약됨 — kotlinx-coroutines 의존성을 이미 갖춘 `core:ui`에 배치.
- **`_isPaused` private 유지**: `requestCancellation()`에서 pause 해제가 필요한 `DownloadProgressTracker`는 `resume()` 공개 메서드를 호출하는 방식으로 내부 필드 노출 없이 처리.
- **DataHolder 위치 (`core:domain`)**: `clear()` 단일 메서드의 마커 인터페이스. 도메인 레이어 객체이므로 Android 의존성 없는 `core:domain`이 적합.

---

## [2026-03-19] refactor: UI Composable 중복 제거 (PagerIndicator / PresetStatsRow / PresetSwipeItem)

### 목표

- 여러 화면에 중복 구현된 UI Composable 3종을 공통 컴포넌트로 추출해 유지보수성 향상
- 로직 변경 없이 순수 리팩토링 (각 단계 빌드 검증)

### 변경사항

| 항목 | 파일 | 변경 내용 |
|------|------|-----------|
| **신규** | `core/ui/.../component/PagerIndicator.kt` | `PagerIndicator(pageCount, currentPage, selectedColor, unselectedColor, dotSize, smallDotSize)` composable 추가 |
| 교체 | `apps-drawer/.../AppDrawerScreen.kt` | `PageIndicatorDot` private fun 제거 → `PagerIndicator` 사용 |
| 교체 | `preset-market/.../PresetDetailScreen.kt` | 이미지 pager 인디케이터(Surface 방식) 2곳 → `PagerIndicator` 사용 |
| **신규** | `preset-market/.../ui/component/PresetStatsRow.kt` | `PresetStatsRow(downloadCount, likeCount, color, iconSize, outerSpacing, innerSpacing)` composable 추가 |
| 교체 | `preset-market/.../MarketPresetCard.kt` | 다운로드/좋아요 Row 블록 → `PresetStatsRow` 사용, 미사용 Icons import 제거 |
| 교체 | `preset-market/.../MarketPresetListItem.kt` | 다운로드/좋아요 Row 블록 → `PresetStatsRow` 사용, 미사용 Icons import 제거 |
| 교체 | `preset-market/.../PresetMarketUserInfoScreen.kt` | `SimplePresetListItem` 내 텍스트 심볼 stats → `PresetStatsRow` 사용 |
| **신규** | `settings/.../ui/PresetSwipeItem.kt` | `SwipeToDismissBox + PresetItemCard` 조합을 `PresetSwipeItem` internal fun으로 추출 |
| 교체 | `settings/.../PortraitPresetSettingsScreen.kt` | items 람다 내 SwipeToDismissBox 블록 → `PresetSwipeItem` 호출로 대체, 미사용 import 제거 |
| 교체 | `settings/.../LandscapePresetSettingsScreen.kt` | items 람다 내 SwipeToDismissBox 블록 → `PresetSwipeItem` 호출로 대체, 미사용 import 제거 |

### 검증 결과

- Step 1 (PagerIndicator): `assembleDebug` BUILD SUCCESSFUL
- Step 2 (PresetStatsRow): `assembleDebug` BUILD SUCCESSFUL
- Step 3 (PresetSwipeItem): `assembleDebug` BUILD SUCCESSFUL

### 설계 결정 및 근거

- **PagerIndicator 위치 (`core:ui`)**: `AppDrawerScreen`(feature:apps-drawer)과 `PresetDetailScreen`(feature:preset-market) 양쪽에서 공유해야 하므로 cross-feature 의존 금지 원칙에 따라 `core:ui`에 배치. `dotSize`/`smallDotSize` 분리 파라미터로 AppDrawerScreen의 선택/비선택 크기 차이(8dp/6dp) 유지.
- **PresetStatsRow 위치 (`feature:preset-market` 내부)**: 3개 사용처 모두 동일 모듈 내이므로 `internal` visibility로 외부 노출 없이 공유. `color`, `iconSize`, `outerSpacing`, `innerSpacing` 파라미터화로 Card(흰색/14dp)와 ListItem(onSurfaceVariant/12dp) 스타일 차이 수용.
- **PresetSwipeItem 위치 (`feature:settings/ui`)**: Portrait/Landscape 두 파일 모두 동일 패키지이므로 별도 파일로 추출. `Modifier.animateItem()`은 LazyColumn 컨텍스트에서만 의미 있으므로 호출부에서 `modifier` 파라미터로 전달.

---

## [2026-03-19] chore: 사소한 코드 정리 (Minor Cleanup)

### 목표

- 불필요한 Kotlin stdlib 명시적 import 제거
- Composable visibility 정책 일관성 확보
- 파일명/함수명 케이싱 불일치 수정 (`LandScape` → `Landscape`)
- Android Studio 자동 생성 보일러플레이트 테스트 파일 제거

### 변경사항

| 항목 | 파일 | 변경 내용 |
|------|------|-----------|
| stdlib import 제거 | `settings/ui/LandscapeSettingsLayout.kt` | `kotlin.collections.chunked`, `kotlin.collections.forEach` import 삭제 |
| stdlib import 제거 | `settings/ui/SavePresetDialog.kt` | `kotlin.text.ifEmpty` import 삭제 |
| stdlib import 제거 | `settings/ui/UploadToMarketDialog.kt` | `kotlin.collections.distinct`, `kotlin.collections.plus` import 삭제 |
| Visibility 수정 | `settings/ui/ColorSettingsContent.kt` | `ColorSettingsGridItem`: `internal` → `private` |
| Visibility 수정 | `settings/ui/PresetSettingsContent.kt` | `PresetSettingsContent`: `public` → `internal` |
| 파일명 rename | `LandScapePresetSettingsScreen.kt` → `LandscapePresetSettingsScreen.kt` | `git mv`로 케이싱 수정 |
| 함수명 수정 | `LandscapePresetSettingsScreen.kt` | `LandScapePresetSettingsScreen` → `LandscapePresetSettingsScreen` |
| 호출부 수정 | `settings/ui/PresetSettingsContent.kt` | 함수 호출명 동일하게 수정 |
| 보일러플레이트 삭제 | `feature/launcher/.../ExampleInstrumentedTest.kt` | 파일 삭제 |
| 보일러플레이트 삭제 | `feature/apps-drawer/.../ExampleInstrumentedTest.kt` | 파일 삭제 |
| 보일러플레이트 삭제 | `feature/apps-drawer/.../ExampleUnitTest.kt` | 파일 삭제 |
| 보일러플레이트 삭제 | `feature/widget/.../ExampleUnitTest.kt` | 파일 삭제 |
| 보일러플레이트 삭제 | `feature/widget/.../ExampleInstrumentedTest.kt` | 파일 삭제 |
| 보일러플레이트 삭제 | `feature/preset-market/.../ExampleUnitTest.kt` | 파일 삭제 |
| 보일러플레이트 삭제 | `feature/preset-market/.../ExampleInstrumentedTest.kt` | 파일 삭제 |

### 검증결과

- `./gradlew assembleDebug` — BUILD SUCCESSFUL (35s, 35 executed / 272 up-to-date)

### 설계결정 및 근거

- **stdlib import**: Kotlin 표준 라이브러리는 자동 import되므로 명시적 import는 불필요한 노이즈. IDE 경고 기준과 일치
- **`private` 강등**: `ColorSettingsGridItem`은 동일 파일 내부에서만 사용되므로 `internal`로 열어둘 필요 없음. 최소 가시성 원칙 적용
- **`internal` 강등**: `PresetSettingsContent`는 다른 feature 모듈에서 참조하지 않으므로 `public` 불필요. feature 모듈 캡슐화 강화
- **`git mv` 사용**: Windows 파일시스템은 대소문자를 구분하지 않으므로 `cp`로는 rename 불가. `git mv`를 통해 Git이 rename으로 추적하도록 처리
- **보일러플레이트 삭제**: Android Studio가 모듈 생성 시 자동으로 만든 빈 테스트 파일로, 실제 로직 없음. 존재하면 CI/테스트 리포트에서 혼란 유발 가능

---

## [2026-03-19] fix(preset): 프리셋 용량 제한 및 예외 처리 개선

### 목표

- `savePreset()`에서 `id != 0`이지만 DB에 존재하지 않는 프리셋 저장 시 10개 용량 제한이 우회되는 버그 수정
- UI 정책(10개 초과 시 저장 차단)과 Repository 정책 일치: 초과 시 가장 오래된 항목 자동 삭제 → `IllegalStateException` throw로 변경
- Repository 예외가 `SettingsViewModel`에서 처리되지 않아 앱이 크래시되는 문제 수정

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/data/.../room/preset/PresetDao.kt` | `getById(id: Int): PresetEntity?` 쿼리 메서드 추가; 더 이상 사용되지 않는 `deleteOldestPreset()` 제거 |
| `core/data/.../repository/PresetRepositoryImpl.kt` | `isInsert = id == 0 || getById(id) == null` 로 실질적 INSERT 판별; 초과 시 `IllegalStateException` throw (기존 자동 삭제 로직 제거) |
| `feature/settings/.../SettingsContract.kt` | `SettingsSideEffect.ShowError(val message: String)` 추가 |
| `feature/settings/.../SettingsViewModel.kt` | `savePresetUseCase` 호출을 `runCatching { }.onFailure { }` 로 감싸 예외 발생 시 `ShowError` SideEffect 발행 |
| `app/.../MainActivity.kt` | `ShowError` SideEffect 수신 시 `settingsSnackbarHostState.showSnackbar()` 로 에러 메시지 표시 |

### 검증결과

- `:app:compileDebugKotlin`, `:feature:settings:compileDebugKotlin` — BUILD SUCCESSFUL

### 설계결정 및 근거

- **자동 삭제 → 예외 throw**: UI(`PresetSettingsContent`)가 이미 10개 초과 시 저장 자체를 막고 안내 다이얼로그를 띄우므로, Repository에서 몰래 삭제하는 동작은 UI 정책과 불일치. 저장 거부로 통일
- **`isInsert` 조건**: `@Insert(onConflict = REPLACE)`는 id가 없는 행도 INSERT로 처리하므로 `id != 0`이더라도 DB에 존재하지 않으면 실질적 INSERT로 간주해 용량 제한 적용
- **`runCatching` 선택**: `try-catch` 대신 `runCatching`으로 성공/실패 분기를 선언적으로 표현. `onFailure`에서 SideEffect 발행
- **`ShowError` SideEffect**: 기존 `UploadError`는 업로드 전용이므로 범용 에러 표시용 SideEffect를 별도 추가. 기존 Snackbar 인프라(`settingsSnackbarHostState`) 재사용

---

## [2026-03-19] refactor(feed): handleToChannelIdCache 제거 및 resolveChannelId 추출

### 목표

- `FeedRepositoryImpl`의 `handleToChannelIdCache: HashMap<String, String>()` 이 앱 수명 동안 무한히 증가할 수 있다는 이슈를 검토
- 실제 앱 구조(YouTube 채널 설정 1개, RSS 제거)에서 인메모리 캐시 자체가 불필요함을 확인
- `@Singleton` 클래스의 `var` 가변 상태 제거 (thread-safety, 테스트 격리, 참조 투명성 문제)

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/data/.../repository/FeedRepositoryImpl.kt` | `HashMap<String, String>` → `Pair<String, String>?` 중간 단계를 거쳐 최종적으로 인메모리 캐시 전체 제거. `resolveChannelId(channelId): String?` private suspend 함수 추출. 세 곳의 중복 핸들 해석 로직을 단일 호출로 통일 |

### 검증결과

- 코드 리뷰 수준 확인 (빌드/테스트 미실행)
- `@Singleton` 클래스 내 `var` 프로퍼티 0개 — 가변 상태 없음

### 설계결정 및 근거

- **캐시 제거 근거**: ViewModel의 60초 쿨다운이 이미 호출 빈도를 제한하고 있어 `@handle → channelId` 해석 API(`part=id`) 중복 호출이 실질적으로 발생하지 않음. `getChannelProfile`의 DataStore 7일 TTL 캐시가 더 비싼 `snippet,statistics` 호출을 보호
- **`var` 제거**: `@Singleton`의 가변 상태는 경쟁 조건, 테스트 간 상태 누수, 호출 순서 의존성을 유발. `resolveChannelId` 추출로 클래스가 완전히 무상태(stateless)가 됨
- **`Pair` 중간 단계**: HashMap보다 단순하지만 `var` 문제는 여전히 존재해 최종적으로 제거. 과정을 통해 "캐시 자체가 필요한가"를 재검토하는 계기가 됨

---

## [2026-03-19] refactor(preset-market): MarketPreset / MarketPresetDto V1 레거시 제거 및 필드 정리

### 목표

- `.slp` 포맷(schemaVersion=2)이 도입된 이후 더 이상 사용되지 않는 V1 전용 필드(개별 이미지 URL 9개 + 설정값 8개 = 17개) 삭제
- V1 다운로드 경로(`invokeV1`, `downloadLegacyImages`) 제거
- `SlpPacker` 입력 타입을 `MarketPreset` → `Preset`(로컬)으로 변경해 업로드 흐름을 명확히 분리

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../model/preset/MarketPreset.kt` | 17개 필드 삭제 (`topLeft*Url`, `topRight*Url`, `bottomLeft*Url`, `bottomRight*Url`, `wallpaperUrl`, `useFeed`, `youtubeChannelId`, `chzzkChannelId`, `appDrawerColumns`, `appDrawerRows`, `appDrawerIconSizeRatio`, `enableParallax`, `themeColorHex`). `schemaVersion` 기본값 1→2 |
| `core/data/.../firestore/MarketPresetDto.kt` | 동일 17개 필드 삭제. `toDomain()` / `toDto()` 매핑 간소화. `schemaVersion` 기본값 1→2 |
| `core/domain/.../repository/PresetUnpackager.kt` | `downloadLegacyImages()` 인터페이스 메서드 삭제 |
| `core/data/.../slp/PresetUnpackagerImpl.kt` | `downloadLegacyImages()` 구현 삭제. 불필요한 `Preset` import 제거 |
| `core/domain/.../usecase/DownloadMarketPresetUseCase.kt` | `invokeV1()` / `downloadWithProgressV1()` 삭제. `invoke()` / `downloadWithProgress()` 단일 V2 경로로 통합. `applySettings(marketPreset, localPreset)` → `applySettings(localPreset)` (boolean flags를 localPreset에서 읽음) |
| `core/data/.../slp/SlpMapper.kt` | `toMarketPreset()`: 설정값 8개 필드 매핑 제거 (boolean flags 유지) |
| `core/domain/.../model/preset/UploadProgress.kt` | `marketPresetId: String? = null` 필드 추가 — 업로드 완료 시 생성된 ID를 ViewModel에 전달 |
| `core/data/.../slp/SlpPacker.kt` | 입력 타입 `MarketPreset` → `Preset`. `buildManifest()` / `pack()` 에 `description`, `tags`, `authorUid`, `authorDisplayName` 파라미터 추가. 이미지 키 참조 `topLeftIdleUrl` → `topLeftIdleUri` 등으로 변경 |
| `core/domain/.../repository/PresetPackager.kt` | `packPreset()` 시그니처 변경: `MarketPreset` → `Preset` + metadata 파라미터 |
| `core/data/.../slp/PresetPackagerImpl.kt` | 변경된 `packPreset()` 시그니처에 맞춰 구현 업데이트 |
| `core/domain/.../usecase/UploadPresetToMarketUseCase.kt` | 입력 타입 `MarketPreset` → `Preset`. `description`, `tags` 파라미터 추가. 사용자 정보(`uid`, `displayName`)를 `getCurrentUser()`로 직접 조회. 완료 이벤트에 `marketPresetId` 포함 |
| `feature/settings/.../upload/UploadDataHolder.kt` | `pendingPreset` 타입 `MarketPreset` → `Preset`. `pendingDescription`, `pendingTags` 필드 추가 |
| `feature/settings/.../SettingsViewModel.kt` | `uploadPreset()`: `MarketPreset` 생성 제거 → 로컬 `Preset` 직접 저장. `pendingUploadMarketPresetId` 제거 → `UploadProgress.marketPresetId`에서 읽음. 불필요한 `UUID`, `MarketPreset` import 제거 |
| `app/.../service/PresetUploadService.kt` | `UploadDataHolder`에서 `description`, `tags` 추가 로드 → `uploadWithProgress()` 전달 |

### 검증결과

- `assembleDebug` BUILD SUCCESSFUL
- `./gradlew test` BUILD SUCCESSFUL (실패 0건)
- `MarketPreset` / `MarketPresetDto`에서 17개 V1 필드 grep 결과 0건 확인

### 설계결정 및 근거

- **SlpPacker 입력 타입 변경**: `MarketPreset`이 이미지 URL을 들고 다니는 패턴은 "마켓 메타데이터 모델이 로컬 파일 경로를 오염"시키는 구조였음. `Preset`(로컬)이 이미지/설정값을 갖고, `MarketPreset`은 Firestore 문서 표현에만 집중하도록 책임 분리
- **UploadProgress.marketPresetId**: 업로드 사용 케이스 내부에서 UUID를 생성하므로 ViewModel이 외부에서 ID를 예측할 수 없음. 완료 이벤트에 ID를 실어서 ViewModel이 로컬 preset과 시장 preset을 연결(`updateMarketPresetIdUseCase`)할 수 있도록 함
- **boolean flags 유지**: `hasTopLeftImage` 등의 flags는 다운로드 전 UI 칩 표시와 `applySettings` 분기에 계속 사용되므로 제거 대상에서 제외

---

## [2026-03-18] feat(preset-market): 유저 정보 화면 추가

### 목표

- MarketHomeScreen 상단바 AccountCircle 클릭 시 로그아웃 다이얼로그 대신 유저 정보 화면으로 네비게이션
- 유저 프로필(이름, 이메일, 아바타) + 업로드한 프리셋 목록을 보여주는 새 화면 구현
- portrait/landscape 레이아웃 자동 전환

### 변경사항

- `core/domain/.../repository/MarketPresetRepository.kt`: `getPresetsByAuthor(uid)` 메서드 추가
- `core/domain/.../usecase/GetUserPresetsUseCase.kt` (신규): `getPresetsByAuthor` 호출
- `core/data/.../repository/MarketPresetRepositoryImpl.kt`: `getPresetsByAuthor` 구현 (Firestore whereEqualTo + orderBy createdAt DESC)
- `feature/preset-market/.../navigation/MarketRoute.kt`: `MarketUserInfo` object 추가
- `feature/preset-market/.../PresetMarketUserInfoContract.kt` (신규): State/Intent/SideEffect
- `feature/preset-market/.../PresetMarketUserInfoViewModel.kt` (신규): GetCurrentMarketUserUseCase, GetUserPresetsUseCase, SignOutUseCase 주입
- `feature/preset-market/.../ui/PresetMarketUserInfoScreen.kt` (신규): portrait(LazyColumn)/landscape(Row) 분기 레이아웃
- `feature/preset-market/.../ui/PresetMarketHost.kt`: MarketUserInfo composable 추가 (slide 트랜지션)
- `feature/preset-market/.../PresetMarketContract.kt`: NavigateToUserInfo Intent/SideEffect 추가
- `feature/preset-market/.../PresetMarketViewModel.kt`: NavigateToUserInfo 핸들러 추가
- `feature/preset-market/.../ui/MarketHomeScreen.kt`: showSignOutDialog 제거, AccountCircle 클릭 → NavigateToUserInfo intent 발행

### 검증결과

- `assembleDebug` BUILD SUCCESSFUL

### 설계결정 및 근거

- 로그아웃은 유저 정보 화면에서만 가능하도록 변경 → 실수 로그아웃 방지 + 유저 정보 확인 기회 제공
- `MarketPresetListItem`은 SharedTransition 의존이 있어 재사용 불가 → `SimplePresetListItem` 내부 private composable로 간소화
- Landscape 감지: `LocalConfiguration.current.screenWidthDp > screenHeightDp` 비교

---

## [2026-03-18] fix(upload-dialog): UploadToMarketDialog 태그·이미지 입력 UX 개선

### 목표

- 태그 입력 시 쉼표를 입력하는 즉시 태그가 등록되도록 즉시 처리 수정
- 태그 글자 수 제한 (10글자 미만) 추가
- 입력된 태그·이미지에 빨간 원형 X 아이콘을 표시해 삭제 가능함을 명시
- 이미지 목록이 많아질 경우를 대비해 가로 스크롤 적용
- 이미지피커를 닫아도 기존 선택 이미지가 초기화되지 않도록 수정

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/settings/.../ui/UploadToMarketDialog.kt` | `endsWith(","\|"\n")` 조건 추가 — 쉼표 입력 즉시 태그 확정 + `tagInput = ""` 초기화 |
| `feature/settings/.../ui/UploadToMarketDialog.kt` | 태그 입력 `take(9)` + `filter { it.length < 10 }` — 10글자 미만 제한 |
| `feature/settings/.../ui/UploadToMarketDialog.kt` | 태그 Row → `LazyRow(items)` 교체, 각 태그를 `Box`로 감싸 우상단 빨간 원형 X 배지 추가 |
| `feature/settings/.../ui/UploadToMarketDialog.kt` | 이미지 Row → `LazyRow(items)` 교체, 각 이미지를 `Box`로 감싸 우상단 빨간 원형 X 배지 추가 |
| `feature/settings/.../ui/UploadToMarketDialog.kt` | `imagePicker` 콜백에 `uris.isNotEmpty()` 조건 추가 — 피커 취소 시 기존 선택 유지 |

### 검증 결과

- `./gradlew :feature:settings:assembleDebug` → **BUILD SUCCESSFUL** (전 항목)

### 설계 결정 및 근거

- **쉼표 즉시 처리**: 기존 `size > 1` 로직은 `"hello,"` → split → filter isNotBlank → `["hello"]`(size=1)로 else 분기에 빠지는 버그. `endsWith` 선행 체크로 분기를 명확히 분리
- **`size > 1` 로직 유지**: 중간에 쉼표가 있는 경우(`"a,b"`) 처리용으로 여전히 필요하므로 제거하지 않음
- **X 배지 패턴**: `Box` + `Alignment.TopEnd` 오버레이로 기존 `SuggestionChip`/`AsyncImage` 레이아웃을 변경 없이 뱃지 추가. `padding(top, end)` 으로 배지가 잘리지 않도록 공간 확보
- **피커 취소 유지**: `PickMultipleVisualMedia` 계약상 취소 시 빈 리스트를 반환하므로 `isNotEmpty` 조건만으로 충분히 구분 가능

---

## [2026-03-18] feat(preset-market): 부분 검색(Substring Search) 구현

### 목표

- 프리셋 마켓에서 이름의 일부만 입력해도 검색 결과가 나오도록 개선
- 기존 `buildSearchKeywords()`는 완전한 단어만 저장했고, `whereArrayContains`가 정확한 일치만 매칭하여 "다크 테마"를 "다"로 검색하면 결과가 나오지 않는 문제 해결

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/data/.../firestore/MarketPresetDto.kt` | `buildSearchKeywords()` 교체 — 공백 제거 후 모든 부분 문자열(substring) 생성; `generateSubstrings()` 헬퍼 추가 |
| `core/data/.../paging/SearchMarketPresetPagingSource.kt` | 검색어 공백 제거 후 단일 `whereArrayContains` 쿼리로 단순화; 클라이언트 필터링 코드 제거 |
| `core/data/.../repository/MarketPresetRepositoryImpl.kt` | `searchPresets()`를 PagingSource와 동일 로직으로 동기화; 클라이언트 필터링 코드 제거 |

### 설계 결정 및 근거

- **Substring 토큰 방식 채택**: Firestore는 `array-contains-any`로 OR 검색은 가능하지만 LIKE 연산이 없음. 업로드 시점에 모든 부분 문자열을 `searchKeywords` 배열에 사전 생성해두면 `whereArrayContains` 단일 쿼리로 전방/중방/후방 매칭을 모두 처리 가능
- **공백 제거 기준 매칭**: "다크 테마"를 "다크테마"로 정규화하면 검색어 "크테"가 단어 경계를 넘어 매칭됨. 검색어도 동일하게 공백 제거하므로 "크 테"로 검색해도 "크테"로 매칭
- **클라이언트 필터 제거**: 기존 복수 키워드 클라이언트 필터는 substring 토큰이 모든 경우를 포함하므로 불필요. 서버 단일 쿼리로 완결되어 네트워크 트래픽과 클라이언트 CPU 모두 절약
- **쿼리 비용 동일**: `whereArrayContains`는 배열 크기와 무관하게 문서당 1회 읽기 — 토큰 수 증가가 Firestore 비용에 영향 없음
- **기존 데이터 마이그레이션 필요**: 기존 업로드 문서는 완전한 단어만 저장되어 있으므로 Firebase Console 또는 Admin SDK 스크립트로 `searchKeywords` 필드 재생성 필요 (신규 업로드부터는 즉시 동작)

---

## [2026-03-18] refactor(arch): Clean Architecture 위반 3건 수정 + README 작성

### 목표

- 프로젝트 전체에 걸쳐 Clean Architecture 원칙(UseCase 계층 우회 금지, data layer 타입 노출 금지)을 위반하는 코드를 탐지하고 수정한다
- 채용 담당관용 README.md를 작성한다
- RSS 미사용 확정에 따라 관련 잔재 코드를 제거한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `README.md` | **신규 생성** — 기능 설명(스크린샷 플레이스홀더 포함), 아키텍처, 모듈 구조, 기술 스택, Q&A 10개 |
| `core/domain/.../usecase/UpdateMarketPresetIdUseCase.kt` | **신규 생성** — `PresetRepository.updateMarketPresetId` 래핑 UseCase |
| `core/data/.../paging/MarketPresetPagingSourceFactory.kt` | **신규 생성** — `PagingSource` 생성 책임을 캡슐화하는 인터페이스 (`core:domain` 순수 JVM 원칙 유지) |
| `core/data/.../repository/MarketPresetRepositoryImpl.kt` | `MarketPresetPagingSourceFactory` 추가 구현, `create()` 메서드 추가 |
| `core/data/.../di/RepositoryModule.kt` | `MarketPresetPagingSourceFactory → MarketPresetRepositoryImpl` Hilt `@Binds` 추가 |
| `feature/preset-market/.../PresetMarketViewModel.kt` | `FirebaseFirestore` + `MarketPresetPagingSource` 직접 참조 제거 → `MarketPresetPagingSourceFactory` 주입 |
| `feature/settings/.../SettingsViewModel.kt` | `PresetRepository` 직접 주입 제거 → `UpdateMarketPresetIdUseCase` 주입 |
| `feature/settings/.../SettingsViewModelTest.kt` | mock 대상 `presetRepository` → `updateMarketPresetIdUseCase` 교체 |
| `feature/settings/.../model/SettingMenuItem.kt` | `Icons.Rounded.RssFeed` → `Icons.Rounded.LiveTv` (RSS 미사용 확정에 따른 잔재 아이콘 교체) |

### 검증 결과

- `JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew assembleDebug` → **BUILD SUCCESSFUL**
- `JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew test` → **BUILD SUCCESSFUL** (실패 0건)

### 설계 결정 및 근거

- **`UpdateMarketPresetIdUseCase` 도입**: `SettingsViewModel`이 `PresetRepository`를 직접 주입받아 `updateMarketPresetId()`를 호출하는 것은 UseCase 계층을 우회하는 패턴. 단일 책임 UseCase를 신규 생성해 `PresetRepository`를 ViewModel 생성자에서 제거
- **`MarketPresetPagingSourceFactory` 도입**: `PresetMarketViewModel`이 `FirebaseFirestore`를 직접 주입받아 `MarketPresetPagingSource`를 생성하던 구조를 개선. `PagingSource`의 키 타입이 `DocumentSnapshot`(Firebase 타입)이어서 domain 인터페이스에 노출할 수 없으므로, `core:data` 내부에 팩토리 인터페이스를 두고 `MarketPresetRepositoryImpl`이 구현. ViewModel은 팩토리 인터페이스만 알고 Firebase 타입에 의존하지 않음. `core:domain`의 순수 JVM 원칙도 유지됨
- **RSS 아이콘 교체**: RSS 기능은 사용하지 않기로 확정. 코드베이스 전체를 검색한 결과 실제 RSS 파싱/URL 저장 코드는 이미 없었고, `SettingMenuItem.kt`의 `RssFeed` 아이콘만 잔재로 남아 있었음. 치지직/YouTube 스트리밍 피드 성격에 맞는 `LiveTv` 아이콘으로 교체
- **README 아키텍처 다이어그램 수정**: 초안의 다이어그램이 `Repository Interface → Data Sources` 방향으로 잘못 표기됐음. `RepositoryImpl`이 인터페이스를 구현(↑)하고 Data Sources에 접근(↓)하는 올바른 의존성 역전(DIP) 구조로 재작성

---

## [2026-03-18] feat(preset-market): 썸네일 Shared Element Transition 구현

### 목표

- 마켓 홈 → 상세 화면 이동 시 썸네일 이미지가 자연스럽게 위치·크기가 변하는 Shared Element Transition 애니메이션 추가
- Top 10 카드(HorizontalPager)와 최근 업로드 리스트(LazyColumn) 각각에서 상세 화면 pager 영역으로 부드럽게 모핑
- 동일 프리셋이 두 섹션에 동시 노출될 때 키 충돌로 인한 애니메이션 오작동 방지
- AdMob 광고 늦은 로딩으로 인한 레이아웃 이동(layout shift) 및 애니메이션 위치 오류 방지

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/preset-market/.../navigation/MarketRoute.kt` | `MarketDetail`에 `fromCard: Boolean = false` 파라미터 추가 — 진입 섹션(카드/리스트) 구분용 |
| `feature/preset-market/.../ui/PresetMarketHost.kt` | `MarketDetail` 트랜지션 `None→fadeIn/fadeOut(tween(300))` 변경; `MarketHome` 동일 적용; `fromCard` 라우트 파라미터 읽어 `PresetDetailScreen`에 전달; `onNavigateToDetailFromCard` 콜백 추가 |
| `feature/preset-market/.../ui/MarketPresetCard.kt` | `sharedTransitionScope`/`animatedVisibilityScope` 파라미터 추가; 카드 전체 Box에 `sharedBounds(key="preset-card-thumb-{id}")` 적용 (clip 이전, `with(sharedTransitionScope)` 래핑) |
| `feature/preset-market/.../ui/MarketPresetListItem.kt` | 동일 파라미터 추가; 썸네일 AsyncImage에 `sharedBounds(key="preset-list-thumb-{id}")` 적용 (size 이후, clip 이전) |
| `feature/preset-market/.../ui/PresetDetailScreen.kt` | `sharedTransitionScope`/`animatedVisibilityScope`/`fromCard` 파라미터 추가; 별도 히어로 이미지 없이 기존 `HorizontalPager`에 `sharedBounds` 직접 적용 (key는 `fromCard` 기반으로 선택) |
| `feature/preset-market/.../ui/MarketHomeScreen.kt` | `onNavigateToDetailFromCard` 파라미터 추가; Top 10 카드 클릭 시 ViewModel 우회 → `onNavigateToDetailFromCard` 직접 호출; AdMob 배너를 `Box(height=58.dp)`로 감싸 광고 로딩 전 공간 예약 |
| `feature/preset-market/.../ui/MarketSearchScreen.kt` | 검색 결과 `MarketPresetListItem`에 `sharedTransitionScope`/`animatedVisibilityScope` 전달 |

### 검증 결과

- `JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew :feature:preset-market:assembleDebug` → **BUILD SUCCESSFUL**
- 마켓 홈 Top 10 카드 탭 → 해당 카드 이미지가 상세 pager로 모핑 ✓
- 마켓 홈 최근 업로드 리스트 탭 → 80dp 썸네일이 상세 pager로 확대 ✓
- 검색 결과 리스트 탭 → 동일 트랜지션 ✓
- 뒤로가기 → 역방향 트랜지션으로 원위치 복귀 ✓
- 동일 프리셋이 Top 10 + 최근 업로드 동시 노출 시 각 섹션 키 독립 → 충돌 없음 ✓
- AdMob 공간 예약으로 광고 로딩 후 레이아웃 이동 없음 ✓

### 설계 결정 및 근거

- **`sharedBounds` vs `sharedElement`**: 카드(Box+텍스트오버레이)→pager, 리스트썸네일→pager는 source/destination 컨텐츠가 다르므로 `sharedBounds` 사용. `sharedElement`는 동일 컨텐츠 leaf 요소에 적합하나 여기서는 컨테이너 크기·내용물 모두 달라 `sharedBounds`가 올바른 API
- **modifier 순서 (size → sharedBounds → clip)**: `clip` 이전에 `sharedBounds` 적용해야 overlay 드로잉 시 clip이 transition에 간섭하지 않음. Android 공식 docs 권장 순서 준수
- **카드 Box 전체에 sharedBounds 적용**: 내부 AsyncImage에만 적용하면 부모 Box의 `clip(RoundedCornerShape)` 제약을 받아 위치 계산 오류 발생. Box 자체를 shared element로 만들어 clip을 sharedBounds 이후에 위치시킴
- **섹션별 독립 키 분리 (`preset-card-thumb-` / `preset-list-thumb-`)**: 동일 프리셋이 Top 10과 최근 업로드에 동시에 노출될 때 같은 키 사용 시 SharedTransitionLayout이 소스 요소를 잘못 선택하는 버그 발생. `fromCard` boolean을 `MarketDetail` 라우트에 추가해 상세 화면이 올바른 키를 사용하도록 함
- **Top 10 카드 클릭 ViewModel 우회**: `PresetMarketIntent.ClickPreset`에 `fromCard` 정보를 추가하는 대신, Top 10 pager의 `onClick`을 `onNavigateToDetailFromCard` 콜백으로 직접 연결. ViewModel 내부 로직(네비게이션 SideEffect만 emit)이 단순해 우회 비용이 낮고 Contract 변경을 최소화함
- **AdMob 공간 예약 (`Box(height=58.dp)`)**: 표준 BANNER 규격(50dp) + 수직 패딩(4dp×2=8dp) = 58dp를 고정 높이로 미리 확보. 광고 로딩 완료 전 레이아웃 이동으로 인해 shared element 소스 좌표가 어긋나는 문제를 근본 해결
- **`MarketHome` 트랜지션도 None→fade 변경**: 홈 화면이 instant하게 나타나면 back 시 pager→썸네일 역방향 애니메이션 시간이 없어짐. 홈/상세 양쪽 모두 `fadeIn/fadeOut(tween(300))`으로 300ms 트랜지션 윈도우를 확보해 shared element가 부드럽게 움직일 수 있도록 함

---

## [2026-03-18] fix(proguard): 릴리즈 빌드 R8 난독화 위험 지점 전체 수정

### 목표

- 릴리즈 빌드에서 Firestore 프리셋 목록 미표시 및 업로드 실패 원인을 진단한다
- 프로젝트 전체에 걸쳐 R8/ProGuard 난독화로 인해 런타임 오류가 발생할 수 있는 모든 위험 지점을 검토하고 수정한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `app/proguard-rules.pro` | **`MarketPresetDto` 패키지 keep 추가** — Firestore `toObject()` 리플렉션 역직렬화 시 getter 이름 난독화로 인한 "Found two getters or fields with conflicting case sensitivity" 오류 방지 |
| `app/proguard-rules.pro` | **`SettingsMenu` enum keep 추가** — `SettingsMenu.COLOR.name`으로 네비게이션 인자를 생성하고 `SettingsMenu.valueOf(route.menu)`로 파싱하는 구조에서 enum 상수 이름 난독화(`COLOR` → `a`) 시 `IllegalArgumentException` 크래시 방지 |

### 검증 결과

- 릴리즈 빌드 후 `app/build/outputs/mapping/release/mapping.txt`에서 해당 클래스/enum 이름 보존 여부 확인 필요
- Firestore 프리셋 목록 조회 및 업로드 기능 릴리즈 빌드에서 재테스트 필요

### 설계 결정 및 근거

- **Firestore DTO keep**: Firebase Firestore SDK는 `toObject(Class)` 호출 시 자체 BeanMapper가 Java 리플렉션으로 getter/setter를 스캔해 Firestore 필드명과 매핑한다. R8이 `getHasTopLeftImage()` 같은 getter 이름을 `a()`로 난독화하면 BeanMapper가 동일 프로퍼티에 여러 getter가 매핑된다고 오해해 오류를 발생시킨다. Firebase SDK 자체의 consumer ProGuard rules는 SDK 내부 클래스만 보호하므로 우리 DTO 패키지(`data.remote.firestore.*`)를 명시적으로 keep해야 한다
- **`SettingsMenu` enum keep**: `proguard-android-optimize.txt`의 기본 enum 규칙(`-keepclassmembers enum * { $VALUES; valueOf(); }`)은 `valueOf()` 메서드와 `$VALUES` 배열은 보존하지만 enum 상수의 **이름 문자열** 자체는 보호하지 않는다. R8은 enum 상수를 `COLOR` → `a`로 rename할 수 있으며, 이 경우 `valueOf("COLOR")`가 `IllegalArgumentException`으로 크래시한다. 네비게이션 라우트 파라미터로 `SettingsMenu.COLOR.name`(문자열 `"COLOR"`)을 전달하고 수신측에서 `valueOf()`로 복원하는 패턴이기 때문에 enum 상수 이름 전체를 반드시 보존해야 한다
- **전체 검토 기준**: 리플렉션(Firestore/Gson/Jackson), enum valueOf/name, Navigation type-safe route, Room/Hilt/WorkManager, DataStore kotlinx-serialization, Retrofit 서비스 인터페이스 6개 카테고리 점검. Navigation type-safe route(`@Serializable` 어노테이션)·Retrofit(`@retrofit2.http.*` 규칙)·Room/Hilt/WorkManager(자체 consumer rules)·SLP/네트워크 응답 모델(`@Serializable` 규칙)은 이미 보호됨을 확인

---

## [2026-03-17] fix(memory): 화면 회전 시 MainActivity 메모리 누수 수정

### 목표

- LeakCanary가 보고한 `static AndroidComposeView.composeViews → AndroidComposeView → 파괴된 MainActivity` 누수를 제거한다
- 화면 회전 시 Activity 재생성 없이 Compose 재구성만으로 대응하도록 전환한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `app/src/main/AndroidManifest.xml` | `<activity>` 에 `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|uiMode"` 추가 — 화면 회전 시 Activity `onDestroy`/`onCreate` 사이클 억제 |
| `app/.../MainActivity.kt` | `onConfigurationChanged()` 오버라이드 추가 — uiMode 변경 시 `updateSystemBarStyle` 재호출 |
| `app/.../MainActivity.kt` | `AppWidgetHost(this, HOST_ID)` → `AppWidgetHost(applicationContext, HOST_ID)` — 위젯 호스트가 Activity context를 강한 참조하지 않도록 개선 |

### 검증 결과

- `JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**
- LeakCanary 누수 경로 제거 확인 (화면 회전 시 Activity 재생성 없음)

### 설계 결정 및 근거

- **`configChanges` 선언이 근본 수정인 이유**: 누수의 원인은 Compose 프레임워크 내부의 `static AndroidComposeView.composeViews` 리스트가 파괴된 Activity의 뷰를 즉시 제거하지 않는 버그다. 앱 코드에서 이 static 리스트를 직접 제어하는 방법은 없으므로, Activity 재생성 자체를 막는 것이 유일한 앱 레벨 해법이다
- **런처 앱에서 `configChanges` 적합성**: 이미 `LocalConfiguration.current`로 가로/세로 레이아웃을 분기하고 있어 Activity 재생성 없이도 UI가 올바르게 재구성된다. 다른 대형 런처(Pixel Launcher 등)도 동일한 패턴을 사용한다
- **`uiMode` 포함 이유**: `updateSystemBarStyle`이 `resources.configuration.uiMode`를 읽어 다크/라이트 모드를 판별하므로, uiMode 변경 시 `onConfigurationChanged`에서 재호출이 필요하다. `configChanges`에 `uiMode`를 포함하지 않으면 시스템이 Activity를 재생성하는 대신 `onConfigurationChanged`를 호출하지 않아 시스템바 색상이 갱신되지 않는다
- **`applicationContext`로 전환**: `AppWidgetHost`는 Activity 수명주기보다 길게 살 수 있는 위젯 바인딩을 관리한다. Activity context 대신 applicationContext를 사용하면 Activity 참조 체인을 끊어 별도 누수 경로를 차단한다

---

## [2026-03-17] refactor(nav): Navigation Type-Safe 리팩토링

### 목표

- 문자열 기반 route 상수 (`SettingsRoute`, `MarketRoute`)를 `@Serializable` 클래스/객체 방식으로 전환한다
- `navArgument()` / 수동 `Uri.decode` 파싱을 제거하고 `toRoute<T>()`로 대체한다
- sealed interface로 route 그룹을 묶어 `(Any)` 대신 타입 안전한 콜백 시그니처를 사용한다
- 컴파일 타임 타입 안전성을 확보해 라우트 오타·파라미터 불일치를 빌드 시점에 검출한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `app/build.gradle.kts` | `alias(libs.plugins.kotlin.serialization)` + `kotlinx-serialization-json` 의존성 추가 |
| `feature/settings/build.gradle.kts` | 동일 (serialization 플러그인 + 의존성) |
| `feature/preset-market/build.gradle.kts` | 동일 |
| `feature/settings/.../navigation/SettingsRoute.kt` | 기존 `object SettingsRoute` 문자열 상수 삭제 → `sealed interface LauncherRoute` + `@Serializable object Launcher`, `@Serializable data class SettingsDetail(val menu: String)`, `@Serializable object PresetMarketHost` (모두 `LauncherRoute` 구현) |
| `feature/preset-market/.../navigation/MarketRoute.kt` | 기존 `object MarketRoute` 문자열 상수 삭제 → `sealed interface MarketRoute` + `@Serializable object MarketHome`, `@Serializable data class MarketDetail(val presetId: String)`, `@Serializable data class MarketSearch(val query: String = "")` (모두 `MarketRoute` 구현) |
| `feature/settings/.../ui/SettingsScreen.kt` | `onNavigate: (String) -> Unit` → `(LauncherRoute) -> Unit`; `SettingsRoute.detail(menu)` → `SettingsDetail(menu)` 직접 전달 |
| `feature/preset-market/.../ui/PresetMarketHost.kt` | `composable<MarketHome/MarketSearch/MarketDetail>` 전환; `navArgument` / `Uri.decode` 제거; `backStackEntry.toRoute<MarketSearch>()` 로 query 추출; `navigate(MarketDetail(presetId = it))` 등 타입 안전 navigate |
| `app/.../MainActivity.kt` | `startDestination = Launcher`; `composable<Launcher/SettingsDetail/PresetMarketRoute>`; `backStackEntry.toRoute<SettingsDetail>()`; `popBackStack(Launcher, inclusive = false)` (객체 인스턴스 직접 전달); `isOpaqueRoute(String?)` → `isOpaqueDestination(NavDestination)` (`qualifiedName` 문자열 비교); `currentRoute: String?` → `isCurrentScreenOpaque: Boolean` |

### 검증 결과

- `JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew assembleDebug` → **BUILD SUCCESSFUL**
- `JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew test` → **BUILD SUCCESSFUL** (실패 0건)

### 설계 결정 및 근거

- **sealed interface로 route 그룹화**: `LauncherRoute` / `MarketRoute` sealed interface를 두어 콜백 타입을 `(Any)` 대신 `(LauncherRoute)` 로 좁힘. 잘못된 route 객체 전달을 컴파일 타임에 차단하고 when 분기 exhaustive 검사도 가능해짐
- **`popBackStack` 객체 인스턴스 전달**: `Launcher::class` (KClass Reflection) 대신 `Launcher` 인스턴스를 직접 전달. Navigation 2.8+ `popBackStack(route: Any, ...)` API를 활용해 Reflection 없이 동작함
- **`PresetMarketHost` 네이밍 충돌 해소**: `feature:settings` 의 route 객체 `PresetMarketHost` 와 `feature:preset-market` 의 Composable `PresetMarketHost` 가 동일 이름이므로, `MainActivity` 에서 `import ... PresetMarketHost as PresetMarketRoute` 별칭을 사용해 두 식별자를 구분함
- **`NavDestination.hasRoute(KClass<T>)` 미지원**: Navigation 2.9.7 에서 KClass 기반 `hasRoute` 확장이 제공되지 않아, `dest.route?.startsWith(T::class.qualifiedName)` 방식으로 불투명 화면 판별. qualifiedName은 패키지+클래스명이므로 prefix 충돌 위험 없음
- **`currentRoute: String?` 제거**: 불투명 여부 판별에만 사용되던 문자열 상태를 `isCurrentScreenOpaque: Boolean` 으로 단순화해 `updateSystemBarStyle` 시그니처를 명확히 함
- **ViewModel 무변경**: type-safe 전환 후에도 `savedStateHandle["presetId"]` 방식은 여전히 동작하므로 ViewModel 수정 범위를 최소화함

---

## [2026-03-16] fix(dragdrop): 앱 드로어에서 홈 셀 드롭 누락 보정

### 목표

- 앱 드로어에서 홈으로 자동 스크롤된 직후 드롭해도 대상 그리드 셀이 정상 감지되게 한다
- 홈 셀 bounds가 늦게 등록되는 타이밍 문제로 드롭이 취소되는 현상을 막는다
- 홈 전환 중에도 앱 드로어 소스 드래그 제스처가 끊기지 않게 한다
- 홈 셀 간 이동 시 축소된 소스 셀에서 드래그 아이템이 dispose되어 드래그가 끊기지 않게 한다
- 홈 셀 간 이동 중 타깃 셀 접촉으로 확장 셀이 즉시 바뀌며 드래그가 취소되지 않게 한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/ui/.../dragdrop/DragDropState.kt` | `endDrag()` 직전에 마지막 `dragOffset`으로 한 번 더 히트 테스트를 수행해 최신 셀 bounds를 반영하도록 수정 |
| `core/ui/.../dragdrop/DragDropStateTest.kt` | 자동 스크롤 뒤 늦게 등록된 셀 bounds도 드롭 결과에 반영되는 회귀 테스트 추가 |
| `app/.../navigation/CrossPagerNavigation.kt` | 드래그 중에는 `VerticalPager`가 인접 페이지를 유지하도록 `beyondViewportPageCount`를 `1`로 올려 앱 드로어 source `pointerInput`이 dispose되지 않게 수정 |
| `feature/launcher/.../ui/HomeScreen.kt` | dynamic capacity 밖으로 밀린 드래그 소스 앱도 숨김 상태로 트리에 유지하고, 홈 내부 드래그 중에는 확장 셀을 소스 셀로 고정해 타깃 접촉 시 레이아웃 급변으로 드래그가 취소되지 않도록 수정 |

### 검증 결과

- `ReadLints` 기준 수정 파일 신규 오류 없음
- `JAVA_HOME=\"C:/Program Files/Java/jdk-17\" .\\gradlew.bat :core:ui:testDebugUnitTest :app:compileDebugKotlin` → **BUILD SUCCESSFUL**

### 설계 결정 및 근거

- **종료 시점 재평가**: 앱 드로어에서 홈으로 넘어가는 동안 레이아웃 composition이 늦게 끝날 수 있으므로, 드래그 중간 상태가 아니라 드롭 직전 좌표로 최종 타깃을 다시 계산하는 편이 더 안정적임
- **상태 구조 유지**: 드롭 경로 전체를 바꾸지 않고 `DragDropState` 내부에서 마지막 히트 테스트만 보강해 기존 홈 내부 이동 동작에 영향 범위를 최소화함
- **소스 제스처 유지**: 앱 드로어에서 홈으로 자동 이동할 때 인접 페이지를 계속 compose 상태로 유지해 long press drag coroutine이 페이지 전환으로 끊기지 않게 함
- **소스 아이템 생존 보장**: 홈 내부 드래그에서는 소스 셀이 축소되며 dynamic capacity가 줄 수 있으므로, 드래그 중인 앱이 보이는 슬롯에서 빠져도 별도 hidden item으로 composition을 유지함
- **홈 내부 레이아웃 고정**: 같은 홈 안에서 셀 간 이동할 때는 호버 셀을 즉시 확장하지 않고 소스 셀 확장을 유지해, 셀 접촉 순간 전체 레이아웃이 다시 계산되며 pointerInput이 흔들리는 문제를 줄임

---

## [2026-03-17] fix(market): 프리셋 마켓 다운로드 후 배경화면 미적용 버그 수정

### 목표

- 프리셋 마켓에서 프리셋을 다운로드했을 때 시스템 배경화면(wallpaper)이 즉시 적용되도록 한다
- 로컬 프리셋 로드 시 적용되는 wallpaper와 동일한 경로로 마켓 다운로드도 처리한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../usecase/DownloadMarketPresetUseCase.kt` | 생성자에 `WallpaperHelper` 주입 추가; `applySettings()`에 `hasWallpaperSettings` 조건부 `wallpaperHelper.setWallpaperFromPreset(wallpaperUri)` 호출 추가 |

### 검증 결과

- `assembleDebug` → **BUILD SUCCESSFUL**
- `./gradlew test` → **BUILD SUCCESSFUL** (실패 0건)

### 설계 결정 및 근거

- **누락 위치**: `SettingsViewModel`에서 로컬 프리셋을 적용할 때는 `wallpaperHelper.setWallpaperFromPreset()` 호출이 존재했으나, `DownloadMarketPresetUseCase.applySettings()`에는 동일 로직이 없어 마켓 다운로드 시 wallpaper만 건너뛰어지던 문제
- **`core:domain` 원칙 유지**: `WallpaperHelper`는 `core:domain`에 정의된 인터페이스이므로 Android 의존성 없이 주입 가능 — 순수 JVM 모듈 원칙 위반 없음
- **영향 범위 최소화**: `applySettings()` 내부에 한 줄 추가로 V1/V2 양쪽 경로(`invokeV1`, `invokeV2`, `downloadWithProgressV1`, `downloadWithProgressV2`) 모두 자동 적용

---

## [2026-03-17] feat(market): Google 로그인 안내 다이얼로그 추가

### 목표

- 다운로드·업로드·좋아요 등 로그인이 필요한 기능 실행 시, 바로 Google 로그인 시트가 뜨지 않고 먼저 안내 다이얼로그를 표시한다
- 사용자가 왜 로그인이 필요한지 이해하고 선택적으로 진행할 수 있도록 UX를 개선한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/ui/src/main/res/values/strings.xml` | **신규** — 로그인 안내 다이얼로그용 문자열 4개 추가 (`sign_in_required_title/message/confirm/cancel`) |
| `core/ui/.../component/GoogleSignInRequiredDialog.kt` | **신규** — `AlertDialog` 기반 재사용 컴포저블. 확인 시 `onConfirm`, 취소 시 `onDismiss` 콜백 |
| `feature/preset-market/.../ui/PresetDetailScreen.kt` | `showSignInDialog` 상태 추가; `RequireSignIn` → `showSignInDialog = true`; 다이얼로그 confirm 시 `showSignInHandler = true` |
| `feature/preset-market/.../ui/MarketHomeScreen.kt` | `showSignInDialog` 상태 추가; `RequireSignIn` 핸들러 및 수동 로그인 버튼 → `showSignInDialog = true`; 다이얼로그 confirm 시 `showSignIn = true` |
| `app/.../MainActivity.kt` | `showSettingsSignInDialog` 상태 추가; `RequireSignIn` → `showSettingsSignInDialog = true`; 다이얼로그 confirm 시 `showSettingsSignIn = true` |

### 검증 결과

- `assembleDebug` → **BUILD SUCCESSFUL**
- `./gradlew test` → **BUILD SUCCESSFUL** (실패 0건)

### 설계 결정 및 근거

- **`core:ui`에 다이얼로그 배치**: `PresetDetailScreen`, `MarketHomeScreen`, `MainActivity` 세 곳 모두에서 동일한 다이얼로그가 필요하므로 공용 컴포넌트로 추출해 중복 구현을 방지
- **Contract/ViewModel 무변경**: 로그인 트리거 흐름(`RequireSignIn` side effect → `GoogleSignInHandler`)은 그대로 유지하고, UI 레이어에서만 중간 상태를 추가해 변경 범위를 최소화
- **수동 로그인 버튼도 동일 다이얼로그 경유**: `MarketHomeScreen` TopAppBar의 로그인 아이콘 버튼도 `showSignInDialog = true`로 전환해 진입 경로에 상관없이 일관된 안내 UX를 제공

---

## [2026-03-16] feat(home): 확장 셀 아이콘 크기와 수용 개수 동기화

### 목표

- 홈 확장 셀의 앱 아이콘 크기를 앱 드로어와 동일한 계산 기준으로 맞춘다
- 앱 드로어 설정의 `iconSizeRatio`가 홈 확장 셀에도 그대로 반영되도록 한다
- 확장 셀 크기에 따라 표시 가능한 슬롯 수를 동적으로 계산하고, 초과 배치는 기존처럼 스킵한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/ui/.../component/AppGridSizing.kt` | 앱 드로어/홈 화면이 함께 쓰는 공용 아이콘 크기 및 그리드 계산 함수를 추가 |
| `feature/apps-drawer/.../ui/AppDrawerScreen.kt` | 앱 드로어가 공용 계산식을 사용하도록 정리해 홈과 동일한 아이콘 크기 정책을 사용 |
| `feature/launcher/.../HomeContract.kt` | 셀별 동적 수용 개수를 상태로 보관하고 UI에서 갱신할 수 있는 intent를 추가 |
| `feature/launcher/.../HomeViewModel.kt` | 앱 배치/셀 간 이동 시 고정 6개 대신 셀별 동적 capacity를 참조하고, 가득 찬 셀로 이동 시 원본 유지 후 스킵하도록 보강 |
| `feature/launcher/.../ui/HomeScreen.kt` | 확장 셀을 `BoxWithConstraints` 기반 동적 그리드로 바꾸고, 빈 슬롯도 드롭 타깃으로 등록하며 공용 아이콘 크기 계산을 적용 |
| `feature/launcher/.../HomeViewModelTest.kt` | 동적 capacity 초과 추가 스킵 및 가득 찬 대상 셀 이동 스킵 테스트를 추가/갱신 |

### 검증 결과

- `ReadLints` 기준 수정 파일 신규 오류 없음
- `JAVA_HOME=\"C:/Program Files/Java/jdk-17\" .\\gradlew.bat :feature:launcher:testDebugUnitTest :app:compileDebugKotlin` → **BUILD SUCCESSFUL**

### 설계 결정 및 근거

- **설정값 재사용**: 홈 전용 아이콘 크기 설정을 새로 만들지 않고, 기존 `appDrawerIconSizeRatio`를 공통 계산식에 반영해 설정 의미를 일관되게 유지함
- **공용 계산식 사용**: 앱 드로어와 홈이 같은 최소/최대 아이콘 정책을 쓰도록 계산을 한 곳에 모아 화면 간 체감 크기 차이를 줄임
- **동적 capacity 반영**: 홈 셀의 실제 확장 크기에서 계산한 슬롯 수를 ViewModel에도 전달해, 보이는 개수와 배치 가능 개수가 어긋나지 않도록 맞춤
- **빈 슬롯 드롭 보강**: 슬롯 bounds를 빈 칸까지 등록해 동적 그리드에서도 드래그 타깃 인덱스가 안정적으로 계산되도록 정리함

---

## [2026-03-16] fix(app-drawer): 가로 모드에서 앱 드로어 행/열 전환

### 목표

- App Drawer가 portrait와 landscape에서 동일한 행/열 값을 그대로 쓰지 않도록 조정한다
- 세로 기준 배치가 `4(column) x 6(row)`일 때 가로에서는 `6(column) x 4(row)`처럼 transpose된 배치를 적용한다
- 검색, 페이지 분할, 페이지 인디케이터, 드래그 앤 드롭 등 기존 App Drawer 동작은 그대로 유지한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `app/.../MainActivity.kt` | `LocalConfiguration`으로 현재 orientation을 읽어 landscape일 때 `uiState.appDrawerGridColumns`와 `uiState.appDrawerGridRows`를 swap한 뒤 `AppDrawerScreen`에 전달 |

### 검증 결과

- `ReadLints` 기준 `app/.../MainActivity.kt` 신규 오류 없음
- `JAVA_HOME=\"C:/Program Files/Java/jdk-17\" .\\gradlew.bat :app:compileDebugKotlin` → **BUILD SUCCESSFUL**

### 설계 결정 및 근거

- **호출부 한정 변경**: `AppDrawerScreen`은 전달받은 `columns`/`rows`만으로 페이지 수와 셀 배치를 계산하므로, 호출부에서만 값을 전환하면 기능 요구사항을 충족할 수 있음
- **세로 기준 설정 유지**: 저장소와 설정 화면의 기본값은 그대로 두고, landscape에서는 표시만 transpose해 사용자가 설정한 세로 기준 레이아웃 의미를 유지함
- **기존 동작 보존**: 앱 정렬, 검색 필터링, 페이지 계산 로직은 그대로 두고 실제 표시 축만 바꿔 회전 대응 범위를 최소화함

---

## [2026-03-16] fix(leak): 홈 이미지/아이콘 경량화 및 잔여 회전 누수 보강

### 목표

- 회전 후 남는 `AndroidComposeView -> MainActivity` retain을 추가로 줄인다
- 홈 배경/앱 아이콘/피드 로딩 애니메이션처럼 retain 크기에 직접 기여할 수 있는 경로를 경량화한다
- 위젯 호스트 detach/Activity 종료 시 cleanup을 보강한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/launcher/.../ui/HomeScreen.kt` | 셀 배경 이미지 요청을 `applicationContext` 기반으로 변경, `crossfade` 제거, 셀 크기 기반 `size()` 지정 |
| `core/ui/.../component/AppIcon.kt` | `SubcomposeAsyncImage` 제거, `rememberAsyncImagePainter` 기반 단순화, 아이콘 요청을 `applicationContext` + no crossfade로 변경 |
| `feature/launcher/.../ui/FeedScreen.kt` | refresh 회전 루프를 infinite transition으로 대체하고 `isVisible`일 때만 회전, 채널 아바타 요청도 `applicationContext` 기반으로 변경 |
| `feature/widget/.../ui/WidgetScreen.kt` | `WidgetContainerView.onDetachedFromWindow()`에서 hosted view를 추가 해제 |
| `app/.../MainActivity.kt` | `onDestroy()`에서 `AppWidgetHost.stopListening()` 재호출 및 미완료 `pendingWidgetId` 정리 |

### 검증 결과

- 구현 후 빌드/린트 검증 예정

### 설계 결정 및 근거

- **이미지 요청의 Activity 의존 축소**: 홈 배경/앱 아이콘/피드 아바타 요청에 `applicationContext`를 사용해 회전 시 old Activity가 이미지 요청 컨텍스트로 남을 가능성을 줄임
- **비트맵 retain 축소**: 홈 셀 배경은 실제 셀 크기에 맞춰 decode size를 제한해 대형 비트맵이 retain size를 키우는 것을 완화
- **무한 루프 제거**: `while(true)` 코루틴을 Compose animation API로 치환해 old composition이 남아도 코루틴이 subtree를 오래 붙잡지 않도록 조정
- **WidgetHost 방어적 정리**: 회전 detach와 Activity 종료 모두에서 `AppWidgetHostView`/`AppWidgetHost` 정리를 한 번 더 수행해 widget 경로의 잔여 참조 가능성을 줄임

---

## [2026-03-16] experiment(leak): Pager precompose 축소 및 마켓 SharedTransition 범위 분리

### 목표

- 회전 시 발생하는 `AndroidComposeView -> MainActivity` retain을 줄이기 위해 오프스크린 pager 유지 범위를 축소한다
- `SharedTransitionLayout`을 루트 `NavHost`에서 제거하고 프리셋 마켓 내부로 한정해 홈 회전 시 마켓 transition machinery가 루트 composition에 남지 않게 한다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `app/.../navigation/CrossPagerNavigation.kt` | 세로/가로 pager의 `beyondViewportPageCount`를 `1 -> 0`으로 조정 |
| `feature/preset-market/.../navigation/MarketRoute.kt` | 마켓 외부 진입용 `HOST` route 추가 |
| `feature/preset-market/.../ui/PresetMarketHost.kt` | 신규. 마켓 전용 `NavHost` + `SharedTransitionLayout` 구성, `HOME/SEARCH/DETAIL`을 내부 네비게이션으로 이동 |
| `app/.../MainActivity.kt` | 루트 `SharedTransitionLayout` 제거, 마켓 진입을 `MarketRoute.HOST`로 변경, 시스템바의 opaque route 판별도 호스트 기준으로 조정 |

### 검증 결과

- `ReadLints` 기준 신규 오류 없음
- 빌드 검증 예정

### 설계 결정 및 근거

- **Pager 실험 우선**: 변경량이 작고 홈 회전 시 남는 오프스크린 subtree(`Feed`/`Widget`)를 직접 줄일 수 있어 1차 분리 실험에 적합
- **마켓 호스트 분리**: 실제 shared transition 사용은 검색바 전환 한 쌍뿐이라, 루트 전체가 아니라 마켓 내부 navigation 범위에서만 `SharedTransitionLayout`을 유지하도록 축소
- **루트 route 단순화**: `MainActivity`는 마켓을 하나의 불투명 화면(`HOST`)으로 취급하고, 내부 `HOME/SEARCH/DETAIL`은 마켓 feature가 자체적으로 관리하도록 정리

---

## [2026-03-16] fix(leak): AdMob 수명주기 보강 및 AndroidView cleanup 정리

### 목표

- LeakCanary가 보고한 `MainActivity` retain 이슈 중 AdMob 배너 관련 누수 가능성을 줄인다
- 광고 on/off 비교 재현을 위해 코드 수정 없이 배너를 비활성화할 수 있는 디버그 플래그를 추가한다
- `AndroidView` 기반 위젯/콜백 cleanup을 보강해 잔여 참조를 줄인다

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/preset-market/.../ui/AdmobBanner.kt` | `AdView`를 Compose release 시점뿐 아니라 `Lifecycle`의 `ON_PAUSE`/`ON_RESUME`/`ON_DESTROY`에도 연동. 해제 시 parent detach 후 `destroy()` 호출 |
| `feature/preset-market/build.gradle.kts` | `local.properties`의 `debug.enable.banner.ads` 값을 읽어 `BuildConfig.ENABLE_BANNER_ADS` 생성. 광고 on/off 비교 재현 지원 |
| `app/.../navigation/CrossPagerNavigation.kt` | `dragDropState.onScrollToHome` 등록을 `DisposableEffect`로 변경하고 dispose 시 null 정리 |
| `feature/widget/.../ui/WidgetScreen.kt` | `WidgetContainerView`에 hosted `AppWidgetHostView` attach/release helper 추가, `AndroidView.onRelease`에서 cleanup 수행 |

### 검증 결과

- 구현 후 빌드/린트 검증 예정

### 설계 결정 및 근거

- **AdMob 조기 해제**: `onRelease`만으로는 액티비티 파괴 타이밍보다 늦을 수 있어 `ON_DESTROY`에서도 parent detach + `destroy()`를 실행해 SDK 내부 참조가 오래 남는 시간을 줄인다
- **비교 재현 플래그**: 배너 코드를 다시 수정하지 않고도 `local.properties`에서 `debug.enable.banner.ads=false`로 꺼서 Ads 누수와 Compose 2차 증상을 분리 관찰할 수 있게 함
- **AndroidView cleanup 일관화**: 위젯 뷰와 드래그 콜백 모두 compose dispose 시 명시적으로 참조를 끊어 `MainActivity` 루트가 오래 유지될 가능성을 줄임

---

## [2026-03-14] fix+perf(market): 프리뷰 이미지 업로드 누락 수정 및 해상도 축소

### 목표

- `UploadPresetToMarketUseCase`에서 프리뷰 이미지(`previewImageUrls`)가 Firestore 문서에 포함되지 않던 버그 수정
- 프리셋 업로드 시 프리뷰/썸네일 이미지를 원본 해상도 그대로 전송하는 문제 개선
- `MarketPresetRepository.uploadImage()`에 `maxWidth`/`quality` 파라미터를 추가하여 업로드 용도별로 다른 해상도 적용

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../usecase/UploadPresetToMarketUseCase.kt` | `invoke()` + `uploadWithProgress()` 두 메서드에 프리뷰 이미지 개별 업로드 블록 추가 (`previewImageUrls`) 및 `finalPreset.copy()`에 `previewImageUrls` 포함 |
| `core/domain/.../repository/MarketPresetRepository.kt` | `uploadImage()`에 `maxWidth: Int = 1080`, `quality: Int = 80` 기본값 파라미터 추가 |
| `core/data/.../repository/MarketPresetRepositoryImpl.kt` | `uploadImage()` 시그니처 일치 업데이트. `ImageCompressor.compressToWebP()` 호출 시 `maxWidth`/`quality` 전달 |

해상도 기준:

| 용도 | maxWidth | quality |
|------|----------|---------|
| 기본 (기존 호출부) | 1080 | 80 |
| 프리뷰 이미지 | 720 | 70 |
| 썸네일 | 480 | 70 |

### 검증 결과

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (46s)
- `./gradlew test` → **BUILD SUCCESSFUL**, 실패 0건 (464 tasks)

### 설계 결정 및 근거

- **버그 원인**: `.slp` 포맷 도입(schemaVersion=2) 이후 `previewImageUrls`가 `finalPreset.copy()`에서 누락된 채로 Firestore에 빈 리스트로 저장되고 있었음. 상세 화면의 프리뷰 슬라이더가 표시되지 않는 증상
- **기본값 유지**: 인터페이스·구현체 모두 기본값(`1080px, 80%`)을 그대로 두어 기존 호출부(셀 이미지 등) 영향 없음
- **단방향 파라미터 흐름**: UseCase(도메인) → Repository 인터페이스 → 구현체 → ImageCompressor 순서로 해상도 결정 책임이 도메인 레이어에 집중됨

---

## [2026-03-14] refactor(slp): DownloadMarketPresetUseCase를 domain 레이어로 이동 — PresetUnpackager 인터페이스 추출

### 목표

- `DownloadMarketPresetUseCase`가 `core:data`에 위치하며 Android `Context`에 직접 의존하는 구조를 업로드 플로우(`PresetPackager`)와 동일한 패턴으로 리팩토링
- Android 파일 I/O 로직을 `PresetUnpackager` 인터페이스로 추상화하여 UseCase를 `core:domain`으로 이동
- `core:domain`이 Android 의존성 없이 순수 비즈니스 로직만 보유하도록 레이어 경계 복원

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../repository/PresetUnpackager.kt` | **신규** — `UnpackedPresetResult(localPreset)` 데이터 클래스 + `PresetUnpackager` 인터페이스 (`downloadAndUnpack()`, `downloadLegacyImages()`, `cleanupPresetDir()`) |
| `core/data/.../slp/PresetUnpackagerImpl.kt` | **신규** — `@ApplicationContext Context` + `MarketPresetRepository` 주입. V2(SlpUnpacker)/V1(개별 이미지) 파일 I/O 구현 |
| `core/data/.../di/PresetUnpackagerModule.kt` | **신규** — `@Binds PresetUnpackagerImpl → PresetUnpackager` (`@InstallIn(SingletonComponent)`) |
| `core/domain/.../usecase/DownloadMarketPresetUseCase.kt` | **신규(이동)** — `PresetUnpackager` 주입으로 Context 제거. `invoke()` + `downloadWithProgress()` (Flow). V1도 3단계 coarse progress로 단순화 |
| `core/data/.../usecase/DownloadMarketPresetUseCase.kt` | **삭제** — `domain.usecase.DownloadMarketPresetUseCase`로 대체 |
| `app/.../service/PresetDownloadService.kt` | import 경로 수정: `data.usecase` → `domain.usecase` |

### 검증 결과

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (1m 9s)
- `./gradlew test` → **BUILD SUCCESSFUL**, 실패 0건 (464 tasks)

### 설계 결정 및 근거

- **PresetPackager와 대칭 구조**: 업로드는 `PresetPackager(pack/deleteTempFile)`, 다운로드는 `PresetUnpackager(downloadAndUnpack/downloadLegacyImages/cleanupPresetDir)`. 인터페이스명과 메서드 분류 방식이 서로 대응됨
- **V1 progress 단순화**: 기존 V1은 이미지 수(n) + 2 단계였으나, `PresetUnpackager`로 분리하면서 UseCase 레벨에서는 3단계(download → apply → increment) coarse progress로 통일. V1은 레거시이므로 세밀한 진행률 불필요
- **cleanupPresetDir 책임**: 파일 삭제는 Context가 필요하므로 `PresetUnpackager` 인터페이스에 포함. 에러 핸들링(catch 블록)은 UseCase에 남겨 도메인 로직 집중도 유지
- **모듈 의존성 변경 없음**: `core:domain`은 pure JVM 유지, `core:data`는 기존과 동일한 의존 구조

---

## [2026-03-13] refactor(slp): UploadPresetToMarketUseCase를 domain 레이어로 복원 — PresetPackager 인터페이스 추출

### 목표

- `UploadMarketPresetUseCase`가 `SlpPacker`(Android Context + ImageCompressor)에 직접 의존하여 `core:data`에 배치된 상태를 Clean Architecture 원칙에 따라 복원
- Android 의존성을 `PresetPackager` 인터페이스로 추상화하여 UseCase를 다시 `core:domain`으로 이동
- `core:domain`이 Android 의존성 없이 순수 비즈니스 로직만 보유하도록 레이어 경계 복원

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../repository/PresetPackager.kt` | **신규** — `PackedPresetResult(slpFilePath, presetTemplate)` 데이터 클래스 + `PresetPackager` 인터페이스 (`packPreset()`, `deleteTempFile()`) |
| `core/data/.../slp/PresetPackagerImpl.kt` | **신규** — `SlpPacker.pack()` + `SlpPacker.buildManifest().toMarketPreset()` 위임. `@ApplicationContext Context` 주입 |
| `core/data/.../di/PresetPackagerModule.kt` | **신규** — `@Binds PresetPackagerImpl → PresetPackager` (`@InstallIn(SingletonComponent)`) |
| `core/domain/.../usecase/UploadPresetToMarketUseCase.kt` | **재생성** — `MarketPresetRepository` + `PresetPackager` 주입. Android import 없음. `invoke()` + `uploadWithProgress()` (Flow 3단계 진행률) |
| `core/data/.../usecase/UploadMarketPresetUseCase.kt` | **삭제** — `UploadPresetToMarketUseCase`(core:domain)로 대체 |
| `app/.../service/PresetUploadService.kt` | import 경로 복원: `data.usecase.UploadMarketPresetUseCase` → `domain.usecase.UploadPresetToMarketUseCase` |
| `core/data/.../slp/SlpPacker.kt` | 주석 내 UseCase 참조명 업데이트 |

### 검증 결과

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (22s)
- `./gradlew test` → **BUILD SUCCESSFUL**, 실패 0건 (464 tasks)

### 설계 결정 및 근거

- **PresetPackager 인터페이스 위치**: `core:domain/repository/` — domain 레이어가 자신이 필요로 하는 추상화를 직접 정의(DIP). `PackedPresetResult`도 domain 모델이므로 같은 파일에 배치
- **PresetPackagerImpl 위치**: `core:data/slp/` — SlpPacker/SlpMapper/ImageCompressor 등 모두 이미 data 레이어에 존재하므로 응집도 유지
- **DownloadMarketPresetUseCase는 core:data 유지**: 이번 작업 이전부터 data에 있었고, 언패킹 로직(SlpUnpacker)이 data에만 존재하므로 현재 위치 유지가 더 일관적
- **uploadWithProgress 로직 보존**: 기존 3단계 진행률(패킹 → SLP 업로드 → 썸네일+Firestore) 동일하게 재구현하여 PresetUploadService 동작 변화 없음

---

## [2026-03-13] feat(slp): 프리셋 커스텀 포맷 .slp 구현 (단일 압축 전송)

### 목표

- 프리셋 업로드/다운로드 시 이미지를 개별 전송하는 방식(최대 ~13회 요청) → 단일 `.slp` 파일 1회 전송으로 교체
- ZIP 기반 커스텀 포맷(`.slp` = StreamLauncher Preset)으로 이미지 + 메타데이터를 번들링
- Firestore DTO 1개 + Storage 파일 1개만 전송하는 원자적 구조 구현
- 기존 v1(개별 URL) 프리셋과 하위 호환 유지

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/data/.../slp/SlpManifest.kt` | **신규** — `.slp` 내 `manifest.json` 스키마: `SlpManifest`, `SlpImagePaths`, `SlpCellFlags`, `SlpFeedSettings`, `SlpAppDrawerSettings`, `SlpWallpaperSettings`, `SlpThemeSettings` (@Serializable) |
| `core/data/.../slp/SlpPacker.kt` | **신규** — `pack(context, preset, previewUris, outDir, presetId): File`. 이미지 WebP 압축(ImageCompressor 재사용) + ZipOutputStream으로 .slp 생성. 이미지 엔트리는 STORED, manifest.json은 DEFLATED. `buildManifest()` 공개 메서드 별도 추출 |
| `core/data/.../slp/SlpUnpacker.kt` | **신규** — `unpack(slpFile, targetDir): Pair<SlpManifest, Map<String, String>>`. ZipInputStream 추출 + 경로 순회 공격(`../`) 차단 + manifest.json 파싱 |
| `core/data/.../slp/SlpMapper.kt` | **신규** — `SlpManifest.toLocalPreset(extractedPaths, marketPresetId)`, `SlpManifest.toMarketPreset(id, slpStorageUrl, thumbnailUrl)` 확장 함수 |
| `core/domain/.../model/preset/MarketPreset.kt` | `slpStorageUrl: String? = null` 필드 추가 |
| `core/domain/.../repository/MarketPresetRepository.kt` | `uploadSlpFile(localPath, storagePath)`, `downloadSlpFile(storageUrl, localPath)` 추가 |
| `core/data/.../remote/firestore/MarketPresetDto.kt` | `slpStorageUrl: String? = null` 필드 추가 + `toDomain()` / `toDto()` 매퍼 반영 |
| `core/data/.../repository/MarketPresetRepositoryImpl.kt` | `uploadSlpFile()` (`putFile` 사용), `downloadSlpFile()` (`getFile` 사용) 구현 |
| `core/data/.../usecase/UploadMarketPresetUseCase.kt` | **신규** (core:domain의 기존 UseCase 대체) — SlpPacker 기반 3단계 업로드: 패킹 → .slp Storage 업로드 → 썸네일 별도 업로드 + Firestore 문서 생성. `schemaVersion=2` |
| `core/domain/.../usecase/UploadPresetToMarketUseCase.kt` | **삭제** — `UploadMarketPresetUseCase`(core:data)로 대체 |
| `core/data/.../usecase/DownloadMarketPresetUseCase.kt` | `slpStorageUrl` 유무로 v2/v1 분기: v2는 .slp 1회 다운로드 → SlpUnpacker 추출 → 설정 적용, v1(레거시)은 기존 개별 다운로드 유지 |
| `app/.../service/PresetUploadService.kt` | inject 타입을 `UploadPresetToMarketUseCase` → `UploadMarketPresetUseCase`로 변경 |

### 검증 결과

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (16s)
- `./gradlew test` → **BUILD SUCCESSFUL**, 실패 0건 (464 tasks)

### 설계 결정 및 근거

- **ZIP + .slp 확장자**: `java.util.zip` 사용으로 추가 의존성 없음. `.slp` 확장자는 브랜딩용. ZIP은 범용 툴로 디버깅 용이
- **이미지 엔트리 STORED(무압축)**: WebP는 이미 손실 압축됨. ZIP DEFLATED를 추가 적용해도 파일 크기 절감 효과 미미하며 pack/unpack 속도 손해
- **썸네일은 별도 업로드**: Firestore 목록(MarketHomeScreen, 검색 결과)에서 .slp 전체를 다운로드하지 않고 썸네일만 표시해야 하므로 별도 Storage 객체로 유지
- **v1 하위 호환**: `slpStorageUrl == null`이면 기존 개별 URL 다운로드 경로 실행. Firestore 문서 구조 그대로 유지하여 마이그레이션 불필요
- **UploadPresetToMarketUseCase를 core:data로 이동**: SlpPacker가 `Context`(ContentResolver) + `ImageCompressor`(core:data)에 의존하므로 core:domain에서 접근 불가. DownloadMarketPresetUseCase와 동일한 패턴으로 core:data에 배치
- **Firestore 스키마**: v2 문서는 `schemaVersion=2`, `slpStorageUrl` 포함, 개별 이미지 URL 필드는 null. v1/v2 문서가 동일 컬렉션에 공존하며 클라이언트가 분기 처리

---

## [2026-03-12] feat(widget): 위젯 자유 배치 시스템 구현 (동적 그리드 + 드래그/리사이즈)

### 목표

- 기존 2×3 고정 슬롯 방식 → 70dp 셀 기반 동적 그리드로 전환
- 위젯이 자연스러운 크기(minWidth/minHeight)로 배치되도록 개선
- 편집 모드에서 드래그로 위젯 이동, 우하단 핸들로 크기 조절
- 기존 CSV 슬롯 데이터를 JSON 형식으로 3단계 마이그레이션

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../model/WidgetPlacement.kt` | **신규** — `appWidgetId`, `column`, `row`, `columnSpan`, `rowSpan`, `minColumnSpan`, `minRowSpan` 필드 |
| `core/domain/.../model/WidgetGrid.kt` | **신규** — `CELL_SIZE_DP=70`, `computeColumns(widthDp)`, `computeRows(heightDp)` (최소 3열×4행 보장) |
| `core/domain/.../repository/WidgetRepository.kt` | 인터페이스 전면 교체: `getWidgetPlacements()`, `addWidget`, `removeWidget`, `updateWidgetPlacement`, `updateWidgetSize`, `migrateLegacySlots` |
| `core/data/.../repository/WidgetRepositoryImpl.kt` | JSON 배열 저장(`widget_placements`), `@Serializable WidgetPlacementDto`, 3단계 마이그레이션(single int → packed CSV → slots CSV → JSON) |
| `feature/widget/.../WidgetContract.kt` | **신규** — `WidgetState`(placements/isEditMode/draggingWidgetId/resizingWidgetId/preview 필드), `WidgetIntent` sealed interface |
| `feature/widget/.../WidgetViewModel.kt` | MVI 패턴 전면 재작성: `handleIntent`, `findAvailablePosition`(행 우선 탐색), 겹침 감지, 드래그/리사이즈 프리뷰 상태 관리 |
| `feature/widget/.../ui/WidgetScreen.kt` | Box 기반 자유 배치 UI: `onGloballyPositioned` 그리드 측정, `Modifier.offset+size` 절대 배치, 편집 모드 그리드 가이드라인(점선), 드래그/리사이즈 프리뷰 사각형, 우상단 삭제 버튼, 우하단 리사이즈 핸들(대각선 3줄), `rememberUpdatedState` 패턴으로 gesture handler 안정성 확보 |
| `feature/widget/.../res/values/strings.xml` | `widget_resize` 문자열 추가 |
| `feature/widget/.../res/values-en/strings.xml` | `widget_resize` 영문 추가 |
| `app/.../navigation/CrossPagerNavigation.kt` | `isWidgetDragging: Boolean` 파라미터 추가 → 드래그 중 `HorizontalPager.userScrollEnabled = false` |
| `app/.../MainActivity.kt` | `pendingSlot` 제거, `launchWidgetPicker()` 슬롯 인자 제거, `addPendingWidget(widgetId)` 신규(minWidth/minHeight → minCols/minRows 계산), `deleteWidget(appWidgetId)` appWidgetId 기반으로 변경, `widgetState` 단일 StateFlow 수집 |

### 검증 결과

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (53s)
- `./gradlew test` → **BUILD SUCCESSFUL**, 실패 0건

### 설계 결정 및 근거

- **셀 크기 70dp**: Android 공식 런처 권장 셀 크기. 대부분의 기기에서 5열 전후로 계산되어 일반 위젯 크기와 잘 맞음
- **동적 그리드 런타임 계산**: `onGloballyPositioned`로 실제 위젯 영역 크기를 측정 후 `floor(area / 70dp)`로 계산 → 태블릿·폴더블 기기에서 자동 최적화
- **`rememberUpdatedState` 패턴**: 드래그 중 `cellWidthPx`, `placement.column` 등이 변경되더라도 `pointerInput` 블록을 재시작하지 않으면서 최신 값을 참조. 드래그 도중 composable 재구성으로 인한 제스처 취소 방지
- **ViewModel에서의 addWidget 위치 탐색**: UI 레이어가 아닌 ViewModel에서 `findAvailablePosition` 수행. 기본 5×10 그리드 기준 탐색으로 실제 화면 크기와 약간 차이나도 유효한 위치 반환. 화면 밖 배치는 스크롤 없이 표시 불가하므로 추후 스크롤 지원 시 개선 가능
- **3단계 마이그레이션**: Step 13(single int) → Step 14(packed CSV) → 현재(slots CSV) → 신규(JSON) 순서로 레거시 키 존재 여부를 계단식으로 확인. 신규 키(`widget_placements`)가 있으면 즉시 return하여 중복 마이그레이션 방지
- **드래그 vs 리사이즈 핸들 충돌**: 위젯 전체 영역에 드래그 핸들, 우하단 32dp Box에 리사이즈 핸들 배치. Compose 내부 제스처 처리에서 `change.consume()`이 있는 내부 composable이 우선권을 가지므로 리사이즈 핸들 드래그가 위젯 이동을 방해하지 않음

---

## [2026-03-10] refactor(preset-market): GoogleSignInHandler 하드코딩 문자열 strings.xml로 추출

### 목표

- `GoogleSignInHandler.kt`에 하드코딩된 에러 메시지 2개를 strings.xml로 관리
- 한국어(`values`) · 영어(`values-en`) 양쪽에 모두 추가
- `context.getString` 대신 `stringResource`를 사용해 Compose 관용적 방식으로 통일

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/preset-market/.../res/values/strings.xml` | `preset_market_unsupported_auth`, `preset_market_sign_in_error` 추가 |
| `feature/preset-market/.../res/values-en/strings.xml` | 동일 키 영문 번역 추가 |
| `feature/preset-market/.../ui/GoogleSignInHandler.kt` | 하드코딩 문자열 제거, `stringResource`로 컴포저블 스코프에서 캡처 후 LaunchedEffect 내에서 `.format()` 사용 |

### 검증 결과

- 별도 빌드 실행 없음 (문자열 리소스 + import 변경만)

### 설계 결정 및 근거

- **`stringResource` vs `context.getString`**: `LaunchedEffect` 내부는 코루틴 스코프이므로 `@Composable` 함수 호출 불가. 포맷 문자열(`%s` 포함)은 컴포저블 스코프에서 `stringResource`로 미리 캡처하고, 코루틴 안에서 Kotlin `.format(e.message)`으로 인자를 채우는 방식 채택 → `context` 의존 없이 순수 Compose 리소스 API만 사용

---

## [2026-03-10] feat(download/upload): 취소 확인 다이얼로그 + 일시정지 + 알림 정리

### 목표

- 마켓 프리셋 다운로드 중 취소 시 확인 다이얼로그 표시 및 로컬 고아 파일 삭제
- 다이얼로그 표시 동안 실제 다운로드/업로드 일시정지 구현
- 바깥 클릭으로 다이얼로그 닫기 비활성화
- 업로드 취소 X 버튼도 동일하게 확인 다이얼로그 경유하도록 수정
- 다운로드 취소 후 진행 중이던 알림(notification) 제거

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/preset-market/.../download/DownloadProgressTracker.kt` | `pause()` / `resume()` / `awaitResume()` 추가, `requestCancellation()` 시 isPaused 해제, `clear()` 시 모든 상태 초기화 |
| `feature/preset-market/.../PresetDetailContract.kt` | `PauseDownload` / `ResumeDownload` / `CancelDownload` intent 추가, `StopDownloadService` side effect 추가 |
| `feature/preset-market/.../PresetDetailViewModel.kt` | `pauseDownload` → `tracker.pause()`, `resumeDownload` → `tracker.resume()`, `cancelDownload()` → `requestCancellation()` + state 초기화 + `StopDownloadService` effect 전송 |
| `feature/preset-market/.../ui/PresetDetailScreen.kt` | `onStopDownloadService` 콜백 추가, `showCancelDownloadDialog` 상태 추가, 다운로드 중 버튼 클릭 → 다이얼로그 표시, 다이얼로그 open 시 `LaunchedEffect`로 PauseDownload, "계속" → ResumeDownload, "취소" → CancelDownload, `onDismissRequest = {}` |
| `app/.../service/PresetDownloadService.kt` | `collect` 블록 첫 줄에 `awaitResume()` 추가, `onDestroy()` 취소 시 `manager.cancel(NOTIFICATION_ID)` + `presetDir.deleteRecursively()` + `progressTracker.clear()` |
| `app/.../MainActivity.kt` | `onStopDownloadService = { stopService(...) }` 콜백 연결 |
| `feature/preset-market/.../res/values/strings.xml` | `preset_market_cancel_download_title` / `_message` / `_resume` 추가 |
| `feature/settings/.../upload/UploadProgressTracker.kt` | `pause()` / `resume()` / `awaitResume()` 추가 |
| `feature/settings/.../SettingsContract.kt` | `PauseUpload` / `ResumeUpload` intent 추가 |
| `feature/settings/.../SettingsViewModel.kt` | `PauseUpload` → `tracker.pause()`, `ResumeUpload` → `tracker.resume()` 핸들러 추가 |
| `feature/settings/.../ui/PresetSettingsContent.kt` | `PresetItemCard`에 `onPauseUpload` / `onResumeUpload` 파라미터 추가, X 버튼 → `showCancelUploadDialog = true`, 다이얼로그 open 시 `LaunchedEffect`로 pause, "계속" → resume, `onDismissRequest = {}` |
| `feature/settings/.../res/values/strings.xml` | `upload_cancel_title` / `upload_cancel_message` / `upload_cancel_resume` 추가 |

### 검증 결과

- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**
- `./gradlew :feature:settings:compileDebugKotlin` → **BUILD SUCCESSFUL** (settings 파일 잠금 문제는 Android Studio 종료 후 해소)

### 설계 결정 및 근거

**1. 파일 단위 일시정지 (`awaitResume()`)**

HTTP 스트림 중간을 멈추는 것은 불가능하므로, 각 파일 다운로드/업로드 완료 후 `collect` 블록 진입 시 `awaitResume()`로 대기. 파일 경계에서 멈추며 파일 1개 미만의 오차가 발생하지만 UX상 충분함.

**2. `requestCancellation()` 시 isPaused 강제 해제**

취소 요청 후 `stopService()` → `scope.cancel()`이 호출되는데, 코루틴이 `awaitResume()` 에서 일시정지 중이면 `CancellationException`으로 정상 취소됨. 그러나 명시적으로 `isPaused = false`를 설정해 coroutine이 suspension point를 통과할 수 있도록 보장.

**3. 취소 시 알림 명시적 제거**

포그라운드 서비스가 외부 `stopService()`로 종료될 때 진행 중이던 알림이 자동 제거되지 않는 경우가 있어, `onDestroy()`에서 `NotificationManager.cancel(NOTIFICATION_ID)`를 명시적으로 호출.

**4. `onDismissRequest = {}`로 바깥 클릭 dismiss 차단**

다이얼로그가 열린 상태에서 바깥 클릭으로 닫히면 일시정지만 걸린 채 다운로드가 재개되지 않는 문제 방지.

---

## [2026-03-10] feat(upload): 프리셋 업로드 취소 기능 + Firebase Cloud Functions 서버 정리

### 목표

- 업로드 진행 중 사용자가 취소할 수 있도록 X 버튼 추가
- 취소 시 포그라운드 서비스 중단 및 UI 상태 초기화
- 취소로 인해 Firebase Storage에 남는 고아(orphan) 파일을 서버에서 자동 정리

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `feature/settings/.../SettingsContract.kt` | `CancelUpload` intent 추가, `StopUploadService` side effect 추가 |
| `feature/settings/.../SettingsViewModel.kt` | `cancelUpload()` 메서드 추가 — `progressTracker.clear()`, state 초기화, `StopUploadService` 전송 |
| `feature/settings/.../ui/PresetSettingsContent.kt` | `PresetItemCard`에 `onCancelUpload` 파라미터 추가, 업로드 영역을 `Row(프로그래스 Column + X IconButton)` 구조로 변경, `Icons.Default.Close` import |
| `app/.../MainActivity.kt` | `StopUploadService` side effect 핸들러 추가 (`stopService`) |
| `app/.../service/PresetUploadService.kt` | `onDestroy()`에 `progressTracker.clear()` 추가 (방어적 정리) |
| `firebase.json` | Firebase 프로젝트 설정 파일 신규 생성 |
| `.firebaserc` | Firebase 프로젝트 ID 연결 (`stream-launcher-c486c`) |
| `functions/package.json` | Node.js 22, firebase-admin ^13, firebase-functions ^6.3.2 |
| `functions/tsconfig.json` | TypeScript Cloud Functions 컴파일 설정 |
| `functions/src/index.ts` | `onPresetDeleted` (Firestore 삭제 트리거), `cleanupOrphanedPresetImages` (매일 KST 03:00 스케줄) |
| `.gitignore` | `functions/lib`, `functions/node_modules` 추가 |

### 검증 결과

- `./gradlew assembleDebug --no-daemon` → **BUILD SUCCESSFUL**
- `./gradlew test --no-daemon` → **BUILD SUCCESSFUL** (실패 0건)
- `firebase deploy --only functions` → `cleanupOrphanedPresetImages` 배포 성공, `onPresetDeleted` IAM 권한 부여 후 재배포 필요

### 설계 결정 및 근거

**1. 취소 시 클라이언트 롤백 대신 서버 자동 정리 채택**

업로드 취소 시 이미 Storage에 올라간 파일을 클라이언트에서 직접 삭제하는 방식은 롤백 도중 재취소, 네트워크 실패 등 엣지 케이스가 많아 복잡도가 높음. 대신 서버 함수 2개로 분리:
- `onPresetDeleted`: Firestore 문서 삭제 이벤트로 Storage 파일 즉시 정리 (정상 삭제 경로)
- `cleanupOrphanedPresetImages`: 업로드 취소로 Firestore 문서 없이 Storage에만 남은 고아 파일을 매일 03시 자동 삭제. 1시간 유예를 두어 진행 중인 업로드는 보호

**2. 고아 파일 유예 시간 1시간**

정상 업로드 완료 시간 대비 충분한 여유를 두되, 불필요한 Storage 비용이 오래 누적되지 않도록 1시간으로 설정.

**3. Node.js 22 선택**

Node.js 20은 2026-04-30 deprecated 예정. 22는 현재 LTS이며 2027년까지 지원.

---

## [2026-03-10] feat(preset-market): 프리셋 다운로드 중복 방지 — 다운로드 완료 상태 표시

### 목표

- `PresetDetailScreen`에서 이미 로컬에 저장된 마켓 프리셋을 재다운로드할 수 없도록 중복 방지
- 다운로드/업로드 완료 후 해당 프리셋에 마켓 ID(`marketPresetId`)를 연결하여 로컬 DB에 영속화
- 화면 재진입 시에도 "다운로드완료" 상태 유지, 로컬 삭제 시 재다운로드 허용

### 변경사항

| 파일 | 변경 내용 |
|------|-----------|
| `core/domain/.../model/preset/Preset.kt` | `marketPresetId: String? = null` 필드 추가 |
| `core/data/.../room/preset/PresetEntity.kt` | `marketPresetId` 컬럼 추가, `toDomain()`/`toEntity()` 매핑 반영 |
| `core/data/.../room/preset/PresetDao.kt` | `existsByMarketPresetId()`, `updateMarketPresetId(): Int` 쿼리 추가 (non-suspend) |
| `core/data/.../room/AppDatabase.kt` | `version = 3`, `MIGRATION_2_3` 추가 (`ALTER TABLE presets ADD COLUMN marketPresetId TEXT DEFAULT NULL`) |
| `core/data/.../di/DatabaseModule.kt` | `addMigrations(MIGRATION_1_2, MIGRATION_2_3)` 추가 |
| `core/domain/.../repository/PresetRepository.kt` | `isDownloadedByMarketId()`, `updateMarketPresetId(): Int` 인터페이스 추가 |
| `core/data/.../repository/PresetRepositoryImpl.kt` | 두 메서드 구현 (`withContext(IO)` 래핑) |
| `core/data/.../usecase/DownloadMarketPresetUseCase.kt` | `invoke()` + `downloadWithProgress()` 두 곳에서 `marketPresetId = marketPreset.id` 포함 |
| `feature/settings/.../SettingsViewModel.kt` | `PresetRepository` 주입, `UUID.randomUUID()` 생성, `MarketPreset(id = marketPresetId, ...)` 설정, 업로드 완료 시 `updateMarketPresetId()` 호출 |
| `feature/preset-market/.../PresetDetailContract.kt` | `isAlreadyDownloaded: Boolean = false` 상태 추가 |
| `feature/preset-market/.../PresetDetailViewModel.kt` | `PresetRepository` 주입, `loadPreset()` 성공 시 `isDownloadedByMarketId()` 체크, 다운로드 완료 시 `isAlreadyDownloaded = true` 업데이트 |
| `feature/preset-market/.../ui/PresetDetailScreen.kt` | `isAlreadyDownloaded` 분기 — 회색 `outlineVariant` 배경 + "다운로드완료" 텍스트 (클릭 비활성) |
| `feature/preset-market/.../res/values/strings.xml` | `preset_market_already_downloaded = "다운로드완료"` 추가 |
| `feature/preset-market/.../res/values-en/strings.xml` | `preset_market_already_downloaded = "Downloaded"` 추가 |
| `feature/settings/.../SettingsViewModelTest.kt` | `PresetRepository` mock 추가, `makeViewModel()` 파라미터 확장 |

### 검증 결과

- `./gradlew assembleDebug` → **BUILD SUCCESSFUL** (307 tasks)
- `./gradlew test` → **BUILD SUCCESSFUL** (472 tasks, 실패 0건)

### 설계 결정 및 근거

**1. DAO suspend vs non-suspend 선택**

신규 `@Query` 메서드를 처음에 `suspend fun`으로 작성했으나, Room KSP 코드 생성기가 `Continuation<Boolean>` vs `Continuation<? super Boolean>` 타입 충돌 오류를 발생시켰다 (`unexpected jvm signature V`). 기존 DAO 메서드들이 모두 non-suspend 패턴을 사용하고 있어 동일하게 non-suspend로 유지하고, I/O 디스패칭은 `PresetRepositoryImpl`의 `withContext(Dispatchers.IO)` 에서 처리했다.

**2. updateMarketPresetId 반환 타입을 Int로**

Room은 `@Query` UPDATE 문에서 영향받은 행 수를 `Int`로 반환한다. `Unit`(void) 반환 시 KSP가 `V` 서명을 처리하지 못하는 버그가 있어 `Int`로 확정했다. 인터페이스/구현체에서 반환값은 호출부에서 무시한다.

**3. 업로드 시 UUID 생성 위치**

업로드 ID를 Firestore에서 자동 생성하는 대신 클라이언트(SettingsViewModel)에서 `UUID.randomUUID()`로 생성하여 `MarketPreset(id = marketPresetId)`에 설정한다. 이렇게 하면 업로드 시작 시점에 ID를 알 수 있어 업로드 완료 후 로컬 Preset과 즉시 연결(`updateMarketPresetId`) 가능하다.

**4. PresetDetailViewModel에 PresetRepository 직접 주입**

`feature:preset-market`은 이미 `core:data`에 의존하고 있어 `PresetRepository` 주입이 가능하다. 별도 UseCase를 만들지 않고 repository를 직접 주입하여 `isDownloadedByMarketId()` 호출 코드를 최소화했다.

---

## [2026-03-03] arch: Android Service 위치 및 `:core:service` 모듈 분리 고찰

### 배경

프리셋 다운로드 포그라운드 서비스 전환 작업 중, `PresetDetailViewModel`에 주입된 `downloadMarketPresetUseCase`가 실제로는 사용되지 않는다는 경고가 발생했다. 이를 계기로 Android Service를 어느 모듈에 배치하는 것이 클린 아키텍처에 부합하는지 검토했다.

### 논의 요약

#### Q1. 도메인 유즈케이스에서 서비스를 호출하는 구조는 가능한가?

`StartDownloadServiceUseCase`를 `:core:domain`에 두려면 `context.startForegroundService()`가 필요하다. 그러나 `:core:domain`은 **순수 JVM 모듈**로 Android 프레임워크 의존성을 가질 수 없으므로 불가능하다.

`:core:data`에 두면 `@ApplicationContext`는 쓸 수 있으나, 서비스 클래스(`PresetDownloadService`)가 `:app`에 있기 때문에 **순환 의존**이 발생한다.

```
:app ──▶ :core:data ──▶ :app (PresetDownloadService)  ← 순환 불가
```

의존성 역전(DIP)으로 우회할 수 있으나(`DownloadServiceLauncher` 인터페이스 + `:app` 구현체 주입), 현재 SideEffect 패턴보다 복잡도만 높아진다.

**결론:** 현재 구조(ViewModel → SideEffect → Activity → `startForegroundService()`)가 더 적절하다. `startForegroundService()`는 비즈니스 로직이 아닌 OS 명령이므로 UseCase가 아닌 UI 레이어(Activity)에서 처리하는 것이 클린 아키텍처 원칙에 부합한다.

#### Q2. 서비스 전용 `:core:service` 모듈로 분리하는 방식은 어떤가?

**현재 구조의 문제점:**

`:app`의 `PresetDownloadService`가 `DownloadDataHolder`, `DownloadProgressTracker`를 참조하기 위해 `:feature:preset-market` 내부 구현에 의존하고 있다. UI 목적이 아닌 인프라 목적의 참조로, 책임이 혼재되어 있다.

**제안 구조 (`core:service`):**

```
:core:service
  ├── AndroidManifest.xml       (서비스 선언 → app manifest 자동 merge)
  ├── PresetUploadService
  ├── PresetDownloadService
  ├── upload/UploadDataHolder, UploadProgressTracker
  └── download/DownloadDataHolder, DownloadProgressTracker
```

의존성 방향:

```
:core:domain ◀── :core:data ◀── :core:service
                                      ▲            ▲
                          :feature:settings   :feature:preset-market
                                               ▲
                                             :app
```

모든 의존성이 단방향이며 순환이 없다. Hilt `@AndroidEntryPoint`는 라이브러리 모듈에서도 지원되고, 라이브러리 모듈의 `AndroidManifest.xml`은 `:app` manifest에 자동 merge된다.

### 설계 결정 및 근거

| 항목 | 결정 |
|------|------|
| Android Service의 레이어 위치 | 프레임워크 진입점(Frameworks & Drivers) — `:app` 또는 전용 모듈 |
| 서비스 기동 책임 | UseCase가 아닌 Activity (SideEffect 패턴 유지) |
| `:core:service` 모듈 분리 | 현재는 보류 — 서비스가 2개로 고정적이고 리팩토링 비용 대비 이점이 크지 않음 |
| 분리 시점 기준 | 서비스 종류가 늘어나거나 여러 feature 모듈이 DataHolder/ProgressTracker를 공유해야 할 때 도입 |

### 미적용 사항 (기술 부채 기록)

- `UploadDataHolder`, `UploadProgressTracker` → 현재 `:feature:settings/upload/`에 위치
- `DownloadDataHolder`, `DownloadProgressTracker` → 현재 `:feature:preset-market/download/`에 위치
- 서비스 추가 시 `:core:service` 모듈 신설 후 위 4개 클래스 + 서비스 2개를 이전하는 것이 권장 방향

---

## [2026-03-03] feat(download): 프리셋 다운로드 포그라운드 서비스 전환

### 목표

기존 `PresetDetailViewModel`에서 `viewModelScope.launch`로 직접 실행되던 다운로드 로직을 **포그라운드 서비스**로 이전한다. 단순 boolean 플래그(`isDownloading`)로만 표시되던 진행 상황을 `DownloadProgress`로 세분화하고, 버튼에 **왼→오 프로그레스 fill 애니메이션**을 추가하여 다운로드 중에도 앱을 백그라운드로 보낼 수 있도록 안정성을 높인다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `core:domain` | `model/preset/DownloadProgress.kt` | 신규 — `UploadProgress`와 동일 구조: `presetName`, `currentStep`, `totalSteps`, `isCompleted`, `error`, `percentage` computed property |
| 2 | `core:data` | `usecase/DownloadMarketPresetUseCase.kt` | `downloadWithProgress(marketPreset): Flow<DownloadProgress>` 추가 — 이미지 URL 수 + 2(설정 적용·카운트 증가)를 `totalSteps`로, 각 이미지 완료마다 `emit()`, 부분 실패 시 `presetDir.deleteRecursively()` 후 에러 emit, `UnknownHostException`/`IOException`/`Exception` 개별 catch |
| 3 | `feature:preset-market` | `download/DownloadDataHolder.kt` | 신규 — `@Singleton`, `pendingPreset: MarketPreset?`, `clear()` |
| 4 | `feature:preset-market` | `download/DownloadProgressTracker.kt` | 신규 — `@Singleton`, `StateFlow<DownloadProgress?>`, `update()`, `clear()` |
| 5 | `feature:preset-market` | `PresetDetailContract.kt` | `PresetDetailState`에 `downloadProgress: DownloadProgress? = null` 추가; `PresetDetailSideEffect`에 `StartDownloadService(presetName)` 추가 |
| 6 | `feature:preset-market` | `PresetDetailViewModel.kt` | `DownloadDataHolder`, `DownloadProgressTracker` 생성자 주입 추가; `init`에 `downloadProgressTracker.progress` collect → state 업데이트 + `DownloadComplete`/`ShowError` 사이드이펙트; `downloadPreset()`에서 직접 실행 대신 중복 방지 조기 리턴 → `DownloadDataHolder` 저장 → `StartDownloadService` 사이드이펙트 발행으로 전환 |
| 7 | `app` | `service/PresetDownloadService.kt` | 신규 — `@AndroidEntryPoint Service`, `PresetUploadService`와 동일 패턴; `NOTIFICATION_ID=9003`, `NOTIFICATION_RESULT_ID=9004`, `CHANNEL_ID="preset_download"`; `downloadJob: Job?`으로 중복 실행 방지(`isActive` 검사) |
| 8 | `app` | `StreamLauncherApplication.kt` | `createNotificationChannels()`에 `"preset_download"` 채널(`IMPORTANCE_LOW`) 추가 |
| 9 | `app` | `AndroidManifest.xml` | `PresetDownloadService` 서비스 등록 (`foregroundServiceType="dataSync"`, `exported="false"`) |
| 10 | `app` | `MainActivity.kt` | `PresetDownloadService` import 추가; `PresetDetailScreen` 호출에 `onStartDownloadService` 콜백 추가 → `startForegroundService()` 호출 |
| 11 | `feature:preset-market` | `ui/PresetDetailScreen.kt` | `onStartDownloadService: (String) -> Unit` 파라미터 추가; `StartDownloadService` 사이드이펙트 처리 추가; 다운로드 버튼을 커스텀 `Box`로 교체 — `animateFloatAsState(tween(300))`으로 primary 색상이 `fillMaxWidth(fraction=…)`로 왼→오 채워지는 효과, 다운로드 중 클릭 비활성화(연타 방지), 진행 중 `XX%` 텍스트 / 대기 중 Download 아이콘+텍스트 표시 |

### 검증 결과

- `assembleDebug` BUILD SUCCESSFUL (스마트 캐스트 오류 1건 수정: `progress.error ?: "…"` 패턴 적용)
- `./gradlew test` BUILD SUCCESSFUL (실패 0건, 회귀 없음)

### 설계 결정 및 근거

- **`DownloadDataHolder` 싱글톤 메모리 전달**: `MarketPreset`은 다수의 String URL 필드를 갖고 있어 `Intent` extras(1MB 제한) 직렬화 위험이 있다. 업로드와 동일한 패턴(`UploadDataHolder`)으로 메모리 전달을 택해 일관성을 확보한다.
- **부분 실패 시 `deleteRecursively()` cleanup**: 이미지 1장이라도 실패하면 이미 내려받은 파일들을 즉시 삭제해 깨진 상태의 프리셋이 로컬에 남지 않도록 방지한다.
- **`downloadJob?.isActive` 중복 방지**: `onStartCommand`가 여러 번 호출되더라도 이미 활성 Job이 있으면 무시한다. ViewModel의 `downloadProgressTracker.progress.value != null` 조기 리턴과 이중으로 중복 실행을 차단한다.
- **스마트 캐스트 오류 우회**: `DownloadProgress.error`는 다른 모듈(`:core:domain`)의 public API property여서 Kotlin이 null 검사 후에도 스마트 캐스트를 거부한다. `?: "…"` elvis 연산자로 nullable을 non-null로 강제 변환하여 해결.
- **왼→오 fill 버튼 구현**: `Button` 대신 `Box + Surface(fillMaxWidth(fraction=animatedFraction))` 레이어 구조를 사용해 표준 Compose 컴포넌트의 제약 없이 자유로운 프로그레스 fill 애니메이션을 구현했다. `clickable` modifier를 `then(if(!isDownloading) Modifier.clickable{…} else Modifier)`로 조건부 적용해 진행 중 연타를 방지한다.

---

## [2026-03-02] feat(upload): 프리셋 업로드 포그라운드 서비스 전환

### 목표

기존 `SettingsViewModel`에서 `viewModelScope.launch`로 직접 실행되던 업로드 로직을 **포그라운드 서비스**로 이전한다. 업로드 중 모달 AlertDialog로 화면을 차단하던 UX를 제거하고, **카드 레벨 LinearProgressIndicator** + **Notification 프로그레스바**로 진행 상황을 표시하여 업로드 중에도 다른 화면을 탐색할 수 있도록 한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `core:domain` | `model/preset/UploadProgress.kt` | 신규 — `presetName`, `currentStep`, `totalSteps`, `isCompleted`, `error`, `percentage` 보유 데이터 클래스 |
| 2 | `core:domain` | `upload/UploadProgressTracker.kt` | 신규 — `@Singleton`, `MutableStateFlow<UploadProgress?>` 보유, `update()` / `clear()` 메서드 |
| 3 | `core:domain` | `upload/UploadDataHolder.kt` | 신규 — `@Singleton`, `pendingPreset: MarketPreset?` / `pendingPreviewUris` 보유, `clear()` 메서드 (Intent 크기 제한 회피용) |
| 4 | `core:domain` | `usecase/UploadPresetToMarketUseCase.kt` | `uploadWithProgress(preset, previewUris): Flow<UploadProgress>` 추가 — 로컬 이미지 수 기반 `totalSteps` 계산, 각 이미지 업로드 후 `emit()`, 네트워크 예외 사용자 친화적 메시지 변환 |
| 5 | `app` | `AndroidManifest.xml` | 권한 추가: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`; `PresetUploadService` 서비스 등록 (`foregroundServiceType="dataSync"`) |
| 6 | `app` | `StreamLauncherApplication.kt` | `createNotificationChannels()` 추가 — `"preset_upload"` 채널, `IMPORTANCE_LOW` (무음) |
| 7 | `app` | `service/PresetUploadService.kt` | 신규 — `@AndroidEntryPoint Service`, `UploadDataHolder`에서 preset 읽기, `ServiceCompat.startForeground()` (API 29 분기), `collect`로 진행 Notification 갱신, 완료/실패 별도 Notification, `START_NOT_STICKY` |
| 8 | `feature:settings` | `SettingsContract.kt` | `SettingsState.isUploading` → `uploadProgress: UploadProgress?`로 교체; `SettingsSideEffect`에 `StartUploadService(presetName)`, `UploadStarted(presetName)` 추가 |
| 9 | `feature:settings` | `SettingsViewModel.kt` | `UploadProgressTracker` / `UploadDataHolder` 주입 추가; `init`에 progress collect → `UploadSuccess`/`UploadError` 사이드이펙트 발행; `uploadPreset()`에서 직접 실행 대신 DataHolder 저장 + `StartUploadService` 사이드이펙트 발행으로 위임 |
| 10 | `feature:settings` | `ui/PresetSettingsContent.kt` | 업로드 중 모달 AlertDialog 제거; `PresetItemCard`에 `uploadProgress: UploadProgress?` 파라미터 추가, 업로드 중 `LinearProgressIndicator` + `%` 텍스트 표시, 카드 배경 강조; 업로드 중 모든 공유 버튼 비활성화; `POST_NOTIFICATIONS` 런타임 권한 요청 (Android 13+) |
| 11 | `feature:settings` | `ui/SettingsDetailScreen.kt` | `onShowSnackbar: (String) -> Unit` 파라미터 추가 → `PresetSettingsContent`에 전달 |
| 12 | `app` | `MainActivity.kt` | `StartUploadService` → `startForegroundService()` 처리; `UploadStarted` → 스낵바 "N을 마켓에 업로드합니다"; `SettingsDetailScreen`에 `onShowSnackbar` 전달 |
| 13 | `feature:settings` | `SettingsViewModelTest.kt` | `UploadProgressTracker` / `UploadDataHolder` mock 추가 (실제 인스턴스 사용), `makeViewModel()` 파라미터 확장 |

### 검증 결과

- `assembleDebug` BUILD SUCCESSFUL
- `./gradlew test` BUILD SUCCESSFUL (실패 0건, 회귀 없음)

### 설계 결정 및 근거

- **`UploadDataHolder` 싱글톤 메모리 전달**: Firebase `StorageReference.putFile()`은 Uri를 직접 처리하므로 `Parcelable` 변환 없이 메모리 객체를 서비스로 전달하는 것이 가장 단순하다. `Intent` extras는 1MB 제한이 있어 `MarketPreset`(로컬 URI 다수)을 직렬화하면 초과 위험이 있다.
- **`START_NOT_STICKY`**: 프로세스 종료 시 `UploadDataHolder`/`UploadProgressTracker`가 초기화되므로 서비스 자동 재시작이 무의미하다. 대신 `onStartCommand`에서 `pendingPreset == null` 방어 로직으로 graceful 종료 처리.
- **`ServiceCompat.startForeground()` + API 29 분기**: `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`는 API 29 상수이므로 조건 분기로 하위 호환성 확보. minSdk 28에서 컴파일 오류 방지.
- **progress tracker에서 UploadSuccess/UploadError 발행**: 서비스가 종료된 후에도 ViewModel의 collect 흐름이 상태 변화를 감지하므로 서비스↔ViewModel 간 직접 통신 없이 단방향 데이터 흐름 유지.
- **스마트 캐스트 오류 해결**: `UploadProgress.error`는 다른 모듈의 public property이므로 Kotlin이 null 체크 후에도 스마트 캐스트를 거부한다. 로컬 변수(`val errorMsg = progress.error`)에 대입 후 검사하거나 `?.let { }` 체이닝으로 우회.

---

## [2026-03-02] feat(preset-market): Top 10 카드에 순위 번호(#1, #2…) 표시

### 목표

`MarketPresetCard`의 프리셋명 앞에 HorizontalPager 페이지 인덱스 기반 순위 텍스트(`#1`, `#2` 등)를 표시하여 Top 10 리스트임을 시각적으로 강조한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `feature:preset-market` | `MarketPresetCard.kt` | `rank: Int? = null` 파라미터 추가 |
| 2 | `feature:preset-market` | `MarketPresetCard.kt` | preset.name 텍스트를 `Row`로 감싸고, `rank != null`일 때 `#$rank` 텍스트를 앞에 삽입 (`titleLarge` + `FontWeight.Bold`, White) |
| 3 | `feature:preset-market` | `MarketHomeScreen.kt` | `HorizontalPager` 블록에서 `rank = page + 1` 전달 |

### 검증 결과

- 컴파일 오류 없음 (파라미터 기본값 `null`로 기존 호출부 호환성 유지)
- `rank = null` 시 순위 텍스트 미렌더링 — 하위 호환 OK

### 설계 결정 및 근거

- **`rank: Int? = null` 기본값**: 호출부가 rank를 넘기지 않아도 기존 동작 유지. 추후 다른 화면에서 순위 없이 재사용 가능.
- **`titleLarge` 크기 선택**: 요청 스펙("titleMedium보다 조금 더 크게")에 맞게 Material3 타입 스케일에서 바로 한 단계 위인 `titleLarge` 적용.
- **`Row` + `spacedBy(6.dp)`**: 순위와 프리셋명 사이 적절한 간격을 유지하면서 name의 `TextOverflow.Ellipsis`가 여전히 동작하도록 배치.

---

## [2026-03-02] fix(ads): AdmobBanner 메모리 누수 수정 및 PresetDetailScreen 광고 배치

### 목표

LeakCanary가 감지한 AdMob → 소멸된 `MainActivity` 메모리 누수를 수정하고, 프리셋 상세 화면에 배너 광고를 추가한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `feature:preset-market` | `AdmobBanner.kt` | `AndroidView`에 `onRelease = { adView -> adView.destroy() }` 콜백 추가 — 컴포저블 해제 시 AdView 리소스 명시적 반환 |
| 2 | `feature:preset-market` | `PresetDetailScreen.kt` | 프리뷰 이미지 섹션 하단, 상세 정보 Column 위에 `AdmobBanner(modifier = Modifier.fillMaxWidth())` 삽입 |

### 검증 결과

- 컴파일 오류 없음 (같은 패키지 내 참조, 별도 import 불필요)
- LeakCanary 누수 원인 제거: `onRelease` 콜백으로 AdView → Activity 참조 해제 보장

### 설계 결정 및 근거

- **`onRelease` 콜백 필수**: `AndroidView`의 `factory`로 생성된 `AdView`는 Activity context를 내부에 보유한다. `onRelease`가 없으면 컴포저블이 composition을 떠나도 AdMob SDK 내부 로더(`nonagon.load.as`)가 `AdView` → 소멸된 Activity를 계속 참조하여 36.6 kB 누수가 발생한다.
- **광고 위치 선정**: 프리뷰 이미지 `if` 블록 바깥에 배치하여 이미지 유무와 관계없이 광고가 항상 표시되도록 했다. 프리뷰 → 광고 → 상세정보 순서로 자연스러운 콘텐츠 흐름을 유지한다.

---

## [2026-03-02] fix(system-bar): 불투명 화면에서 시스템 바 색상 적응형 전환 구현

### 목표

프리셋 마켓·설정 상세처럼 불투명 Material3 배경을 가진 화면에서 시스템 바 아이콘 색상이 월페이퍼 밝기가 아닌 시스템 다크/라이트 모드 기준으로 전환되도록 한다. 런처 홈 화면에서는 기존 월페이퍼 밝기 기반 동작을 그대로 유지한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `App` | `MainActivity.kt` | `import android.content.res.Configuration`, `DisposableEffect`, `NavController` 임포트 추가 |
| 2 | `App` | `MainActivity.kt` | 클래스 필드 `currentRoute = mutableStateOf<String?>(null)` 추가 (Compose 외부 리스너에서도 접근 가능) |
| 3 | `App` | `MainActivity.kt` | `isOpaqueRoute(route)` 헬퍼 추가 — `settings_detail/` 또는 `preset_market` prefix 판별 |
| 4 | `App` | `MainActivity.kt` | `updateSystemBarStyle(isDark: Boolean)` → `updateSystemBarStyle(isWallpaperDark: Boolean, route: String?)` 리팩터링: 불투명 화면은 `UI_MODE_NIGHT_*` 기준, 런처 화면은 월페이퍼 기준으로 분기 |
| 5 | `App` | `MainActivity.kt` | `setContent` 내 `DisposableEffect(navController)` 추가 — 목적지 변경 시 `currentRoute.value` 갱신 + `updateSystemBarStyle` 재호출 |
| 6 | `App` | `MainActivity.kt` | 기존 호출 3곳 수정: `onCreate`(`null`), `wallpaperColorsChangedListener`(`currentRoute.value`), `onResume`(`currentRoute.value`) |

### 검증 결과

- `./gradlew :app:assembleDebug` BUILD SUCCESSFUL (227 tasks, 41 executed)
- 컴파일 오류 없음

### 설계 결정 및 근거

- **`currentRoute`를 클래스 필드로 선언**: `wallpaperColorsChangedListener`와 `onResume`은 Compose 컨텍스트 밖에서 실행되므로 `remember`로 생성한 로컬 Compose 상태에 접근할 수 없다. `mutableStateOf`를 클래스 필드로 두면 Compose 안(DisposableEffect)에서 쓰고 Compose 밖(리스너)에서 읽는 것이 모두 가능하다.
- **`DisposableEffect(navController)`로 리스너 관리**: 컴포저블 생명주기에 맞춰 리스너를 자동 등록/해제하므로 메모리 누수 없이 안전하다.
- **불투명 화면 prefix 기반 판별**: `SettingsRoute.DETAIL = "settings_detail/{menu}"`, `MarketRoute.HOME/SEARCH/DETAIL`이 모두 `"preset_market"` prefix를 공유하므로 `startsWith`만으로 세 화면을 한 번에 포괄한다. 새 불투명 화면이 추가될 경우 `isOpaqueRoute`에 조건만 추가하면 된다.
- **`useDarkIcons` 로직**: 라이트모드(`UI_MODE_NIGHT_YES` 아님)에서는 배경이 밝으므로 검은 아이콘(dark icons)이 필요 → `nightMode != UI_MODE_NIGHT_YES`가 `true`일 때 `SystemBarStyle.light`(dark icons) 적용.

---

## [2026-03-02] feat(preset-market): 검색 화면 SharedElement 전환 애니메이션 + 리네이밍

### 목표

프리셋 마켓 홈 화면의 검색바를 탭하면 `MarketSearchScreen`으로 전환될 때, Shared Element Transition으로 검색바가 자연스럽게 상단으로 모핑되는 애니메이션을 적용한다. `MarketSearchResultScreen` → `MarketSearchScreen` 리네이밍 병행.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Feature(PresetMarket)` | `navigation/MarketRoute.kt` | SEARCH 경로 `{query}` → `?query={query}` (optional), `search()` 기본값 추가 |
| 2 | `Feature(PresetMarket)` | `PresetMarketContract.kt` | `NavigateToSearch` intent/sideeffect: `data class(query)` → `data object` |
| 3 | `Feature(PresetMarket)` | `PresetMarketViewModel.kt` | `NavigateToSearch` 핸들러에서 query 인자 제거 |
| 4 | `Feature(PresetMarket)` | `ui/MarketHomeScreen.kt` | `SharedTransitionScope`/`AnimatedVisibilityScope` 파라미터 추가; `OutlinedTextField` → 클릭 전용 fake `Surface` SearchBar + `sharedBounds(key="market-search-bar")`; `onNavigateToSearch: () -> Unit`으로 변경 |
| 5 | `Feature(PresetMarket)` | `ui/MarketSearchResultScreen.kt` | **삭제** (MarketSearchScreen.kt로 대체) |
| 6 | `Feature(PresetMarket)` | `ui/MarketSearchScreen.kt` | **신규 생성**: `SharedTransitionScope`/`AnimatedVisibilityScope` 파라미터 추가; TopAppBar 내 `OutlinedTextField`에 `sharedBounds(key="market-search-bar")` 적용; 전환 완료 후 400ms 딜레이 뒤 `FocusRequester`로 자동 포커스; `ImeAction.Search` + `KeyboardActions` 엔터 검색; `LocalSoftwareKeyboardController.hide()` 검색 후 키보드 자동 숨김 |
| 7 | `App` | `MainActivity.kt` | `NavHost`를 `SharedTransitionLayout`으로 래핑, `sharedTransitionScope` 캡처; HOME exitTransition/popEnterTransition → `fadeOut`/`fadeIn`; SEARCH enterTransition/popExitTransition → `fadeIn`/`fadeOut`; `navArgument("query") { defaultValue = "" }` 선언; scope 전달; `MarketSearchResultScreen` → `MarketSearchScreen` import 변경; `NavType`/`navArgument` import 추가 |

### 검증 결과

- `./gradlew assembleDebug` BUILD SUCCESSFUL (227 tasks)
- `./gradlew test` BUILD SUCCESSFUL (실패 0건)

### 설계 결정 및 근거

- **HOME↔SEARCH 전환을 fade로 변경**: Shared Element가 위치를 이동하는 동안 slide 전환이 겹치면 요소가 두 방향으로 동시에 이동해 시각적 충돌이 발생한다. fade는 배경만 크로스페이드되므로 shared element의 모핑 경로를 방해하지 않는다.
- **검색바를 fake Surface로 교체**: 홈 화면의 검색바는 탭하는 즉시 검색 화면으로 전환되므로 실제 텍스트 입력이 불필요하다. `OutlinedTextField`를 유지하면 포커스 획득 시 키보드가 올라오는 부작용이 생긴다. 클릭 가능한 `Surface`로 교체해 외관은 동일하되 입력 기능은 제거한다.
- **자동 포커스에 400ms 딜레이 적용**: shared element 전환 애니메이션(기본 300ms) 진행 중에 키보드가 올라오면 레이아웃이 흔들린다. 전환 완료를 보장하는 여유 시간(400ms) 후 `focusRequester.requestFocus()`를 호출한다.
- **`keyboardController?.hide()` 조건 밖 배치**: 빈 쿼리일 때도 검색 버튼을 누르면 키보드가 내려가야 자연스럽다. 검색 실행 조건(`isNotBlank`)과 무관하게 항상 키보드를 숨긴다.

---

## [2026-03-02] chore(i18n): 하드코딩 텍스트 strings.xml 추출 및 영문 리소스 정비

### 목표

전체 모듈에 산재한 하드코딩 텍스트를 `strings.xml`로 이동하고, 미흡했던 영문(`values-en`) 리소스를 모듈별로 완전하게 갖춘다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Feature(PresetMarket)` | `res/values/strings.xml` | 신규 7개 추가: `preset_market_download_limit_title/message`, `preset_market_includes_home/feed/drawer/theme/wallpaper` |
| 2 | `Feature(Launcher)` | `res/values/strings.xml` | 신규 2개 추가: `youtube_platform`, `feed_live_text` |
| 3 | `Feature(Settings)` | `res/values/strings.xml` | 신규 4개 추가: `grid_cell_top_left/top_right/bottom_left/bottom_right` |
| 4 | `Feature(PresetMarket)` | `ui/PresetDetailScreen.kt` | `"다운로드 완료…"` → `stringResource(preset_market_download_complete)` (LaunchedEffect 외부 캐싱); `"프리셋 저장 불가"` / `"저장 가능한 개수…"` / `"확인"` → `stringResource`; 포함 설정 칩 레이블 5개 → `stringResource` |
| 5 | `Feature(PresetMarket)` | `ui/MarketPresetListItem.kt` | `"by ${authorDisplayName}"` → `stringResource(preset_market_by, …)`; `R` / `stringResource` import 추가 |
| 6 | `Feature(Launcher)` | `ui/FeedScreen.kt` | `contentDescription = "YouTube"` → `stringResource(youtube_platform)`; `text = "LIVE"` 2곳 → `stringResource(feed_live_text)` |
| 7 | `Feature(Settings)` | `ui/SettingsScreen.kt` | `text = cell.name` → `when(cell)` + `stringResource(grid_cell_*)` 로 한국어 셀 이름 표시 |
| 8 | `Feature(Launcher)` | `res/values-en/strings.xml` | **정리**: settings/notice 오배치 문자열 제거, 신규 `youtube_platform` / `feed_live_text` 추가 |
| 9 | `Feature(Settings)` | `res/values-en/strings.xml` | **신규 생성**: settings, preset, grid_cell, notice 전체 40개 영문 번역 |
| 10 | `Feature(PresetMarket)` | `res/values-en/strings.xml` | **신규 생성**: preset market 전체 42개 영문 번역 |

### 검증 결과

- `./gradlew assembleDebug` BUILD SUCCESSFUL (307 tasks, 0 errors)

### 설계 결정 및 근거

- **launcher `values-en`에 오배치된 settings 문자열 제거**: settings 모듈 분리 이전에 작성된 영문 파일이 정리되지 않아, `feature/launcher/values-en`에 `settings_*` / `notice_*` 문자열이 포함되어 있었다. 각 모듈은 자신의 R 클래스를 사용하므로 해당 번역은 실제로 적용되지 않는 dead code였다. 번역 파일을 각 모듈의 `values-en`으로 이동해 소유권을 명확히 한다.
- **`grid_cell_*` 문자열 신규 추가**: `ImageSettingsContent`에서 `cell.name`이 그대로 노출되어 사용자에게 `TOP_LEFT` 같은 enum 식별자가 보였다. `when(cell)` 분기 + `stringResource`로 "좌상 / 우상 / 좌하 / 우하" 형태로 현지화한다. `GridCell`은 순수 JVM 모듈(`core:domain`)이므로 Android 리소스를 포함할 수 없어 UI 레이어에서 매핑한다.
- **`LaunchedEffect` 내부의 `stringResource` 패턴 통일**: Compose의 `stringResource`는 컴포저블 컨텍스트에서만 호출 가능하므로 코루틴 블록 내부에서 직접 호출 불가. 컴포저블 범위에서 변수로 미리 읽어 클로저로 참조하는 패턴을 프로젝트 전반에 일관 적용한다.

---

## [2026-03-02] feat(preset-market): 로그아웃 확인 다이얼로그 및 로그인 상태 아이콘 구분 추가

### 목표

프리셋 마켓 화면에서 실수로 로그아웃되는 것을 방지하기 위해 로그아웃 전 확인 다이얼로그를 추가하고, 로그인/비로그인 상태를 아이콘으로 시각적으로 구분한다. 또한 로그인 성공 시 스낵바 피드백을 제공한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Feature(PresetMarket)` | `ui/MarketHomeScreen.kt` | `showSignOutDialog` 상태 추가; 로그인 상태 아이콘 클릭 시 즉시 로그아웃 → `AlertDialog` 표시로 변경 |
| 2 | `Feature(PresetMarket)` | `ui/MarketHomeScreen.kt` | 로그아웃 확인 `AlertDialog` 추가: 제목 "로그아웃", 내용 "로그아웃 하시겠습니까?", 버튼 "로그아웃" / "취소" |
| 3 | `Feature(PresetMarket)` | `ui/MarketHomeScreen.kt` | 비로그인 아이콘 `AccountCircle` → `Person`으로 변경해 로그인/비로그인 상태 시각적 구분 |
| 4 | `Feature(PresetMarket)` | `ui/MarketHomeScreen.kt` | `SignInSuccess` 사이드 이펙트 수신 시 "로그인 성공" 스낵바 표시; `stringResource`를 `LaunchedEffect` 외부에서 사전 캐싱 |
| 5 | `Feature(PresetMarket)` | `PresetMarketContract.kt` | `PresetMarketSideEffect`에 `SignInSuccess` 추가 |
| 6 | `Feature(PresetMarket)` | `PresetMarketViewModel.kt` | `signIn()` 성공 핸들러에서 `SignInSuccess` side effect 전송 추가 |
| 7 | `Feature(PresetMarket)` | `res/values/strings.xml` | `preset_market_logout_confirm` ("로그아웃 하시겠습니까?"), `preset_market_sign_in_success` ("로그인 성공") 추가 |

### 검증 결과

- 코드 리뷰 수준 검증 (빌드 미실행)

### 설계 결정 및 근거

- **로그아웃 확인 다이얼로그를 UI 레이어 로컬 상태로 처리**: 로그아웃 확인은 순수 UI 관심사이므로 ViewModel에 intent를 추가하지 않고 `showSignOutDialog` 로컬 상태로 관리. 실제 로그아웃 실행(`SignOut` intent)은 다이얼로그 확인 버튼에서만 발생.
- **아이콘 구분 — `AccountCircle` vs `Person`**: 로그인 상태는 채워진 원형 프로필(`AccountCircle`)로 "계정이 연결됨"을 표현, 비로그인 상태는 윤곽선 형태의 단순 사람 아이콘(`Person`)으로 "미연결"을 표현. material-icons-extended 의존성 없이 기본 아이콘 세트에서 조달.
- **`stringResource`를 `LaunchedEffect` 외부에서 캐싱**: Compose의 `stringResource`는 컴포저블 컨텍스트에서만 호출 가능하므로, 코루틴 내부(LaunchedEffect)에서 직접 호출 불가. 변수로 미리 읽어 코루틴 클로저에서 참조하는 표준 패턴 적용.
- **`SignInSuccess` 전용 side effect 추가**: 기존 `ShowError`(Snackbar)를 공유 메시지로 재사용하는 것보다 의도를 명확히 하고, 향후 로그인 성공 시 추가 처리(예: 화면 갱신, 환영 메시지 변경)로 확장하기 쉽도록 별도 side effect로 분리.

---

## [2026-03-02] fix(settings/preset-market): 프리셋 UI 버그 수정 및 개수 제한 경고 추가

### 목표

프리셋 저장 화면의 3가지 버그를 수정하고, 프리셋 최대 개수(10개) 초과 시 일관된 경고 다이얼로그를 프리셋 설정 화면과 마켓 다운로드 화면 양쪽에 제공한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Feature(Settings)` | `ui/PresetSettingsContent.kt` | `SavePresetDialog`: 배경화면 체크 시 이미지 미선택 상태에서 저장 버튼 비활성화 (`enabled = !saveWallpaper \|\| selectedWallpaperUri != null`) |
| 2 | `Feature(Settings)` | `ui/PresetSettingsContent.kt` | 프리셋 10개 초과 클릭 시 `SettingsIntent.ShowNotice`(공지사항 다이얼로그) 오용 → `showLimitDialog` 로컬 상태로 전환; "저장 가능한 개수를 초과하였습니다" AlertDialog 신규 추가 |
| 3 | `Feature(Settings)` | `ui/PresetSettingsContent.kt` | 스와이프 삭제 취소 시 카드가 복원되지 않는 버그 수정: `rememberSwipeToDismissBoxState(confirmValueChange = { false })` 패턴으로 교체, 기존 `LaunchedEffect` + `reset()` 제거 |
| 4 | `Domain` | `preset_market/PresetDetailContract.kt` | `PresetDetailSideEffect`에 `PresetLimitExceeded` 추가 |
| 5 | `Feature(PresetMarket)` | `preset_market/PresetDetailViewModel.kt` | `GetAllPresetsUseCase` 주입; `downloadPreset()` 실행 전 저장된 프리셋 수 확인 — 10개 이상이면 `PresetLimitExceeded` side effect 발생 후 조기 반환 |
| 6 | `Feature(PresetMarket)` | `preset_market/ui/PresetDetailScreen.kt` | `PresetLimitExceeded` 수신 시 프리셋 설정 화면과 동일한 AlertDialog 표시 (`showLimitDialog` 상태) |

### 검증 결과

- 코드 리뷰 수준 검증 (빌드 미실행)

### 설계 결정 및 근거

- **`confirmValueChange = { false }` 패턴**: `SwipeToDismissBox`의 기본 `confirmValueChange`는 `true`를 반환해 스와이프 완료 시 dismiss 상태로 확정된다. 이후 `reset()`을 호출해도 content(카드)가 이미 사라진 채로 배경만 남는 시각적 버그가 발생. `false` 반환으로 dismiss 확정을 막으면 스와이프 후 카드가 자동으로 원위치로 복귀하면서 `EndToStart` 감지 시점에 삭제 다이얼로그를 표시할 수 있다.
- **`ShowNotice` 오용 제거**: 기존 코드에서 프리셋 개수 초과 시 `SettingsIntent.ShowNotice`를 호출했는데, 이는 앱 공지사항 다이얼로그를 위한 인텐트로 맥락이 다르다. 로컬 `showLimitDialog` 상태로 처리해 관심사를 분리.
- **`PresetLimitExceeded` 전용 side effect**: 기존 `ShowError`(Snackbar)를 재사용하면 경고의 시각적 무게감이 부족하고 프리셋 설정 화면과 UX가 불일치한다. 별도 side effect로 분리해 UI에서 AlertDialog로 일관되게 처리.
- **개수 확인 위치**: `DownloadMarketPresetUseCase` 내부가 아닌 ViewModel의 `downloadPreset()`에서 확인. UseCase는 단일 책임(다운로드 실행)을 유지하고, 개수 제한은 비즈니스 규칙이므로 ViewModel에서 판단하는 것이 적합.

---

## [2026-03-01] fix(preset-market): 월페이퍼 업로드·클린 아키텍처·UI 오버플로우 수정

### 목표

프리셋 마켓 업로드 후 Firestore에 `wallpaperUrl: null`이 저장되는 버그를 해결하고, ViewModel이 Repository를 직접 참조하는 클린 아키텍처 위반을 수정한다. 추가로 상세 화면의 "포함된 설정" 칩 목록이 가로 오버플로우로 잘리는 레이아웃 버그도 수정한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Domain` | `usecase/ObserveAuthStateUseCase.kt` (신규) | `repository.authStateChanges(): Flow<MarketUser?>` 를 캡슐화; ViewModel의 직접 Repository 의존 제거 |
| 2 | `Domain` | `usecase/SignOutUseCase.kt` (신규) | `repository.signOut()` 캡슐화 |
| 3 | `Domain` | `util/WallpaperHelper.kt` (수정) | `saveCurrentWallpaperForPreset(Long)` → `copyWallpaperFromUri(sourceUri: String, presetId: Long): String?` 로 교체; WallpaperManager API 의존 완전 제거 |
| 4 | `Data` | `util/WallpaperHelperImpl.kt` (수정) | WallpaperManager 기반 캡처 로직 제거; `copyWallpaperFromUri()` 구현 — 사용자가 선택한 `content://` URI를 `ImageCompressor.compressToWebP(context, uri)` 로 WebP 압축 후 내부 저장소 저장 |
| 5 | `Data` | `util/ImageCompressor.kt` (수정) | `compressToWebP(File, maxWidth, quality): ByteArray` 오버로드 추가 — `BitmapFactory.decodeFile()` 사용(ContentResolver 우회); 공통 `encodeToWebP(Bitmap, ...)` private 헬퍼 추출 |
| 6 | `Data` | `repository/MarketPresetRepositoryImpl.kt` (수정) | `uploadImage()`: 절대경로(`/`로 시작) → `compressToWebP(File)`, content:// URI → `compressToWebP(context, Uri)` 분기 처리 |
| 7 | `Data` | `local/room/AppDatabase.kt` (수정) | DB 버전 1→2; `MIGRATION_1_2` 추가 — `youtubeChannelName` 컬럼 제거를 위한 create-copy-drop-rename 패턴(SQLite 3.35 미만 DROP COLUMN 미지원) |
| 8 | `Data` | `di/DatabaseModule.kt` (수정) | `.addMigrations(AppDatabase.MIGRATION_1_2)` 추가 |
| 9 | `Data` | `local/room/preset/PresetEntity.kt` (수정) | `youtubeChannelName` 컬럼 제거; `toDomain()`·`toEntity()` 매퍼 반영 |
| 10 | `Data` | `remote/firestore/MarketPresetDto.kt` (수정) | `youtubeChannelName` 필드 제거; `toDomain()`·`toDto()` 매퍼 반영 |
| 11 | `Data` | `usecase/DownloadMarketPresetUseCase.kt` (수정) | `youtubeChannelName` 매핑 라인 제거 |
| 12 | `Domain` | `model/preset/Preset.kt` (수정) | `youtubeChannelName` 필드 제거 |
| 13 | `Domain` | `model/preset/MarketPreset.kt` (수정) | `youtubeChannelName` 필드 제거 |
| 14 | `Feature(Settings)` | `SettingsContract.kt` (수정) | `SavePreset`에 `wallpaperUri: String? = null` 파라미터 추가 |
| 15 | `Feature(Settings)` | `SettingsViewModel.kt` (수정) | `MarketPresetRepository` 직접 의존 제거 → `ObserveAuthStateUseCase` 주입; `savePreset()`: `copyWallpaperFromUri(intent.wallpaperUri, ...)` 사용 |
| 16 | `Feature(PresetMarket)` | `PresetMarketViewModel.kt` (수정) | `MarketPresetRepository` 직접 의존 제거 → `ObserveAuthStateUseCase` + `SignOutUseCase` 주입 |
| 17 | `Feature(Settings)` | `ui/PresetSettingsContent.kt` (수정) | `SavePresetDialog`: `PickVisualMedia` 갤러리 피커 추가(월페이퍼 체크 시 "이미지 선택" 버튼 표시, 선택됨 ✓ 표시); 권한 요청 로직 전체 제거; `PresetItemCard` 태그 `Row` → `FlowRow` 교체 |
| 18 | `Feature(PresetMarket)` | `ui/PresetDetailScreen.kt` (수정) | "포함된 설정" 칩 `Row` → `FlowRow` 교체 (`@OptIn(ExperimentalLayoutApi::class)`) |
| 19 | `Test` | `SettingsViewModelTest.kt` (수정) | `MarketPresetRepository` mock → `ObserveAuthStateUseCase` mock 교체; `makeViewModel()` 파라미터 갱신 |

### 검증 결과

- `:feature:settings:compileDebugKotlin` BUILD SUCCESSFUL
- `:core:data:compileDebugKotlin` BUILD SUCCESSFUL
- `:feature:preset-market:compileDebugKotlin` BUILD SUCCESSFUL

### 설계 결정 및 근거

- **월페이퍼 자동 캡처 → 갤러리 선택 방식 전환**: Android 14+(API 34+)에서 `WallpaperManager.getDrawable()` / `getWallpaperFile()`이 시스템 전용 권한(`READ_WALLPAPER_INTERNAL`)을 요구해 일반 앱에서는 항상 SecurityException 발생. `PickVisualMedia`(시스템 포토 피커)는 Android 11+에서 별도 저장소 권한 없이 동작하므로 근본적으로 더 안정적인 방식.
- **`READ_EXTERNAL_STORAGE`가 `maxSdkVersion="32"`로 제한된 이유**: manifest에 `maxSdkVersion="32"`로 선언되어 API 33+ 기기에서는 해당 권한이 부여되지 않음. `WallpaperManager` API는 이 권한을 요구하므로 API 33+ 기기에서 SecurityException이 발생, `catch(e: Exception)`에 잡혀 null 반환.
- **`ObserveAuthStateUseCase` / `SignOutUseCase` 도입**: ViewModel이 Repository 인터페이스를 직접 참조하면 UseCase 계층을 우회하는 클린 아키텍처 위반. UseCase로 래핑해 단일 책임 원칙과 의존 방향(Feature → Domain → Data) 유지.
- **`FlowRow` 적용**: 고정 `Row`에서 AssistChip 5개(Home/Feed/Drawer/Theme/Wallpaper) 전부 표시 시 총 너비(~430dp)가 가용 너비(~330dp)를 초과해 마지막 칩이 잘림. `FlowRow`로 교체해 칩이 넘칠 경우 자동 줄바꿈.
- **Room 마이그레이션 전략**: SQLite 3.35.0 미만에서 `DROP COLUMN` 미지원(API 34 미만 모두 해당). `CREATE TABLE ... → INSERT INTO ... SELECT ... → DROP TABLE → RENAME` 패턴으로 안전하게 컬럼 제거; `fallbackToDestructiveMigration` 대신 명시적 Migration으로 기존 사용자 데이터 보존.
- **`youtubeChannelName` 제거**: Firestore 스키마 및 로컬 DB에서 사용되지 않는 필드가 확인됨. 도메인 모델·DTO·Room 엔티티에서 일괄 제거하고 DB 마이그레이션으로 무결성 유지.

---

## [2026-03-01] feat(preset-market): 프리셋 마켓 기능 전체 구현

### 목표

사용자들이 자신의 런처 프리셋을 다른 사용자와 공유하고 다운로드할 수 있는 "프리셋 마켓" 기능을 Firebase Firestore/Storage/Auth 기반으로 구현한다. 
다운로드수/좋아요/태그/프리뷰 이미지를 지원하며, 다운로드한 프리셋은 즉시 런처에 반영되고 로컬 프리셋 목록에 추가된다. 
Google Sign-In(Credential Manager API)으로 인증하며, Paging 3으로 무한 스크롤을 구현한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Gradle` | `libs.versions.toml` | paging(3.3.6), playServicesAds(24.3.0), playServicesAuth(21.3.0), credentials(1.5.0), googleid(1.1.1) 버전 추가; firebase-firestore/storage/auth(BOM 관리, non-ktx), paging-runtime/compose, play-services-ads/auth, credentials/credentials-play-services, googleid 라이브러리 추가 |
| 2 | `Gradle` | `feature/preset-market/build.gradle.kts` | android.library + kotlin.compose + hilt + ksp; core:domain/core:data/core:ui 의존; Paging Compose, Firebase BOM+Firestore, AdMob, Credential Manager, Coil 의존성 추가 |
| 3 | `Gradle` | `core/data/build.gradle.kts` | Firebase BOM + firebase-firestore/storage/auth + paging-runtime 추가 |
| 4 | `Gradle` | `app/build.gradle.kts` | feature:preset-market 모듈 의존성 + play-services-ads 추가 |
| 5 | `Domain` | `model/preset/MarketPreset.kt` (신규) | 마켓 프리셋 도메인 모델: id/authorUid/authorDisplayName/name/description/tags/previewImageUrls/thumbnailUrl/downloadCount/likeCount + Preset 데이터 필드 전체(URL 기반) + createdAt/updatedAt |
| 6 | `Domain` | `model/preset/MarketUser.kt` (신규) | 마켓 사용자 모델: uid/displayName/email/photoUrl |
| 7 | `Domain` | `repository/MarketPresetRepository.kt` (신규) | Firestore CRUD + 좋아요 토글 + 이미지 업로드/다운로드 + 검색 + 다운로드 카운트 등 13개 메서드 인터페이스 |
| 8 | `Domain` | `usecase/GetTopDownloadPresetsUseCase.kt` (신규) | 다운로드 Top 10 조회 |
| 9 | `Domain` | `usecase/GetTopLikePresetsUseCase.kt` (신규) | 좋아요 Top 10 조회 |
| 10 | `Domain` | `usecase/GetMarketPresetDetailUseCase.kt` (신규) | 마켓 프리셋 상세 조회 |
| 11 | `Domain` | `usecase/SearchMarketPresetsUseCase.kt` (신규) | 검색 쿼리 실행 |
| 12 | `Domain` | `usecase/ToggleMarketPresetLikeUseCase.kt` (신규) | 좋아요 토글 |
| 13 | `Domain` | `usecase/UploadPresetToMarketUseCase.kt` (신규) | 프리셋 업로드 오케스트레이션 |
| 14 | `Domain` | `usecase/SignInWithGoogleUseCase.kt` (신규) | Google 로그인 |
| 15 | `Domain` | `usecase/GetCurrentMarketUserUseCase.kt` (신규) | 현재 로그인 유저 조회 |
| 16 | `Data` | `di/FirebaseModule.kt` (신규) | FirebaseAuth/Firestore/Storage @Provides @Singleton Hilt 모듈 |
| 17 | `Data` | `remote/firestore/MarketPresetDto.kt` (신규) | Firestore 문서 DTO: @DocumentId/@ServerTimestamp 어노테이션, toDomain()/toDto() 확장함수, buildSearchKeywords() (이름 소문자 공백분리 + 태그 결합) |
| 18 | `Data` | `util/ImageCompressor.kt` (신규) | compressToWebP(max 1080px, quality 80, API 30+ WEBP_LOSSY/이하 WEBP 분기) + generateThumbnail(200px, quality 70) |
| 19 | `Data` | `repository/MarketPresetRepositoryImpl.kt` (신규) | Firestore CRUD; 좋아요 runTransaction(likes 서브컬렉션 atomic increment/decrement); Storage WebP 압축 업로드 + getFile() 다운로드; searchPresets: whereArrayContains("searchKeywords") + 복수키워드 클라이언트 필터링 |
| 20 | `Data` | `paging/MarketPresetPagingSource.kt` (신규) | PagingSource<DocumentSnapshot, MarketPreset>: createdAt DESC + startAfter cursor 기반 페이징 |
| 21 | `Data` | `paging/SearchMarketPresetPagingSource.kt` (신규) | 검색 쿼리 PagingSource: whereArrayContains("searchKeywords", firstKeyword) + 복수키워드 클라이언트 필터링 |
| 22 | `Data` | `usecase/DownloadMarketPresetUseCase.kt` (신규) | Storage URL→로컬 파일 다운로드(filesDir/market_presets/{id}/); MarketPreset→Preset 변환 + savePreset(); 런처 설정 즉시 적용(save*UseCase 호출); incrementDownloadCount() |
| 23 | `Data` | `di/RepositoryModule.kt` (수정) | MarketPresetRepositoryImpl → MarketPresetRepository @Binds 추가 |
| 24 | `Feature(PresetMarket)` | `PresetMarketContract.kt` (신규) | PresetMarketState(currentUser/topDownloadPresets/topLikePresets/selectedTab); MarketTab enum; Intent 7개; SideEffect 4개 |
| 25 | `Feature(PresetMarket)` | `MarketSearchContract.kt` (신규) | MarketSearchState(query/isLoading/error); Intent/SideEffect 정의 |
| 26 | `Feature(PresetMarket)` | `PresetDetailContract.kt` (신규) | PresetDetailState(preset/isLiked/isDownloading/isLoading/error); Intent/SideEffect 정의 |
| 27 | `Feature(PresetMarket)` | `PresetMarketViewModel.kt` (신규) | init: currentUser 로드 + LoadTopPresets; recentPresetsPaging Flow<PagingData<MarketPreset>> cachedIn |
| 28 | `Feature(PresetMarket)` | `MarketSearchViewModel.kt` (신규) | queryFlow MutableStateFlow; flatMapLatest → SearchMarketPresetPagingSource; searchResultsPaging cachedIn |
| 29 | `Feature(PresetMarket)` | `PresetDetailViewModel.kt` (신규) | pendingAction 패턴으로 로그인 후 원래 액션 재실행; ensureSignedIn{} 헬퍼; signIn 성공 시 pendingAction.invoke() |
| 30 | `Feature(PresetMarket)` | `navigation/MarketRoute.kt` (신규) | HOME/DETAIL/{presetId}/SEARCH/{query} 라우트 상수 + detail()/search() 헬퍼 |
| 31 | `Feature(PresetMarket)` | `ui/AdmobBanner.kt` (신규) | AndroidView + AdView adaptive banner |
| 32 | `Feature(PresetMarket)` | `ui/MarketPresetCard.kt` (신규) | 썸네일 + 그라디언트 오버레이 + 태그 칩 카드 (Top 10 ViewPager용) |
| 33 | `Feature(PresetMarket)` | `ui/MarketPresetListItem.kt` (신규) | 가로형 카드: 왼쪽 썸네일 + 오른쪽 정보(제목/태그/카운트) |
| 34 | `Feature(PresetMarket)` | `ui/MarketHomeScreen.kt` (신규) | 검색바 + AdmobBanner + TabRow(다운로드/좋아요 Top10) + HorizontalPager(3초 auto-scroll) + LazyColumn(Paging 3) |
| 35 | `Feature(PresetMarket)` | `ui/MarketSearchResultScreen.kt` (신규) | TopAppBar 내 검색바(프리필) + LazyColumn(Paging 3) + Empty state |
| 36 | `Feature(PresetMarket)` | `ui/PresetDetailScreen.kt` (신규) | HorizontalPager(프리뷰) + PagerIndicator + 정보/태그/카운트 + 좋아요/다운로드 버튼 |
| 37 | `Feature(PresetMarket)` | `ui/GoogleSignInHandler.kt` (신규) | Credential Manager API 래퍼; RequireSignIn SideEffect 수신 시 Google Sign-In 바텀시트 표시; idToken → ViewModel 전달 |
| 38 | `Feature(PresetMarket)` | `ui/UploadPresetDialog.kt` (신규) | PickMultipleVisualMedia(최대 4장) + 태그 InputChip + LinearProgressIndicator |
| 39 | `App` | `MainActivity.kt` (수정) | MarketRoute HOME/SEARCH/DETAIL composable 3개 추가(슬라이드 전환); SettingsSideEffect when 블록에 UploadSuccess/UploadError/RequireSignIn 분기 추가; onNavigateToMarket 콜백 연결 |
| 40 | `App` | `StreamLauncherApplication.kt` (수정) | MobileAds.initialize(this) 추가 |
| 41 | `App` | `AndroidManifest.xml` (수정) | INTERNET 권한 추가; AdMob APPLICATION_ID meta-data(테스트 ID) 추가 |
| 42 | `Feature(Settings)` | `SettingsContract.kt` (수정) | UploadPreset intent 추가; UploadSuccess/UploadError/RequireSignIn SideEffect 추가 |
| 43 | `Feature(Settings)` | `SettingsViewModel.kt` (수정) | UploadPresetToMarketUseCase + GetCurrentMarketUserUseCase 주입; uploadPreset() 핸들러(로그인 체크 → MarketPreset 빌드 → 업로드) |
| 44 | `Feature(Settings)` | `ui/PresetSettingsContent.kt` (수정) | "프리셋 마켓" GlassSettingsTile 추가; PresetItemCard에 공유 IconButton 추가; UploadToMarketDialog 인라인 컴포저블 추가; onNavigateToMarket 파라미터 추가 |
| 45 | `Feature(Settings)` | `ui/SettingsDetailScreen.kt` (수정) | onNavigateToMarket 파라미터 추가 및 PRESET 케이스에 전달 |
| 46 | `Test` | `SettingsViewModelTest.kt` (수정) | UploadPresetToMarketUseCase + GetCurrentMarketUserUseCase mock 추가; makeViewModel() 파라미터 및 SettingsViewModel 생성자 인수 반영 |

### 검증 결과

- `assembleDebug` BUILD SUCCESSFUL
- `./gradlew test` BUILD SUCCESSFUL (실패 0건)

### 설계 결정 및 근거

- **Firebase BOM 34.x non-ktx 아티팩트 사용**: Firebase BOM 34.x에서 `-ktx` suffix 아티팩트가 BOM에서 제거됨. `firebase-firestore-ktx` → `firebase-firestore`로 변경하여 BOM 버전 관리 유지.
- **DownloadMarketPresetUseCase를 core:data에 배치**: Android Context가 필요한 이미지 다운로드 로직이 포함되어 있어 순수 JVM 모듈인 core:domain에 배치 불가. core:data에서 Context를 주입받아 구현.
- **feature:preset-market이 core:data에 의존**: DownloadMarketPresetUseCase(core:data) 및 FirebaseFirestore(core:data가 provide)를 PresetDetailViewModel과 MarketSearchViewModel에서 직접 사용해야 하므로 core:data 의존 추가. 단방향 의존성(feature→core) 원칙 유지.
- **UploadToMarketDialog를 feature:settings 내 인라인 구현**: feature:settings는 feature:preset-market에 의존하지 않으므로 preset-market의 UploadPresetDialog를 직접 import 불가. 업로드 다이얼로그를 feature:settings 내에 인라인 컴포저블로 구현하여 단방향 의존성 원칙 준수.
- **pendingAction 패턴으로 로그인 후 액션 재실행**: 미로그인 상태에서 좋아요/다운로드 시도 시 RequireSignIn SideEffect를 발송하고 pendingAction에 원래 람다를 저장. 로그인 성공 후 pendingAction.invoke()로 재실행하여 UX 중단 없이 의도한 액션 완료.
- **Firestore searchKeywords 배열 기반 검색**: Firestore는 full-text search 미지원. 업로드 시 프리셋 이름(소문자 공백분리)과 태그를 결합한 searchKeywords 배열 필드를 생성, array-contains로 단일 키워드 쿼리. 복수 키워드는 클라이언트 필터링으로 처리(Firestore array-contains-any는 다른 필드와 복합 쿼리 불가 제약).
- **AdMob 테스트 ID 사용**: 실 APPLICATION_ID 노출 방지를 위해 테스트 ID(ca-app-pub-3940256099942544~3347511713) 적용. 배포 시 실제 ID로 교체 필요.

---

## [2026-03-01] refactor(settings): 네비게이션 구조를 NavHost 기반으로 리팩토링

### 목표

`SettingsScreen` 내부의 `when(currentTab)` 분기로 서브화면을 전환하던 방식을 제거하고, 최상위 `NavHost`에 `launcher` / `settings_detail/{menu}` 두 destination을 분리함으로써 런처 Pager와 설정 상세 화면을 독립된 화면으로 관리한다. 설정 상세 화면은 공통 Scaffold(`SettingsDetailScreen`)에서 메뉴에 따라 content만 교체하는 방식을 채택한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Feature(Settings)` | `navigation/SettingsRoute.kt` (신규) | `SettingsRoute` object(라우트 상수 + `detail()` 헬퍼) 및 `SettingsMenu` enum(`COLOR`, `IMAGE`, `FEED`, `APP_DRAWER`, `PRESET`) 정의 |
| 2 | `Feature(Settings)` | `build.gradle.kts` | `androidx.navigation.compose` 의존성 추가 |
| 3 | `Feature(Settings)` | `SettingsContract.kt` | `SettingsState`에서 `currentTab` 필드 제거; `SettingsIntent.ChangeTab` 제거; `SettingsSideEffect.NavigateToMain` data object 추가 |
| 4 | `Feature(Settings)` | `SettingsViewModel.kt` | `ChangeTab` 핸들러 제거; `ResetTab` → `sendEffect(NavigateToMain)` 으로 변경; `SettingsTab` import 제거 |
| 5 | `Feature(Settings)` | `SettingsScreen.kt` | `state` 파라미터 제거; `onNavigate: (String) -> Unit` 파라미터 추가; `BackHandler` / `when(currentTab)` 제거 → `MainSettingsContent`만 렌더링; 네비게이션 타일 클릭 → `onNavigate(SettingsRoute.detail(...))` 위임; 서브 컴포저블 `private` → `internal`; `SettingsPageHeader` 함수 삭제; 각 서브 컴포저블에서 헤더 코드 제거 |
| 6 | `Feature(Settings)` | `ui/SettingsDetailScreen.kt` (신규) | 공통 Scaffold 래퍼: `glassEffect(glassSurface)` 배경, `TopAppBar`(뒤로가기 + 메뉴 타이틀), `SettingsMenu`에 따라 content 교체 |
| 7 | `Feature(Settings)` | `model/SettingsTab.kt` | 파일 삭제 |
| 8 | `App` | `MainActivity.kt` | `rememberNavController()` 추가; 기존 `CrossPagerNavigation` 블록을 `NavHost` > `composable(LAUNCHER)` 으로 래핑; `composable(DETAIL)` destination 추가(슬라이드 전환 애니메이션); `settingsViewModel.effect` collect에 `NavigateToMain` → `popBackStack(LAUNCHER)` 핸들러 추가; `SettingsScreen`에 `onNavigate = { navController.navigate(it) }` 전달 |
| 9 | `Feature(Settings)` | `SettingsViewModelTest.kt` | `ChangeTab` / `SettingsTab` 관련 테스트 1개 제거; `ResetTab` 테스트 → `NavigateToMain` side effect 발행 검증으로 교체(Turbine `effect.test {}` 패턴) |

### 검증 결과

- `assembleDebug` BUILD SUCCESSFUL
- 전체 unit test BUILD SUCCESSFUL (실패 0건)
- (수동) 설정 메인 타일 클릭 → 슬라이드 전환으로 상세 화면 진입 확인
- (수동) 뒤로가기 → 런처(설정 Pager 위치) 복귀 확인
- (수동) HOME 버튼 → `NavigateToMain` side effect → `popBackStack` → 런처 홈 복귀 확인

### 설계 결정 및 근거

- **공통 Scaffold 방식 채택 (방식 B):** 각 서브화면을 개별 NavGraph destination으로 분리하는 대신 `SettingsDetailScreen` 하나에서 `when(menu)` content 교체 방식을 선택. destination 수를 최소화하면서도 TopAppBar·배경 등 공통 UI를 한 곳에서 관리할 수 있어 유지보수에 유리.
- **`SettingsPageHeader` 완전 삭제:** Scaffold의 `TopAppBar`가 이미 메뉴별 타이틀을 표시하므로 각 서브 컴포저블 내부의 `SettingsPageHeader`는 중복 렌더링. `showHeader` 조건 분기를 거치지 않고 함수 자체를 제거해 불필요한 코드를 원천 차단.
- **`SettingsDetailScreen`을 `public`으로 노출:** 멀티모듈 구조에서 `internal`은 같은 Gradle 모듈 내에서만 접근 가능하므로 `app` 모듈의 `MainActivity`에서 호출하려면 `public` 필요.
- **`glassEffect(overlayColor = glassSurface)` 적용:** `SettingsScreen`이 렌더링되는 `UpPage`와 동일한 `glassSurface` 색상을 사용해 시각적 일관성 유지.

---

## [2026-03-01] feat(settings): 이미지 설정 초기화 버튼 및 확인 다이얼로그 추가

### 목표

홈 이미지 설정 화면(`ImageSettingsContent`)에서 선택된 모든 그리드 셀 이미지를 한 번에 초기화할 수 있는 버튼과, 실수 방지를 위한 확인 다이얼로그를 구현한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Feature(Settings)` | `SettingsContract.kt` | `ResetAllGridImages` data object intent 추가 |
| 2 | `ViewModel` | `SettingsViewModel.kt` | `handleIntent`에 `ResetAllGridImages` 분기 추가; `resetAllGridImages()` 함수 구현 — 4개 GridCell 전체 이미지 URI를 null로 초기화하고 DataStore에 영속화 |
| 3 | `Resources` | `strings.xml` | `settings_image_reset`("이미지 초기화"), `settings_image_reset_dialog_message`("이미지 셋팅을 초기화하시겠습니까?") 문자열 추가 |
| 4 | `UI` | `SettingsScreen.kt` | `AlertDialog`, `TextButton` import 추가; `showResetDialog` 상태 변수 추가; 이미지 선택 버튼 Row 하단에 전체 너비 `OutlinedButton`("이미지 초기화") 추가; `AlertDialog` 컴포저블 추가 — "확인" 클릭 시 `ResetAllGridImages` intent 발행 후 닫힘, "취소" 클릭 시 그대로 닫힘 |

### 검증 결과

- 파일 수정 완료, 컴파일 오류 없음 (import 누락 없음, sealed interface 패턴 준수)
- 기존 이미지 선택/저장 흐름(`SetGridImage`) 영향 없음 — 독립적인 버튼 및 Intent로 추가됨

### 설계 결정 및 근거

- **`OutlinedButton` 사용:** 초기화는 이미지 선택보다 낮은 우선순위의 보조 액션이므로 Filled 버튼 대신 `OutlinedButton`으로 시각적 위계를 구분.
- **다이얼로그를 Column 외부에 배치:** Compose에서 `AlertDialog`는 컨텐츠 계층 밖(`if (showResetDialog)` 조건부 렌더링)에 두는 것이 레이아웃 중첩 없이 올바르게 동작하며, 기존 `NoticeDialog` 패턴과 일치.
- **기존 문자열 재사용(`preset_confirm`, `cancel`):** 새 문자열을 최소화하여 리소스 중복 방지.
- **낙관적 업데이트 패턴 유지:** state를 즉시 반영한 뒤 코루틴으로 DataStore 영속화 — 기존 ViewModel 패턴 그대로 적용.

---

## [2026-02-28] feat(settings): 런처 설정 프리셋 기능 전체 구현 및 시스템 배경화면 권한 연동

### 목표

현재 런처의 5가지 핵심 설정(홈 이미지, 피드, 앱 서랍, 배경화면, 테마 컬러)을 사용자가 원하는 항목만 취합하여 여러 개의 "프리셋"으로 저장하고 손쉽게 전환할 수 있도록 한다. 초기 기획 단계를 거쳐 Room DB를 핵심 저장 매체로 채택하고, UI 구현 및 시스템 배경화면 권한 이슈까지 모두 대응하여 완성도를 높인다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Data` | `PresetEntity.kt`, `PresetDao.kt`, `PresetDatabase.kt` | DataStore 대신 Room DB를 사용하도록 결정하고, 확장성을 고려한 프리셋 엔티티 및 Dao 쿼리 설계. 앱 구동 시 내부 DB 인스턴스 빌드 로직 추가 |
| 2 | `Data/DI` | `RoomDatabaseModule.kt`, `RepositoryModule.kt` | Hilt를 통해 `PresetDao` 및 `PresetRepositoryImpl`의 의존성이 도메인 계층에 원활히 주입되도록 모듈 셋업 |
| 3 | `Domain` | `GetPresetsUseCase`, `SavePresetUseCase` 등 | 파라미터 5개 항목(선택 여부 포함)을 받아 프리셋을 저장, 로드, 삭제하는 UseCase 3종 구현 |
| 4 | `Data` | `WallpaperHelperImpl.kt` | 현재 적용된 시스템 바탕화면의 Bitmap Drawable을 추출하여 내부 저장소 Cache 디렉토리에 PNG로 백업하고, 프리셋 로드 시 해당 PNG를 다시 시스템 배경으로 덮어씌우는 하드웨어 연동 로직 구현 |
| 5 | `Manifest` | `AndroidManifest.xml` | 바탕화면 추출을 위한 `READ_EXTERNAL_STORAGE` (Max SDK 32) 및 `READ_MEDIA_IMAGES` (API 33+) 권한 명시 추가 |
| 6 | `UI` | `PresetSettingsContent.kt` | 설정 화면 하위에 프리셋 리스트 페이지 구현. 현재 설정을 프리셋으로 추가하는 버튼 및 항목 체크박스 다이얼로그 구성. `rememberLauncherForActivityResult`를 활용해 시스템 파일 권한 런타임 요청 파이프라인 구축 |
| 7 | `UI` | `PresetSettingsContent.kt` | 프리셋 리스트의 SwipeToDismiss(스와이프 삭제) 구현. 하단 시스템 네비게이션 바와 리스트가 겹치지 않도록 `windowInsetsPadding(WindowInsets.navigationBars)` 여백 적용 |
| 8 | `UI` | `PresetSettingsContent.kt` | 권한 2회 이상 거부로 시스템 팝업이 막힌 경우(`shouldShowRequestPermissionRationale`)를 대비한 권한 필요 안내 커스텀 다이얼로그(Rationale) 및 자체 설정 화면 포워딩 UI 구성 |
| 9 | `Resources`| `strings.xml` | 프리셋 관련 다국어 레이블(항목 이름, 다이얼로그 타이틀, 권한 안내 메시지 등) 리소스 대거 추가 |

### 버그 수정 이력

| 버그 | 원인 | 해결 |
|------|------|------|
| 스와이프 삭제 취소 시 항목이 옆으로 멈춰버림 | `confirmValueChange`에서 알러트 창을 띄우고 상태 전이를 일시정지시키려 했으나, Jetpack Compose Material 3의 `SwipeToDismissBoxState` 렌더링 사이클 특성상 `false`를 즉시 반환하지 않으면 멈춰버림 | 상태 람다에서 즉시 `false`를 반환하여 카드는 제자리로 튕겨 돌아가게 하고, 삭제 예정 상태(presetToDelete)만 별도 State로 빼내어 Alert Dialog를 띄우도록 구조 100% 분리 |
| 스와이프 도중 절반 지점에서 삭제 배경 및 휴지통 노출됨 | 색상 표시의 분기 기준이 `targetValue`(목적지)로 되어있어 스와이프 도중에 값이 바뀌어버림 | 기준을 `targetValue`가 아닌 사용자가 현재 손을 움직이는 방향인 `dismissDirection`으로 수정하여 스와이프를 시작하는 즉시 배경색과 휴지통이 드러나도록 고침 |
| 시스템 배경화면(Wallpaper)이 프리셋으로 저장 안 됨 | Android 11 이상부터는 다른 앱이 세팅한 바탕화면 원본을 읽기 위해 외부 저장소 읽기 권한이 반드시 필요하나 manifest에 누락됨 | `AndroidManifest`에 권한을 추가하고, 앱 화면 내에서 동의를 구하는 팝업 로직을 전면 구현. 권한 거절 시 배경화면 옵션을 제외하고 잔여 4가지만 저장되도록 융통성 처리 방어 코드 작성 |

### 설계 결정 및 근거

- **DataStore 대신 Room DB 채택:** 로컬 설정 저장은 기존에 DataStore를 썼지만, 프리셋은 사용자가 임의의 개수를 무한정 쌓아두거나 개별 요소만 지울 수 있어야 하며, 훗날 프리셋 코드를 주고받는 '프리셋 마켓' 기획 확장을 고려할 때 관계형 DB인 Room을 쓰는 것이 장기적으로 훨씬 유리하다고 판단하여 도메인 채택.
- **바탕화면 Bitmap 추출 저장 방식:** 안드로이드 시스템상 Live Wallpaper(움직이는 배경) 등은 파일 형태 캐싱이 불가능함. 추후 공유 기능을 고려할 때 이미지 파일 자체를 빼내어 복사본을 들고 다니는 쪽이 독립성 유지에 유리하므로, 현재 설정된 배경화면을 PNG 파일로 인코딩(`compress`)하여 캐시 영역에 물리적으로 떨어뜨리는 강고한 아키텍처 적용.
- **Rationale Dialog 분기 처리:** 안드로이드 팝업 스팸 어뷰징 방지 정책상 사용자가 2번 거절하면 팝업 호툴 코드가 먹히지 않으므로, 이 경우 말없이 저장이 취소되는 답답함을 없애기 위해 사용자를 직접 앱 권한 시스템 세팅 창(`ACTION_APPLICATION_DETAILS_SETTINGS`)으로 보내버리는 UX 도입.

---

## [2026-02-28] refactor(domain/data/ui): RSS 피드 연동 기능 완전 제거

### 목표

프로젝트에서 더 이상 사용하지 않기로 결정된 RSS 관련 기능(피드 표출, 설정, 저장, 백그라운드 동기화)과 코드를 모든 계층(Domain, Data, Network, UI)에서 완전히 제거하여 코드베이스를 경량화하고 레거시를 정리한다.

### 변경 사항

| # | 계층 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `Network` | `RssFeedApi.kt`, `RssFeedResponse.kt` | 파일 삭제 (RSS 전용 Retrofit 인터페이스 및 DTO 모델 제거) |
| 2 | `Network` | `NetworkQualifiers.kt`, `NetworkModule.kt` | `@XmlRetrofit` 한정자 및 `XmlConverterFactory`를 사용하는 Retrofit XML 파서 주입 코드 완전 삭제 (의존성 정리) |
| 3 | `Domain` | `FeedItem.kt` | `sealed interface`에서 RSS 전용 데이터 클래스인 `NoticeItem` 제거 |
| 4 | `Domain` | `LauncherSettings.kt`, `SettingsRepository.kt` | 설정 모델에서 `rssUrl` 변수 제거 및 RssUrl 갱신 함수 시그니처 제거 |
| 5 | `Domain` | `GetIntegratedFeedUseCase`, `SaveFeedSettingsUseCase` 등 | 파라미터에서 `rssUrl` 제거 및 내부 연결 로직 삭제 |
| 6 | `Data` | `FeedRepositoryImpl.kt` | `RssFeedApi` 의존성 제거, `fetchRssItems()` 메서드 삭제, 피드 통합 로직에서 파싱 및 결합부 덜어냄 |
| 7 | `Data` | `SettingsRepositoryImpl.kt` | DataStore 관리 키에서 `rssUrlKey` 제거 및 초기화/저장 로직 삭제 |
| 8 | `Data` | `worker/FeedSyncWorker.kt` | 백그라운드 동기화 조건에서 `rssUrl.isEmpty()` 검사 로직 제외 |
| 9 | `Feature(Settings)` | `SettingsContract`, `SettingsViewModel`, `SettingsScreen` | State와 Intent에서 `rssUrl` 파라미터 제외, 설정 화면의 RSS 입력 OutlinedTextField 및 에러 상태 징후 로직 제거 |
| 10| `Feature(Launcher)` | `FeedContract`, `FeedViewModel`, `FeedScreen` | 피드 새로고침 조건에서 RSS 파라미터 배제, UI의 `NoticeItemRow` 컴포넌트 렌더링부 삭제 (문법 오류 수정 포함) |
| 11| `Tests` | 전 계층 테스트 코드 | `FeedRepositoryImplTest` (기능 상실로 전면 삭제), `FeedItemTest`, `SettingsViewModelTest`, `FeedViewModelTest` 등에서 RSS/NoticeItem 관련 mock 셋업과 분기, Assert 구문 일관 제거 |

### 설계 결정 및 근거

-   **사용성(Product) 전략:** 앱의 핵심 가치가 라이브 플랫폼(YouTube, 치지직) 통합 알림으로 집중됨에 맞춰, 구형 정적 텍스트 기반인 RSS 지원 기능을 의도적으로 제외(Sunset).
-   **XML 파서 및 관련 클래스 일소:** Retrofit 사용 시 JSON 데이터와 XML 데이터를 직렬화하기 위해 두 가지 `ConverterFactory`를 운용하고 한정자로 분리해 썼음. RSS가 제거됨에 따라 이들 팩토리와 네트워크 설정 부수 코드를 지워 메모리 풋프린트를 줄이고 빌드 속도 및 앱 사이즈 최적화.
-   **단일 진실 공급원(SSOT) 재정립:** 설정 화면부터 백그라운드 워커에 이르는 거대한 파이프라인에서 RSS라는 축을 하나 빼냄으로써 `youtubeChannelId`, `chzzkChannelId` 2방향 통합으로 구조 최적화.

---

## [2026-02-28] refactor(settings): feature:settings 모듈 분리 — God ViewModel 해소

### 목표

`feature:launcher`의 `HomeViewModel`이 런처 로직과 설정 로직을 모두 담당하는 "God ViewModel" 상태를 해소한다.
설정 관련 코드를 `feature:settings`로 분리하여 단일 책임 원칙을 달성하고,
두 모듈이 `core:domain`을 통해서만 간접 통신하는 클린 아키텍처 구조로 리팩토링한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `feature/settings/build.gradle.kts` | Compose/Hilt/KSP 플러그인 + `:core:domain`, `:core:ui` 의존성 추가 |
| 2 | `app/build.gradle.kts` | `implementation(project(":feature:settings"))` 추가 |
| 3 | `feature/settings/.../model/SettingsTab.kt` | `launcher.model` → `settings.model` 패키지로 이동 |
| 4 | `feature/settings/.../model/ImageType.kt` | `launcher.model` → `settings.model` 패키지로 이동 |
| 5 | `feature/settings/.../SettingsContract.kt` | `SettingsState` / `SettingsIntent` / `SettingsSideEffect` 신규 생성 |
| 6 | `feature/settings/.../SettingsViewModel.kt` | `@HiltViewModel`; 설정 관련 UseCase 7개 주입; `checkNotice(version)` 공개 메서드; settings Flow collect (init) |
| 7 | `feature/settings/.../ui/SettingsScreen.kt` | `HomeState/HomeIntent` → `SettingsState/SettingsIntent`로 교체, R import 변경 |
| 8 | `feature/settings/.../ui/NoticeDialog.kt` | 패키지·R import 변경 |
| 9 | `feature/settings/src/main/res/values/strings.xml` | settings_* / notice_* 문자열 신규 리소스 파일로 분리 |
| 10 | `feature/settings/.../SettingsViewModelTest.kt` | 신규 13개 테스트: ChangeAccentColor, SetGridImage, SaveFeedSettings, SaveAppDrawerSettings, ChangeTab, ResetTab, ShowNotice, DismissNotice, checkNotice |
| 11 | `feature/launcher/.../HomeContract.kt` | `currentSettingsTab`, `colorPresetIndex`, `chzzkChannelId`, `youtubeChannelId`, `rssUrl`, `showNoticeDialog` 제거; `ChangeSettingsTab`, `ChangeAccentColor`, `SetGridImage`, `SaveFeedSettings`, `SaveAppDrawerSettings`, `ShowNotice`, `DismissNotice` Intent 제거 |
| 12 | `feature/launcher/.../HomeViewModel.kt` | 설정 관련 UseCase 6개 제거; `checkFirstLaunch`에서 notice 로직 분리; `resetHome`에서 `currentSettingsTab` 초기화 제거; `changeAccentColor`, `setGridImage`, `saveFeedSettings`, `saveAppDrawerSettings`, `dismissNotice` 메서드 제거 |
| 13 | `feature/launcher/.../HomeViewModelTest.kt` | 설정 관련 mock 6개 + 테스트 7개 제거 (25개 잔존) |
| 14 | `app/.../MainActivity.kt` | `settingsViewModel: SettingsViewModel` 추가; `settingsState` 수집; `SettingsScreen`·`NoticeDialog`·`colorPresetIndex` → settingsViewModel 연결; `onCreate`에 `settingsViewModel.checkNotice()` 추가; `onNewIntent`에 `SettingsIntent.ResetTab` 추가 |
| 15 | `feature/launcher/.../ui/SettingsScreen.kt` | 삭제 (feature:settings로 이동) |
| 16 | `feature/launcher/.../ui/NoticeDialog.kt` | 삭제 (feature:settings로 이동) |
| 17 | `feature/launcher/.../model/SettingsTab.kt` | 삭제 (feature:settings로 이동) |
| 18 | `feature/launcher/.../model/ImageType.kt` | 삭제 (feature:settings로 이동) |
| 19 | `feature/launcher/src/main/res/values/strings.xml` | settings_* / notice_* 문자열 삭제 (feed_* / home_* 유지) |

### 검증 결과

- `assembleDebug` BUILD SUCCESSFUL
- 전체 단위 테스트 BUILD SUCCESSFUL (실패 0건)
  - `SettingsViewModelTest`: 13개 신규 테스트 전부 통과
  - `HomeViewModelTest`: 25개 잔존 테스트 전부 통과 (regression 없음)

### 설계 결정 및 근거

**단방향 의존 구조 유지**
`feature:settings`는 `core:domain`과 `core:ui`에만 의존하며, `feature:launcher`에 의존하지 않는다. 반대 방향 의존도 없다. 두 모듈은 `core:domain`의 UseCase/Repository를 통해서만 데이터를 공유하는 클린 아키텍처 원칙을 따른다.

**HomeState에 파생 설정 필드 유지 (`gridCellImages`, `cellAssignments`, `appDrawerGrid*`)**
`HomeScreen`과 `AppDrawerScreen`이 직접 소비하는 필드들은 `HomeState`에 잔류시켰다. `HomeViewModel`은 `GetLauncherSettingsUseCase`를 통해 읽기 전용으로만 유지하므로 중복 쓰기 없이 단일 진실 공급원(DataStore)이 보장된다.

**`SettingsViewModel.checkNotice()` 공개 메서드**
공지 확인은 앱 시작 시점에 `MainActivity.onCreate`에서 호출하는 일회성 동작이므로, Intent 방식보다 단순 메서드 호출이 더 명료하다고 판단했다.

---

## [2026-02-28] feat(launcher): 홈 그리드 셀 앱 아이콘 드래그 이동 기능 고도화

### 목표

홈 화면 그리드 셀에 배치된 앱 아이콘을 드래그로 재배치할 수 있도록 구현한다.
- 편집 모드 진입 후 드래그로 셀 내 순서 변경 및 다른 셀로 이동
- 드래그 중 대상 셀 확장 시각 피드백 유지 + 드래그 취소 버그 해결
- 드롭 위치 감지로 끼어들기 배치 지원

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `core/ui/.../DragDropState.kt` | `DragResult` data class 도입 (targetCell, targetSlotIndex, sourceCell, sourceIndex); `dragSourceCell`, `dragSourceIndex`, `hoveredSlotIndex` 상태 추가; `startDrag`에 sourceCell/sourceIndex 파라미터 추가; `registerSlotBounds` / `unregisterSlotBounds` 추가; `updateDrag`에서 슬롯 인덱스 실시간 추적 |
| 2 | `HomeContract.kt` | `MoveAppBetweenCells(app, fromCell, toCell, toIndex=-1)` Intent 추가 |
| 3 | `HomeViewModel.kt` | `moveAppBetweenCells` 함수 추가 — fromCell 제거 후 toCell에 toIndex 위치 삽입(끼어들기), DataStore 저장 |
| 4 | `HomeScreen.kt` | `GridAppItem`에 `appIndex` 파라미터 추가; `draggable` 제거 → 편집 모드 여부에 따라 `combinedClickable`(비편집) / `detectDragGestures`(편집) 분기; `onGloballyPositioned`에서 슬롯 bounds 등록; 드래그 완료 시 동/이셀 판단 후 Intent 발행; `expandedCell` 계산 — 그리드 드래그 시 `hoveredCell` 복원 + `isDragSource` 조건으로 소스 셀 `GridAppItem` 트리 유지 |
| 5 | `AppDrawerScreen.kt` | `endDrag()` 반환 타입 `DragResult?` 대응 (`result.first/second` → `result.app/targetCell`) |

### 버그 수정 이력

| 버그 | 원인 | 해결 |
|------|------|------|
| 편집 모드에서 앱 아이콘 탭 시 그리드 셀이 눌림 | 편집 모드 `pointerInput`이 탭 이벤트를 소비하지 않아 부모 `Surface.onClick`으로 전파 | `clickable(indication=null)` 추가로 탭 이벤트 조용히 소비 |
| 다른 셀로 드래그 시 드래그 모드가 즉시 취소됨 | `hoveredCell` 변경 → 소스 셀 `isExpanded=false` → `GridAppItem` Dispose → `pointerInput` coroutine 취소 | `expandedCell`을 드래그 중 `hoveredCell`로 유지하되, `GridCellContent`에서 `isDragSource=true`이면 앱 목록 컴포저블을 트리에 강제 유지 (`contentAlpha=0`으로 시각적으로만 숨김) |
| 드래그 중 흔들림 애니메이션 지속 | `rotation` 계산에 드래그 상태 미반영 | `isDragged(draggedApp==app)` 조건을 `when` 최우선으로 추가해 드래그 중 `rotation=0f` 고정 |

### 검증 결과

- `assembleDebug` BUILD SUCCESSFUL
- 전체 단위 테스트 BUILD SUCCESSFUL (실패 0건)

### 설계 결정 및 근거

**`detectDragGesturesAfterLongPress` → `detectDragGestures` 교체**
편집 모드는 이미 길게 누름으로 진입하므로, 드래그 시작에 롱프레스가 두 번 필요한 UX는 과도하다. 편집 모드 진입 후에는 즉시 드래그가 가능한 `detectDragGestures`가 더 자연스럽다.

**소스 셀 `isDragSource` 조건으로 트리 유지**
드래그 중 소스 셀을 `expandedCell`로 고정하는 방법도 있으나, 이 경우 대상 셀의 확장 피드백이 없어 UX가 저하된다. 대신 소스 셀의 `GridAppItem`을 트리에만 유지하고 `contentAlpha=0`으로 숨기는 방식을 선택해, 드래그 취소 방지와 시각적 피드백을 동시에 확보했다.

**`toIndex` 끼어들기 삽입**
드롭 시 슬롯 위 앱 인덱스(`hoveredSlotIndex`)를 `MoveAppBetweenCells.toIndex`로 전달해 `toList.add(toIndex, pkg)`로 삽입. `toIndex=-1`(빈 공간 드롭)이면 끝에 추가하여 기존 동작과 하위 호환을 유지한다.

---

## [2026-02-25] 월페이퍼 색상 대응 — 시스템바 동적 스타일 & 위젯 화면 가시성 개선

### 목표

어두운 월페이퍼 사용 시 상단 시스템바 아이콘(시간·배터리 등)과 위젯 화면의 "화면을 길게 눌러 위젯을 추가하세요" 메시지·아이콘이 배경에 묻혀 보이지 않는 문제를 해결한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `app/.../MainActivity.kt` | `WallpaperManager.OnColorsChangedListener` 등록/해제; `onCreate`에서 초기 색상 즉시 조회; `onResume`에서 백그라운드 변경 재조회; `updateSystemBarStyle(isDark)` — `SystemBarStyle.dark` / `SystemBarStyle.light` 동적 전환 |
| 2 | `feature/widget/.../WidgetScreen.kt` | `+` 아이콘 tint `Color.White.copy(alpha=0.7f)` 고정; `drawBehind` + `BlurMaskFilter`로 아이콘 원형 글로우 쉐도우 추가; 메시지 텍스트 `Color.White.copy(alpha=0.85f)` + `TextStyle.shadow(blurRadius=12f)` 적용 |

### 핵심 구현 상세

#### 1. 월페이퍼 밝기 판별 — `isDarkFromColors()`

`WallpaperColors.HINT_SUPPORTS_DARK_TEXT` 상수는 **API 31(Android 12)에서 추가**되었으므로, API 28~30에서 `colorHints`와 비트 비교하면 항상 `0`이 반환되어 모든 배경을 "어둡다"고 오판한다. API 레벨에 따라 분기 처리한다.

```kotlin
private fun isDarkFromColors(colors: WallpaperColors?): Boolean {
    if (colors == null) return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // API 31+: HINT_SUPPORTS_DARK_TEXT(=1) 플래그가 없으면 어두운 배경
        (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0
    } else {
        // API 28-30: 주 색상의 상대적 휘도로 판별 (0.0=검정, 1.0=흰색)
        val luminance = ColorUtils.calculateLuminance(colors.primaryColor.toArgb())
        luminance < 0.5
    }
}
```

| API 범위 | 판별 방법 | 사용 이유 |
|---|---|---|
| API 31+ | `colorHints and HINT_SUPPORTS_DARK_TEXT` | 상수가 API 31에서 추가된 공식 방법 |
| API 28~30 | `ColorUtils.calculateLuminance(primaryColor)` | 상수가 없어 colorHints는 항상 0 → luminance 직접 계산 |

#### 2. 시스템바 동적 전환

```kotlin
private fun updateSystemBarStyle(isDark: Boolean) {
    val transparent = android.graphics.Color.TRANSPARENT
    if (isDark) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(transparent),
            navigationBarStyle = SystemBarStyle.dark(transparent),
        )
    } else {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
    }
}
```

#### 3. onResume 재조회 (백그라운드 변경 대응)

`OnColorsChangedListener`만으로는 앱이 백그라운드일 때 변경된 월페이퍼 색상을 포그라운드 복귀 시 반영하지 못하는 경우가 있어, `onResume`에서 추가로 재조회한다.

```kotlin
override fun onResume() {
    super.onResume()
    val colors = WallpaperManager.getInstance(this)
        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    val isDark = isDarkFromColors(colors)   // API 레벨 분기 포함
    if (isDark != isWallpaperDark.value) {  // 변경된 경우에만 재적용
        isWallpaperDark.value = isDark
        updateSystemBarStyle(isDark)
    }
}
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `SystemBarStyle.auto()` → `dark/light` 직접 지정 | `auto()`는 시스템 다크모드 기준으로만 판단하여 월페이퍼 색상을 반영하지 못함. `dark/light` 직접 호출로 월페이퍼 밝기와 시스템바 아이콘 색상을 1:1 대응 |
| API 31+: `HINT_SUPPORTS_DARK_TEXT` 비트 플래그 | `WallpaperColors.HINT_SUPPORTS_DARK_TEXT` 상수 자체가 API 31에서 추가됨. 그 이전 버전에서 `colorHints`를 비트 비교하면 항상 `0` 반환 → 항상 어두운 배경으로 오판 |
| API 28~30: `ColorUtils.calculateLuminance()` | `primaryColor`의 상대 휘도(0.0~1.0)를 직접 계산하여 0.5 미만이면 어두운 배경으로 판별. `androidx.core`에 포함되어 추가 의존성 없음 |
| `isDarkFromColors()` 단일 함수로 추출 | 리스너·`onCreate`·`onResume` 세 곳에서 동일한 판별 로직 재사용. API 분기 코드를 한 곳에서만 관리 |
| `colors == null` 시 `isDark=true`(어두운 배경) 처리 | 라이브 배경화면 등 색상 정보 없는 경우의 safe default. 런처 특성상 어두운 배경이 기본값으로 적절 |
| `onResume` 재조회 + `if (isDark != isWallpaperDark.value)` 조건 | 리스너 누락 케이스 보완, 불필요한 `enableEdgeToEdge` 재호출 방지 |

---

## [2026-02-25] feat(ui): 앱 드로어 드래그 시 홈 그리드 섹션 자동 확장 + 드롭 후 확장 유지

### 목표

앱 드로어에서 앱 아이콘을 홈 화면으로 드래그할 때, 마우스 포인터(손가락)가 올라간 그리드 섹션이 자동으로 확장(80%)되어 배치 대상 셀을 시각적으로 명확히 하고, 드롭 완료 후에는 해당 섹션이 확장된 상태를 유지하도록 한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `feature/launcher/.../ui/HomeScreen.kt` | `HomeScreen` 컴포저블 상단에서 `LocalDragDropState.current`를 읽어, 드래그 중(`isDragging == true`)일 때는 `hoveredCell`을, 그 외에는 기존 `state.expandedCell`을 `expandedCell`로 사용하도록 분기 처리 |
| 2 | `feature/launcher/.../HomeViewModel.kt` | `assignAppToCell()` 함수의 `updateState` 블록에 `expandedCell = cell` 추가 — 앱 배치 완료 시 해당 셀을 영구 확장 상태로 설정 |

### 동작 흐름

```
1. 앱 드로어에서 길게 눌러 드래그 시작
   → DragDropState.startDrag() 호출 → isDragging = true
2. 드래그 중 그리드 섹션 위로 이동
   → DragDropState.updateDrag() → hoveredCell 갱신
   → HomeScreen: expandedCell = hoveredCell → spring 애니메이션으로 해당 섹션 확장(80%)
3. 드래그를 섹션 밖으로 이동
   → hoveredCell = null → 균등(50%) 복원
4. 원하는 셀 위에서 드롭 (손가락 뗌)
   → AssignAppToCell 인텐트 처리
   → assignAppToCell()에서 expandedCell = cell 설정
   → isDragging = false 이므로 state.expandedCell(= 배치된 셀) 유지
```

### 설계 결정 및 근거

- **로컬 상태 override 방식 채택**: 드래그 중 일시적 확장은 ViewModel의 비즈니스 로직을 건드리지 않고 `HomeScreen` 컴포저블 내 로컬 연산으로 처리. `DragDropState.hoveredCell`이 이미 `Compose State`로 관리되므로 recomposition이 자동으로 트리거됨.
- **기존 spring 애니메이션 재활용**: `topRowWeight`, `leftColWeight`의 `animateFloatAsState(spring)` 애니메이션이 `expandedCell` 변경에 자동 반응하므로 별도 애니메이션 추가 없이 자연스러운 확장/축소 효과 제공.
- **드롭 후 확장 유지**: 배치 완료 후 ViewModel의 `state.expandedCell`을 해당 셀로 설정하여 드래그 이후에도 사용자가 배치 결과를 즉시 확인할 수 있도록 함.

---

## [2026-02-25] feat(feed): 유튜브 채널 라이브 상태 표시 배지 추가


### 목표

사용자가 설정한 유튜브 채널이 현재 라이브 방송 중인지 파악하여 피드 화면의 채널 프로필 카드에 시각적인 LIVE 배지를 표시한다. 기존 치지직 라이브 상태 배지와 통일감 있는 애니메이션을 주어 사용자 경험을 개선한다.

### 변경 사항

| # | 모듈 | 파일 | 작업 |
|---|------|------|------|
| 1 | `core/data` | `FeedRepositoryImpl.kt` | 유튜브 Search API를 호출하여 `eventType="live"` 조건으로 현재 스트리밍 중인 영상을 검색해 상태를 반환하는 `getYoutubeLiveStatus` 구현 |
| 2 | `core/domain` | `GetYoutubeLiveStatusUseCase.kt` | 채널 ID를 기반으로 라이브 상태 조회를 요청하는 신규 UseCase 생성 |
| 3 | `feature/launcher` | `FeedViewModel.kt` | `refresh` 단계에서 치지직 채널 상태와 함께 별도의 코루틴 Job으로 `GetYoutubeLiveStatusUseCase`를 실행하여 뷰모델 상태 업데이트 |
| 4 | `feature/launcher` | `FeedScreen.kt` | 치지직의 기존 숨쉬기(Breathing) 애니메이션 글로우 효과 코드를 공용 컴포저블(`BreathingLiveBadge`)로 추출하여, 채널 프로필 이름 옆에 붉은색 네온 라이브 뱃지가 노출되도록 디자인 개선 |
| 5 | `feature/launcher` | `FeedViewModelTest.kt` | 새로 주입받는 `GetYoutubeLiveStatusUseCase`에 대한 mockk 객체를 뷰모델 테스트 코드에 추가하고 초기 설정 갱신 |
| 6 | `feature/launcher` | `FeedContract.kt` | `FeedState`에 `youtubeLiveStatus` 변수를 추가하여 상태로 관리 |

### 설계 결정 및 근거

- **과도한 API 트래픽 제어**: 유튜브 Search API(`list=live`)는 1회 통신당 API 할당량(Quota) 100을 소모하므로 코스트가 매우 높다. 따라서 피드를 계속 당긴다고 해서 무조건 조회하지 않고, `MIN_REFRESH_INTERVAL_MS`(60초) 쿨타임 정책을 동일하게 적용하여 불필요한 호출을 제어함.
- **애니메이션 컴포넌트 재사용**: 치지직 채널 카드의 'LIVE' 배지가 제공하는 `drawBehind` 네온 글로우 애니메이션 코드가 동일하게 사용되므로, 이를 떼어내어 `BreathingLiveBadge` 컴포저블 함수로 재사용 구조를 설계하여 UI 일관성을 확보함.
- **배지 영역에서의 링크 분리 (동작 결정)**: 유튜브 라이브 배지 부분의 터치 시 명시적인 라이브 URL로 연결하는 클릭 인텐트를 분리할지 고민했으나, 전체 프로필 영역(`ChannelProfileCard`)의 클릭 동작을 최우선으로 고려해 클릭 리스너 분리를 해제하고 시각적 인디케이터로만 작동하도록 의도함.

## [2026-02-25] refactor(domain): GetLiveStatusUseCase 클래스명 변경

### 목표

유튜브 채널의 실시간 라이브 상태 조회 기능 추가를 앞두고, 기존 치지직의 라이브 상태 조회를 전담하던 `GetLiveStatusUseCase`의 이름을 명확하게 변경하여 역할과 책임을 구분한다.

### 변경 사항

| # | 모듈 | 파일 | 작업 |
|---|------|------|------|
| 1 | `core/domain` | `usecase/GetChzzkLiveStatusUseCase.kt` | 기존 `GetLiveStatusUseCase.kt` 파일명 및 클래스명을 `GetChzzkLiveStatusUseCase`로 변경 |
| 2 | `core/domain` | `usecase/GetChzzkLiveStatusUseCaseTest.kt` | 파일명 및 테스트 클래스명, 내부 인스턴스 변수명 변경 반영 |
| 3 | `feature/launcher` | `FeedViewModel.kt` | 의존성 주입 시 기존 `GetLiveStatusUseCase` 대신 `GetChzzkLiveStatusUseCase`를 주입받고 호출하도록 변경 |
| 4 | `feature/launcher` | `FeedViewModelTest.kt` | 관련된 mockk 모의 객체 선언부 및 변수명 일괄 수정 |

### 설계 결정 및 근거

- **도메인 명확화**: 앞으로 피드 화면에서 '치지직 라이브'와 '유튜브 라이브' 두 가지 비동기 상태를 동시에 다루게 되므로, 각 플랫폼별 UseCase를 명명 규칙에서부터 확실히 분리하여 혼동을 막기 위해 리팩토링을 선행함.

---

## [2026-02-25] bugfix(feed): 피드 설정 즉각 반영 및 채널 ID 빈값 처리 로직 개선

### 목표

설정 화면에서 피드 설정(유튜브 채널 ID, 치지직 채널 ID 등)을 변경하고 피드 화면으로 돌아왔을 때 변경된 설정이 즉시 반영되지 않는 문제(최소 새로고침 쿨타임 제한)를 해결한다. 또한 사용자가 설정에서 특정 채널 ID를 명시적으로 비우고 저장했을 때, 피드 화면에 남아있던 해당 채널의 이전 프로필 카드나 라이브 상태 카드가 완전히 숨김 처리되도록 로직을 개선한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `feature/launcher/.../FeedViewModel.kt` | `init` 블록에서 DataStore의 설정을 수집할 때마다 이전 상태값과 비교하여 변경 여부(`isChanged`)를 판단하고, 설정이 변경되었다면 API 강제 새로고침(`refresh(force = true)`)을 호출하도록 수정 |
| 2 | `feature/launcher/.../FeedViewModel.kt` | `refresh()` 메서드의 `profileJob` 내에서 `youtubeChannelId`가 빈 문자열일 경우, 즉시 화면 State의 `channelProfile`을 `null`로 초기화하여 UI 상에서 유튜브 채널 카드가 렌더링되지 않도록 추가 |
| 3 | `feature/launcher/.../FeedViewModel.kt` | `refresh()` 메서드의 `liveJob` 내에서 `chzzkChannelId`가 빈 문자열일 경우, 화면 State의 `liveStatus`를 `null`로 초기화하여 텅 빈 라이브 상태 카드가 노출되지 않도록 방어 코드 추가 |

### 설계 결정 및 근거

- **강제 새로고침(Force Refresh) 플래그 우회 구조**: API 할당량 소모를 막기 위해 존재하는 60초 쿨타임(`MIN_REFRESH_INTERVAL_MS`) 메커니즘은 유지하되, 사용자의 명시적인 UI 설정 변경 이벤트가 감지되었을 때만 예외적으로 쿨타임을 무시하도록 구현하여 즉각적인 피드백(반응성)을 확보함.
- **채널 ID 빈값에 대한 State null 역산 처리**: 사용자가 채널 연동을 해제(문자열 지움)한 경우 빈 ID로 불필요한 API 네트워크 요청을 보내 에러를 받는 대신, ViewModel 단계에서 조기에 차단(`return@launch`)하고 State의 해당 슬롯을 명시적 `null`로 바꾸어 View(Compose) 단에서 아예 해당 카드 컴포넌트를 그리지 않도록(Visually Hidden) 근본적인 상태 분리 설계를 적용함.

---

## [2026-02-25] refactor(di): DataStore 의존성 주입을 위한 Hilt 모듈 분리

### 목표

`SettingsRepositoryImpl`에서 안드로이드 `Context`를 직접 주입받아 DataStore를 생성하던 방식을 개선하여, 클린 아키텍처 및 유닛 테스트 용이성을 확보하고자 별도의 Hilt 모듈(`DataStoreModule`)을 만들어 `DataStore<Preferences>` 자체를 주입받도록 리팩토링한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `core/data/.../di/DataStoreModule.kt` | **신규** — `ApplicationContext`를 이용해 `DataStore<Preferences>` 싱글톤 인스턴스를 제공(`@Provides`)하는 Hilt 모듈 작성 |
| 2 | `core/data/.../SettingsRepositoryImpl.kt` | 리포지터리 생성자에서 `Context` 대신 `DataStore<Preferences>`를 직접 주입받고, 해당 인스턴스를 사용하도록 교체하여 결합도 최소화 |

### 설계 결정 및 근거

- **테스트 용이성 (Testability) 확보**: 기존에는 Repository 테스트 시 안드로이드 프레임워크의 거대한 `Context` 구조물 전체를 모킹해야 했으나, 이제 `DataStore<Preferences>` 인터페이스만 모킹하면 되므로 유닛 테스트 작성이 간결하고 직관적으로 변경됨.
- **최소 의존성 원칙 및 인터페이스 분리**: Repository 동작 시 실제 필요한 것은 구체적인 저장 매체(`DataStore`)이며 시스템 환경 정보(`Context`)가 아님. 따라서 더 작은 구체적 단위의 객체만 의존성으로 주입받아 객체 지향적 책임을 명확히 함.

---
## [2026-02-25] feat(ui): 앱 드로어 아이콘 배열 하이브리드 전략 구현 (Responsive Design & Customization)

### 목표

다양한 기기 해상도 및 화면 비율에서도 앱 드로어 배열이 깨지지 않도록 가용 영역에 맞춰 동적으로 조절하는 하이브리드 전략(Step 1)과, 사용자가 직접 가로/세로 그리드 개수 및 아이콘 크기를 조절할 수 있는 커스터마이징 기능(Step 2)을 통합하여 구현한다. 이로써 런처 앱의 안정성과 개인화 요구를 동시에 충족시킨다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `docs/app_drawer_icon_layout_strategy.md` | **신규** — 앱 드로어 아이콘 배열 파편화 해결 방안에 대한 하이브리드 전략(Step 1: 자동 적응, Step 2: 설정 추가) 기획 문서 작성 |
| 2 | `core/domain/.../LauncherSettings.kt` | `LauncherSettings`에 `appDrawerGridColumns`, `appDrawerGridRows`, `appDrawerIconSizeRatio` 속성 추가 |
| 3 | `core/data/.../SettingsRepositoryImpl.kt` | 신규 설정(컬럼, 로우, 사이즈 배율)에 대한 Preferences 키 추가 및 `DataStore` 저장/불러오기 로직 반영 |
| 4 | `core/domain/.../SaveAppDrawerSettingsUseCase.kt` | **신규** — 설정값 3가지를 묶어서 Repository에 저장하는 UseCase 클래스 작성 |
| 5 | `feature/launcher/.../ui/SettingsScreen.kt` | 설정 화면 내 '앱 서랍' 탭 신설, 슬라이더 3개(열 개수, 행 개수, 사이즈 배율) 및 초기화, 저장 버튼이 포함된 `AppDrawerSettingsContent` UI 구현 |
| 6 | `feature/apps-drawer/.../AppDrawerScreen.kt` | `AppGridPage` 내부를 `BoxWithConstraints`로 감싸고, 기기 해상도 및 사용자 설정값(`columns`, `rows`, `iconSizeRatio`)에 맞춰 `iconSize`, `textWidth`, `fontSize`를 동적 계산하도록 개선 |
| 7 | `feature/launcher/.../HomeViewModel.kt` 등 | 앱 서랍 설정 Intent(`SaveAppDrawerSettings`) 처리 로직 파이프라인 형성 및 관련 단위 테스트 추가 검증 |

### 설계 결정 및 근거

- **`BoxWithConstraints` 활용**: 페이저 내부의 패딩 등을 고려하여 실제 렌더링 가능한 가용 영역(`maxWidth`, `maxHeight`)을 기반으로 정확히 셀의 크기를 계산하기 위해 채택.
- **동적 최소/최대 제약**: 아이콘 크기가 너무 작아지거나 터치 영역을 침범하지 않도록 `coerceAtLeast(36.dp)` 하한선을 두고, 테블릿 등에서 비정상적으로 커지지 않도록 `minOf(64.dp)` 상한선을 설정하여 시각적 안정성 보장.
- **단방향 데이터 흐름 정립**: 설정 화면에서 값을 변경하고 저장하면 최상위 `HomeViewModel`의 State가 갱신되며 이 상태가 AppDrawer까지 흘러들어가 별도 화면 로딩 없이 즉각 재렌더링(Recomposition)되도록 설계함.
- **UI 슬라이더 안전 범위 제어**: 유저가 무효한 형태의 그리드를 생성하지 않도록 열(3~6), 행(4~8), 사이즈 배율(50%~150%) 등 유효한 범위 안에서 스냅 단위로만 조절하도록 제한하고, `초기화(Reset)` 버튼을 제공해 기본값 접근성을 높임.

---

## [2026-02-25] refactor(ui): Coil 기반의 앱 서랍 아이콘 로딩 렌더링 파이프라인 구축 및 메모리 최적화

### 목표

수십 개의 아이콘 렌더링 시 발생하는 가비지 컬렉션(GC) 및 프레임 드랍(Jank) 이슈를 근본적으로 해결하기 위해, LruCache 수동 캐싱 방식을 버리고 `Coil` 이미지 로더 기반의 커스텀 파이프라인 생태계를 프로젝트에 구축합니다. 추가로 Coil의 내부 String 매퍼와의 원천적인 타입 충돌을 피하기 위해 런처 파이프라인 전용의 강타입 데이터 모델(`AppIconData`)을 도입했습니다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `org/comon/streamlauncher/ui/component/AppIconFetcher.kt` | **신규** — `PackageManager`로 앱 아이콘을 가져오고 Bitmap 가속 포맷으로 넘겨주는 Coil `Fetcher` 및 `Factory` 구현. |
| 2 | `org/comon/streamlauncher/ui/component/AppIconFetcher.kt` | **신규 데이터 타입** — 커스텀 로더만 안전하게 통과시키기 위한 파이프라인 전용 모델 클래스 `AppIconData` 추가. |
| 3 | `core/ui/build.gradle.kts` | `AppIconFetcher` 구현을 위한 `coil` 및 `coil-compose` 의존성 모듈 선언. |
| 4 | `app/.../StreamLauncherApplication.kt` | 글로벌 `ImageLoader`의 `ComponentRegistry` 인스턴스에 `AppIconFetcher.Factory()`를 주입하여 백그라운드 스레드에서 전역 관리하도록 연결. |
| 5 | `core.../ui/component/AppIcon.kt` | 직접 LruCache를 생성, 관리하던 기존 보일러플레이트 코드 완전 제거, `SubcomposeAsyncImage` 단일 컴포저블로 마이그레이션 적용 및 `AppIconData` 전파. |
| 6 | `docs/appdrawer_performance_issue.md` | 현상에 따른 프레임 드랍 이슈 원인과 이 Coil 렌더 파이프라인을 통한 아키텍처 해결 과정을 한 문서로 요약 통합. |
| 7 | `docs/launcher_icon_architecture_plan.md` | **삭제** — 도입 사전 기획 문서를 상기 `#6` 문서의 해결 과정 챕터로 병합한 후 완전히 제거. |

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| 직접 Caching이 아닌 Coil Fetcher 통합 | `Coil`은 `LRU Cache`, 백그라운드 최적화 스레드 풀 운용, `Hardware Accelerate Display` 등 안드로이드 비트맵 제어의 모든 Best Practice를 지원합니다. 이를 우회할 이유가 없습니다. |
| `String` 패키지네임 대신 `AppIconData` 래핑 객체 파라미터 사용 | Coil의 수많은 내부 기본 Mapper(URI, 로컬 File 경로 등 지정)가 단순 String 입력을 가로채면서 `Error` 취급해버려 기본 아이콘이 나오는 치명적인 버그가 발생했기 때문에, 커스텀 Fetcher만 100% 매핑 반응하도록 타입 수준에서 격리시켰습니다. |
| `AsyncImage` 대신 `SubcomposeAsyncImage` 사용 | `AsyncImage`의 `error` 매개변수는 단순 `Painter`만을 입력받기 때문에 복잡한 구조의 UI(예: Surface + Icon 컴포저블)를 대응 아이콘으로 조립할 수 없었으므로 더 높은 UI 커스텀 제어권을 위해 교체했습니다. |

---

## [2026-02-25] bugfix(ui): 피드 화면 비활성 시 LIVE 무한 애니메이션 중단 로직 구현

### 목표

`FeedScreen.kt`의 `LiveStatusCard` 내에 있는 LIVE 배지(네온 숨결 효과) 애니메이션이 `HorizontalPager` 및 `VerticalPager`의 프리렌더링 때문에 화면에 보이지 않을 때에도 무한 반복 실행되며 `setRequestedFrameRate frameRate=NaN` 로그를 매 프레임 남기던 현상을 해결한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `app/.../CrossPagerNavigation.kt` | 화면 노출 여부(`isFeedVisible`)를 계산하여 `feedContent` 람다로 전달 (`verticalPagerState.targetPage == 1 && horizontalPagerState.targetPage == 0`) |
| 2 | `app/.../MainActivity.kt` | `CrossPagerNavigation`에서 전달받은 `isVisible` 파라미터를 `FeedScreen`으로 전달토록 변경 |
| 3 | `feature/launcher/.../ui/FeedScreen.kt` | `FeedScreen`, `FeedContent`, `LiveStatusCard` 컴포저블에 `isVisible: Boolean` 파라미터 추가 (`LiveStatusCard`까지 상태 파이프라인 형성) |
| 4 | `feature/launcher/.../ui/FeedScreen.kt` | `LiveStatusCard` 내부의 `rememberInfiniteTransition` 블록을 `if(isVisible)` 조건문으로 감싸 노출될 때만 무한 애니메이션을 구동시키고 아니면 정적 알파(`1.0f`)를 반환하도록 처리 |

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `targetPage` 기반 렌더링 검사 | 현재 보고 있는 페이지이거나 곧 볼 예정인 페이지만 애니메이션하기 적합. `currentPage`를 사용할 경우 스크롤 중 애니메이션이 뒤늦게 활성화될 우려가 있음. |
| `LiveStatusCard`에 상태 의존성 주입 | 컴포저블 트리 최상단(`CrossPagerNavigation`)에서 판단한 가시성(Visibility) 값을 단방향 데이터 흐름을 통해 단방향으로 명시적으로 전달하여 의존성 및 생명주기를 투명하게 만듦. |

### 성능 최적화 효과

화면에 보이지 않는 상태에서도 `rememberInfiniteTransition`이 계속 동작하면 안드로이드의 `dispatchDraw()`가 단말기 주사율에 맞춰 매 프레임(초당 60~120회) 강제 호출됩니다. 이 현상을 해결함으로써 얻어지는 직접적인 이점은 다음과 같습니다.
1. **배터리 수명 및 디바이스 발열 방지**: 불필요한 GPU/CPU 렌더링 파이프라인 호출 방지
2. **UI 스레드 부하 감소**: 앱 서랍과 설정 화면에서의 스크롤링 및 네비게이션 애니메이션 프레임 드랍(버벅임)을 예방
3. **디버그 환경 개선**: `setRequestedFrameRate frameRate=NaN`과 같은 무의미한 로그 스팸이 제거되어 정상적인 로그캣 가독성 확보

---

## [2026-02-24] investigation: setRequestedFrameRate(NaN) 로그 매 프레임 출력 현상 원인 분석

### 발견한 현상

런처 앱을 실행하면 로그캣에서 아래와 같은 `setRequestedFrameRate frameRate=NaN` 로그가 **매 프레임마다** 빠르게 출력되는 현상을 발견했다.

```
View  I  setRequestedFrameRate frameRate=NaN, this=android.view.View{...}
         caller=androidx.compose.ui.platform.Api35Impl.setRequestedFrameRate
                androidx.compose.ui.platform.AndroidComposeView.dispatchDraw
```

- **홈 화면, 설정 화면, 피드 화면, 앱 드로어 화면**: 가만히 있어도 로그가 빠르게 연속 출력됨
- **위젯 화면**: 로그가 출력되지 않음 (편집 모드가 아닌 한)
- Compose BOM을 `2026.02.00`으로 업데이트해도 동일하게 발생

### 원인 분석

`FeedScreen.kt`의 `LiveStatusCard`에 **LIVE breathing 무한 애니메이션**이 존재한다:

```kotlin
// FeedScreen.kt — LiveStatusCard 내부
val infiniteTransition = rememberInfiniteTransition(label = "liveBreathing")
val breathAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 1200),
        repeatMode = RepeatMode.Reverse,
    ),
)
```

이 `rememberInfiniteTransition`은 컴포지션이 유지되는 한 **무한 반복**으로 매 프레임 `dispatchDraw()`를 유발한다. `dispatchDraw()` 호출 시 Compose 내부의 `Api35Impl.setRequestedFrameRate()`가 실행되며, 이때 프레임 레이트 값이 `NaN`으로 전달되어 로그가 출력된다.

핵심은 `CrossPagerNavigation.kt`의 Pager 설정에 있다:

```kotlin
HorizontalPager(
    beyondViewportPageCount = 1,  // 현재 페이지 ± 1페이지를 미리 렌더링
)
```

`beyondViewportPageCount = 1` 설정으로 인해 **현재 보고 있는 페이지의 인접 페이지**가 항상 프리렌더링(Pre-compose)된다. FeedScreen은 `HorizontalPager`의 **page 0**에 위치하므로:

| 현재 위치 | HorizontalPager 프리렌더링 범위 | FeedScreen (page 0) 포함 여부 |
|-----------|-------------------------------|:---:|
| 홈 (page 1) | page 0, page 2 | ✅ 포함 → 무한 애니메이션 실행 중 |
| 위젯 (page 2) | page 1 만 | ❌ 범위 밖 → 애니메이션 미실행 |

또한 `VerticalPager`도 `beyondViewportPageCount = 1`이므로:
- **설정 화면** (vertical page 0) → CenterRow (page 1) 프리렌더링 → FeedScreen 활성 → 로그 출력
- **앱 드로어** (vertical page 2) → CenterRow (page 1) 프리렌더링 → FeedScreen 활성 → 로그 출력

위젯 화면만 FeedScreen과 **2페이지 이상 떨어져 있어** 프리렌더링 범위 밖이므로 무한 애니메이션이 활성화되지 않고, 결과적으로 매 프레임 draw가 발생하지 않아 로그가 출력되지 않는다.

### 현재 상태

원인 파악만 완료한 상태이며, 코드 수정은 아직 진행하지 않음. 향후 개선 방향으로는 FeedScreen이 실제로 화면에 보이지 않을 때 무한 애니메이션을 중단하는 방법(예: `PagerState.currentPage` 기반 조건부 애니메이션) 등을 검토할 예정.

---

## [2026-02-24] feat(apps-drawer): 앱 드로어 스크롤 방식을 HorizontalPager 그리드로 변경

### 목표

앱 드로어 화면의 스크롤 방식을 기존 세로 리스트(`LazyColumn`)에서 좌우 스와이프 페이지 전환(`HorizontalPager`) 방식으로 변경한다. 한 페이지에 4(가로) × 6(세로) = 24개의 앱을 아이콘 그리드 형태로 배치하며, 왼쪽 상단부터 오른쪽 방향으로 배치 후 한 줄씩 내려가는 순서로 앱을 표시한다.

### 변경 사항

| # | 모듈 | 파일 | 작업 |
|---|------|------|------|
| 1 | `feature/apps-drawer` | `ui/AppDrawerScreen.kt` | `LazyColumn` 기반 세로 리스트 → `HorizontalPager` + 4×6 그리드 레이아웃으로 전면 교체 |
| 2 | `feature/apps-drawer` | `ui/AppDrawerScreen.kt` | `AppDrawerItem` (Row: 아이콘+텍스트 가로 배치) → `AppDrawerGridItem` (Column: 아이콘 위 + 앱 이름 아래 세로 배치) 컴포저블 이름 및 레이아웃 변경 |
| 3 | `feature/apps-drawer` | `ui/AppDrawerScreen.kt` | `AppGridPage` private 컴포저블 신규 추가 — 한 페이지의 4열×6행 그리드 배치, `Column > Row > Box` 중첩 구조로 균등 분배 |
| 4 | `feature/apps-drawer` | `ui/AppDrawerScreen.kt` | 페이지 인디케이터(dot indicator) 추가 — 2페이지 이상일 때 하단에 현재 페이지 위치 표시, `Canvas`로 원형 그리기, `accentPrimary` 컬러 사용 |

### 구조

```
AppDrawerScreen
├─ OutlinedTextField (검색바, 기존 유지)
├─ HorizontalPager (weight=1f)
│   └─ AppGridPage (페이지별)
│       └─ Column (6행)
│           └─ Row (4열, weight 균등)
│               └─ Box (contentAlignment = Center)
│                   └─ AppDrawerGridItem
│                       ├─ AppIcon (48dp)
│                       └─ Text (11sp, maxLines=1, ellipsis, width=64dp)
└─ Row (페이지 인디케이터, pageCount > 1일 때만)
    └─ Canvas × pageCount (선택: 8dp, 비선택: 6dp)
```

- 페이지 분할: `filteredApps`를 24개씩 `ceil(size / 24)` 페이지로 분할
- 배치 순서: `appIndex = rowIndex * COLUMNS + colIndex` (행 우선, 왼→오른, 위→아래)
- 검색 기능: 기존과 동일하게 유지, 필터링된 결과도 페이지 단위로 표시
- 드래그앤드롭: 기존 `detectDragGesturesAfterLongPress` 로직 완전 보존

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `LazyVerticalGrid` 대신 `Column > Row` 고정 레이아웃 사용 | 한 페이지 최대 24개(4×6) 고정 아이템이므로 스크롤 불필요. 고정 레이아웃으로 `weight(1f)` 기반 균등 분배가 가능하여 기기 해상도에 독립적인 크기 조정 가능 |
| 아이콘 크기 48dp + 앱 이름 너비 64dp | 4열 그리드에서 아이콘이 충분히 식별 가능한 크기이면서, 앱 이름이 너무 길어져 다른 열을 침범하지 않도록 고정 너비 제한 |
| 페이지 인디케이터를 `Canvas` 직접 그리기로 구현 | 외부 라이브러리 의존성 없이 간단한 dot indicator 구현. `accentPrimary` 테마 컬러와 자연스럽게 통합 |
| `rememberPagerState(pageCount = { pageCount })` 람다 방식 | 검색 결과에 따라 `filteredApps` 크기가 동적으로 변하므로, 리컴포지션 시 pageCount가 자동 갱신되도록 람다 형태로 전달 |
| 상수 `COLUMNS=4`, `ROWS=6`, `ITEMS_PER_PAGE=24` 파일 상수로 정의 | 매직 넘버 방지 및 향후 레이아웃 변경 시 한 곳만 수정하면 되도록 상수화 |

---

## [2026-02-24] feat(home): 홈 화면 그리드 편집 모드 (앱 삭제, 순서 변경 준비, 페이저 스크롤 잠금)

### 목표

홈 화면 그리드 셀 내에서 아이콘을 길게 누르면 편집 모드에 진입하도록 구현한다. 편집 모드에서는 흔들림 애니메이션 효과가 나타나며, X 버튼을 눌러 선택한 앱을 셀에서 삭제할 수 있다. 또한, 향후 드래그 앤 드롭을 통한 앱 순서 변경을 위해 코어 로직과 UI 기반을 다듬고, 편집 중 의도치 않은 화면 스와이프를 방지하기 위해 네비게이션 `userScrollEnabled`를 차단한다.

### 변경 사항

| # | 모듈 | 파일 | 작업 |
|---|------|------|------|
| 1 | `feature/launcher` | `HomeContract.kt` | `HomeState`에 `editingCell: GridCell?` 파라미터 추가; `HomeIntent`에 `SetEditingCell` 및 `MoveAppInCell` 등 편집 관련 인텐트 추가 |
| 2 | `feature/launcher` | `HomeViewModel.kt` | `distributeApps`에서 알파벳 자동 정렬 로직 해제하여 앱 순서 보존 활성화; `SetEditingCell`, `MoveAppInCell` 처리 핸들러 추가 |
| 3 | `feature/launcher` | `ui/HomeScreen.kt` | `BackHandler`를 사용해 뒤로가기 시 편집 모드 종료; `GridAppItem`을 롱클릭 시 편집 모드 진입(`SetEditingCell`); 편집 상태 시 `animateFloatAsState`를 이용해 화면 흔들림(Wiggle) 효과 구현; X 버튼 영역 구현 (`IconButton(Close)`); 앱 내 드래그 앤 드롭 준비를 위한 `draggable` 추가 |
| 4 | `app` | `navigation/CrossPagerNavigation.kt` | `CrossPagerNavigation` 쪽에 `isHomeEditMode: Boolean` 인자 추가; 편집 중일 때 좌우 `HorizontalPager` 및 상하 `VerticalPager` 스와이프 차단 |
| 5 | `app` | `MainActivity.kt` | `HomeScreen`의 `editingCell != null` 여부에 따라 `CrossPagerNavigation`에 `isHomeEditMode` 전달 |

### 설계 결정 및 근거

- **페이저 스크롤 차단 (userScrollEnabled 차단):** 홈 화면과 그리드 인터페이스 특성상 아이콘 롱클릭 및 드래그 동작이 스와이프 제스처와 충돌할 가능성이 매우 높으므로, 아이콘이 흔들리고 X 버튼이 나온 편집 모드 상태에서는 아예 주변 Feed/App Drawer로의 스크롤을 무효화하여 안전한 편집 경험을 보장함.
- **편집 모드(`Wiggle` 애니메이션):** iOS 방식의 흔들리는 애니메이션을 적용하여 사용자가 시각적으로 즉시 현재 편집 중 상태임을 알 수 있도록 직관적 UX 부여.
- **뒤로가기와 외부 영역 터치:** 편집 중에 시스템 '뒤로가기' 버튼을 누르거나 셀 바깥 빈 공간을 터치하면 편집 모드가 자연스럽게 종료되도록 예외 처리 구현.

## [2026-02-24] bugfix(navigation): 앱 드로어에서 드래그 앤 드롭 취소 및 앱 사라짐 현상 수정

### 발생한 버그

앱 드로어 화면에서 앱을 길게 눌러(Long Press) 홈 화면으로 드래그 앤 드롭하려고 하면, 앱 드로어 화면에서 홈 화면으로 전환되는 애니메이션 도중에 드래그 중이던 앱 아이콘이 사라지고 제스처가 강제 취소되는 현상이 발생했다.

### 원인 분석

`CrossPagerNavigation.kt`에서 화면 전환을 담당하는 `VerticalPager`와 `HorizontalPager`의 `beyondViewportPageCount` 옵션이 0으로 설정되어 있었다.
이로 인해 앱을 드래그하여 앱 드로어(Page 2)에서 홈 화면(Page 1)으로 페이저 스크롤이 트리거되면, 화면 밖으로 벗어나는 앱 드로어의 UI 컴포넌트가 메모리에서 해제(Dispose)되었다.
UI 컴포넌트가 해제됨에 따라 `AppDrawerItem` 컴포저블에 바인딩된 `pointerInput` 제스처 감지 코루틴도 함께 취소되어, 결과적으로 드래그 상태가 초기화(`cancelDrag()`)된 것이 원인이었다.

### 해결 방법

화면 전환 중에도 컴포지션이 즉시 해제되지 않도록 보장하여 터치 이벤트 스트림이 끊어지지 않게 수정했다.

| # | 파일 | 작업 |
|---|------|------|
| 1 | `app/.../CrossPagerNavigation.kt` | `VerticalPager`와 `HorizontalPager`의 `beyondViewportPageCount` 값을 0에서 1로 변경하여, 스크롤 애니메이션 중에도 인접 페이지의 상태와 제스처가 유지되도록 반영 |

---
## [2026-02-24] feat(ui): 시스템 배경화면 투명화 및 위젯 편집 모드 조건부 글래스 이펙트

### 목표

런처 전반의 자체 배경 이미지 대신 안드로이드 시스템 배경화면이 투명하게 비치도록 `windowShowWallpaper=true` 테마 속성을 적용한다. 또한, 위젯 화면은 기본적으로 투명한 상태를 유지하여 시스템 바탕화면 전체가 잘 보이게 하되, 롱클릭으로 "편집 모드(Edit Mode)"에 진입했을 때만 어두운 글래스 이펙트 처리(투명도 0.55)가 적용되어 위젯 배치에 집중할 수 있도록 개선한다. 기존 커스텀 배경화면 관리 코드는 모두 제거한다.

### 변경 사항

| # | 모듈 | 파일 | 작업 |
|---|------|------|------|
| 1 | `app` | `res/values/themes.xml` | `Theme.StreamLauncher`에 `android:windowBackground = @android:color/transparent` 및 `android:windowShowWallpaper = true` 속성 추가로 시스템 바탕화면 투과 허용 |
| 2 | `app` | `res/values-night/themes.xml` | 다크 테마에도 동일하게 투명화 및 바탕화면 표시 속성 추가 |
| 3 | `app` | `CrossPagerNavigation.kt` | 기존 커스텀 배경 이미지 표시용 뷰(`WallpaperLayer`) 삭제. `RightPage`(위젯 화면)에 `isWidgetEditMode` 상태 파라미터를 추가하여 켜져있을 때만 글래스 반투명 배경 테두리가 활성화 되도록 분기 처리 |
| 4 | `app` | `MainActivity.kt` | 더 이상 사용하지 않는 `wallpaperImage` 관련 옵저빙 및 전달부 제거. 대신, `widgetViewModel.uiState.isEditMode`를 관찰하여 `CrossPagerNavigation`으로 파라미터 전달 |
| 5 | `feature/widget` | `WidgetViewModel.kt` | 위젯 화면 내부에 고립되어 있던 `isEditMode` 상태를 뷰모델의 `uiState`인 `WidgetState`로 끌어올림(State Hoisting)하여 상위의 `MainActivity` 및 `CrossPagerNavigation`이 이를 구독하도록 개편 |
| 6 | `feature/widget` | `WidgetScreen.kt` | 뷰모델에서 관리되는 `isEditMode`와 그 변경을 위한 이벤트 람다(`onEditModeChange`)를 받아 사용하도록 수정 |
| 7 | `feature/launcher` | `SettingsScreen.kt` | 설정 화면의 기존 "배경화면" 설정을 삭제하고, 버튼 누를 시 시스템 배경화면 픽커 액티비티(`Intent.ACTION_SET_WALLPAPER`)를 직접 호출하도록 간소화 |
| 8 | `feature/launcher` | `HomeViewModel.kt` 등 | `SetWallpaperImage` 유즈케이스와 `wallpaperImage` 영속성 보관 등의 불필요해진 이전 코드 삭제, Enum `SettingsTab.WALLPAPER` 제거 | 

### 설계 결정 및 근거

- **커스텀 배경 스크롤 패럴렉스 로직 삭제:** 이전의 방식은 배경 이미지를 앱 안에서 그리기 때문에 진정한 시스템 바탕화면과 괴리가 있었음. 시스템 바/내비 바 지원의 완벽한 일체감을 주고 불필요한 이미지 렌더 메모리 점유를 줄이기 위해 제거.
- **`isEditMode` 호이스팅 (WidgetScreen → ViewModel):** 이전에는 편집 모드 상태가 `WidgetScreen` 내부에 종속적이었으나, 부모 레이어인 내비게이션 `RightPage` 단에서 글래스 이펙트 모서리를 그렸기 때문에 렌더링에 필요한 상태 파라미터를 위로 전달할 필요가 생겨 MVI 방식에 맞춰 뷰모델의 State로 승격시킴.
- **편집 모드 조건부 글래스 배경:** 대부분 런처 시스템에서는 페이지 이동 시 위젯 페이지 전체의 아름다움(바탕화면 풀뷰)이 중시되지만, 실제 아이템의 사이즈 조정이나 이동(편집) 작업 중에는 배경이 너무 밝거나 복잡하면 방해가 되므로 편집 모드일 때만 반투명 검은 계열로 디밍 처리함.

---

## [2026-02-24] bugfix(navigation): 홈 화면 뒤로가기 시 런처 종료 현상 수정

### 발생한 버그

에뮬레이터나 실기기에서 StreamLauncher를 기본 홈 앱으로 설정한 후, 홈 화면(중앙 화면)에서 디바이스의 '뒤로가기' 버튼을 클릭하면 앱이 종료되면서 기존의 구글(또는 단말기) 기본 런처로 튕겨버리는 현상이 발생했다.

### 원인 분석

`CrossPagerNavigation.kt` 내부에서 `BackHandler(enabled = !isAtCenter)`로 설정되어 있었다.
런처 앱의 경우 피드나 앱 서랍 등 주변부 화면(`!isAtCenter`)에서는 뒤로가기 시 중앙(홈 화면)으로 이동하는 것이 맞지만, 이미 중앙 화면(`isAtCenter`)에 있을 때는 백 버튼 이벤트 처리를 비활성화(`enabled = false`)하고 있었다.
결과적으로 중앙 화면에서는 핸들링되지 않은 백 버튼 이벤트가 상위 액티비티로 전달되어 앱이 종료(`finish()`)되는 안드로이드 기본 동작이 수행된 것이다.

### 해결 방법

홈 화면(중앙)에 있을 때도 `BackHandler`가 활성화되도록 변경하여, 앱 종료 이벤트를 가로채고(consume) 시스템에 의해 런처가 강제 종료되는 것을 방지했다.

| # | 파일 | 작업 |
|---|------|------|
| 1 | `app/.../CrossPagerNavigation.kt` | `BackHandler(enabled = true)`로 변경. 중앙 화면(`currentPage == 1 && horizontalPagerState.currentPage == 1`)일 때는 페이저 이동 등 별도의 액션을 취하지 않고 이벤트만 소비하도록 처리. |

---

## [2026-02-24] bugfix(navigation): 설정화면 스와이프 시 앱 드로어로 강제 스크롤(Jump) 현상 수정

### 발생한 버그

앱의 설정 화면(Page 0)에서 화면을 아래로 살짝 드래그(위로 스와이프)할 경우, 홈 화면(Page 1)을 건너뛰고 앱 드로어 화면(Page 2)으로 강제로 빠르게 스크롤되며 넘어가는 버그가 발생했다. 특히 처음 앱을 실행한 직후에 이 현상이 100% 재현되었다.

### 원인 분석

십자형 내비게이션(`CrossPagerNavigation`)은 Jetpack Compose Foundation의 `VerticalPager`를 기반으로 작동한다. Pager는 스크롤 애니메이션의 부드러움을 위해 현재 화면에 완전히 나타나지 않더라도 **인접한 페이지를 미리 렌더링(Pre-compose)**하는 특징이 있다.

앱 드로어 화면(`AppDrawerScreen`) 내부에는 화면 진입 시 사용자 편의를 위해 검색창에 자동으로 포커스를 주고 키보드를 띄우는 코드가 존재했다:
```kotlin
LaunchedEffect(Unit) {
    focusRequester.requestFocus()
}
```

설정 화면(Page 0)에서 홈 화면(Page 1) 방향으로 스와이프를 시작하는 순간, `VerticalPager`는 스크롤 준비를 위해 앱 드로어(Page 2)를 미리 렌더링하기 시작한다. 이때 화면 상에는 앱 드로어가 전혀 보이지 않지만, 내부적으로 로딩되면서 위 `LaunchedEffect` 블록이 실행되어 버린다.
갑작스럽게 스크린 밖의 입력창이 포커스를 요청하게 되면서, 안드로이드/Compose 포커스 시스템은 **"현재 포커스된 입력창을 사용자에게 보여주어야 한다"**고 판단하여 즉시 앱 드로어(Page 2) 위치로 화면 전체를 강제 스크롤(Jump)시켜버리는 것이 근본 원인이었다.

### 해결 방법

앱 드로어 진입 시 검색창에 자동 포커스를 주는 기능 자체를 완전히 제거하여, 의도치 않은 포커스 요청으로 인한 스크롤 간섭을 방지했다.

| # | 파일 | 작업 |
|---|------|------|
| 1 | `feature/apps-drawer/.../ui/AppDrawerScreen.kt` | `FocusRequester` 인스턴스 변수 및 입력창(`OutlinedTextField`)의 `.focusRequester(focusRequester)` 모디파이어 선언부 삭제 |
| 2 | `feature/apps-drawer/.../ui/AppDrawerScreen.kt` | 컴포저블 렌더링 직후 무조건 실행되던 `LaunchedEffect` 내 `focusRequester.requestFocus()` 호출부 완전 삭제 |

---
## [2026-02-24] refactor(launcher): 홈 초기 진입 클린 아키텍처 및 MVI 패턴 리팩터링

### 목표

초기 앱 실행 시 기본 홈 앱 설정 화면으로 유도하는 로직(commit `53fd7a379cb2cb3fe0059e25453774ffe5559114`)이 `MainActivity`에 직접 비즈니스 로직으로 구현되어 있던 문제를 클린 아키텍처와 MVI 패턴에 맞게 리팩터링한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `core/domain/usecase/CheckFirstLaunchUseCase.kt` | **신규** — 첫 실행 여부 확인 로직 캡슐화 |
| 2 | `core/domain/usecase/SetFirstLaunchUseCase.kt` | **신규** — 첫 실행 상태 저장 로직 캡슐화 |
| 3 | `core/domain/usecase/SaveWallpaperImageUseCase.kt` | **신규** — 바탕화면 이미지 저장 로직 캡슐화 |
| 4 | `feature/launcher/.../HomeContract.kt` | `HomeIntent.CheckFirstLaunch`, `HomeSideEffect.NavigateToHomeSettings` 추가 |
| 5 | `feature/launcher/.../HomeViewModel.kt` | `SettingsRepository` 직접 의존성 제거, UseCase 주입 및 `handleIntent(HomeIntent.CheckFirstLaunch)` 구현 |
| 6 | `feature/launcher/.../HomeViewModelTest.kt` | `SettingsRepository` 모킹 파트와 주입부를 UseCase로 변경하여 테스트 코드 갱신 |
| 7 | `app/.../MainActivity.kt` | `onCreate` 부에서 비즈니스 로직 제거, `viewModel.handleIntent(HomeIntent.CheckFirstLaunch)` 호출 및 `LaunchedEffect` 내 `HomeSideEffect.NavigateToHomeSettings` 처리 추가 |

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| 비즈니스 로직을 `MainActivity`에서 `HomeViewModel`과 `UseCase`로 분리 | 안드로이드 핵심 컴포넌트인 Activity는 UI 렌더링과 사용자 인터랙션 처리, OS와의 통신(Intent 등) 책임만 가져야 한다. |
| `SettingsRepository` 직접 의존 제거 | UI 계층인 ViewModel이 데이터/도메인 계층 Repository를 직접 참조하는 대신 UseCase를 통해 간접 접근하도록 강제하여 클린 아키텍처 원칙 준수 |
| `HomeSideEffect`를 통한 내비게이션 | MVI 패턴을 준수하여 ViewModel이 상태(State)와 부수 효과(SideEffect)로만 UI에 이벤트를 전달하도록 구성 |

---

## [2026-02-24] Step 22: 기본 런처 등록 및 시스템 최적화

### 목표

기본 홈 앱(Launcher)으로서의 완성도를 높이기 위해 시스템 설정 연결, Edge-to-Edge 투명화, 하단 그라데이션 스크림, Coil 메모리 캐시 제한, WorkManager 피드 갱신, LeakCanary 통합을 구현한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `feature/launcher/.../SettingsScreen.kt` | "기본 홈 앱" 버튼 추가 — `Settings.ACTION_HOME_SETTINGS` 호출, API 28 대응(`resolveActivity()` 체크 + `ActivityNotFoundException` try-catch), fallback `ACTION_SETTINGS` |
| 2 | `app/src/main/res/values/themes.xml` | 시스템 바 투명 + `windowLightStatusBar/NavigationBar=true` (라이트: 어두운 아이콘) |
| 3 | `app/src/main/res/values-night/themes.xml` | **신규** — 다크 모드 테마, `windowLightStatusBar/NavigationBar=false` (밝은 아이콘) |
| 4 | `app/.../MainActivity.kt` | `enableEdgeToEdge()` → `SystemBarStyle.auto(transparent, transparent)` + `detectDarkMode` 람다로 Light/Dark 아이콘 자동 전환 |
| 5 | `app/.../navigation/CrossPagerNavigation.kt` | 홈 페이지(page 1) 하단 48dp 그라데이션 스크림 추가 — `Brush.verticalGradient(Transparent → Black 15% alpha)` |
| 6 | `app/.../StreamLauncherApplication.kt` | `ImageLoaderFactory` 구현 (메모리 15%, 디스크 50MB, crossfade); `Configuration.Provider` + `HiltWorkerFactory` 주입; `scheduleFeedSync()` — OneTimeWorkRequest(최초 1회) + PeriodicWorkRequest(6시간 주기) |
| 7 | `app/src/main/AndroidManifest.xml` | WorkManager 기본 초기화 비활성화 provider 추가 (`tools:node="remove"`) |
| 8 | `gradle/libs.versions.toml` | `workRuntimeKtx`, `hiltWork`, `leakcanary` 버전 및 라이브러리 추가 |
| 9 | `app/build.gradle.kts` | WorkManager, hilt-work, LeakCanary(debugImplementation) 의존성 추가 |
| 10 | `core/data/build.gradle.kts` | WorkManager, hilt-work 의존성 추가 |
| 11 | `core/data/.../worker/FeedSyncWorker.kt` | **신규** — `@HiltWorker` + `FeedRepository`/`SettingsRepository` DI 주입, `getIntegratedFeed()` 호출, 최대 3회 retry |

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `resolveActivity()` + `ActivityNotFoundException` 이중 방어 | API 28 이하·일부 OEM에서 `ACTION_HOME_SETTINGS` 미지원 시 크래시 방지; fallback으로 일반 설정 화면 진입 |
| `values-night/themes.xml` 분리 | 라이트 모드(어두운 아이콘) vs 다크 모드(밝은 아이콘) — 배경색에 묻히지 않도록 `windowLightStatusBar` 분기 |
| 하단 48dp 그라데이션 스크림 | `navigationBarsPadding`만으로는 밝은 배경화면에서 내비게이션 바 아이콘 가독성 저하; 15% alpha 옅은 스크림으로 보완 |
| OneTimeWorkRequest + PeriodicWorkRequest 병행 | 최초 설치 시 캐시가 비어있을 수 있으므로 즉시 1회 실행; 6시간 주기는 배터리 효율 유지 |
| `HiltWorkerFactory` + Manifest WorkManagerInitializer 제거 | WorkManager가 Hilt로 `FeedSyncWorker`를 생성하려면 커스텀 `Configuration.Provider`와 `tools:node="remove"` 필수 |
| LeakCanary debugImplementation | 릴리즈 빌드 미포함; 드래그 앤 드롭(Step 19) Modifier·람다 캡처 부분 중점 점검 대상으로 devlog에 명시 |

---

## [2026-02-23] feat(home): 홈 그리드 아이콘 레이아웃 + 수동 앱 배치

### 목표

홈 화면 4개 그리드 영역을 클릭(확장)했을 때 기존 `LazyColumn` 텍스트 리스트 대신 **2열×3행 앱 아이콘 그리드**를 표시한다. 각 셀에는 최대 6개의 앱을 배치할 수 있으며, 앱은 사용자가 앱 드로어에서 직접 드래그 앤 드롭으로 배치한다. 기존의 설치 앱 자동 4등분 배분 로직을 제거하여, 초기 상태에서 셀이 비어 있도록 변경한다.

### 구조

```
GridCellContent (확장 시)
├─ 배경 이미지 (AsyncImage, 있을 경우)
├─ 반투명 오버레이
└─ 2×3 Grid (Column > Row)
    ├─ Row: [AppItem 1] [AppItem 2]
    ├─ Row: [AppItem 3] [AppItem 4]
    └─ Row: [AppItem 5] [AppItem 6]

GridAppItem:
  Column(horizontalAlignment = Center)
  ├─ AppIcon (weight=1, aspectRatio=1)
  └─ Text (labelSmall, maxLines=1, ellipsis)
```

- 배치 순서: 행 우선 (왼→오른, 위→아래) — `index = row * 2 + col`
- 아이콘 사이 패딩: 5dp
- 아이콘 크기: `weight(1f)` + `aspectRatio(1f)`로 그리드에 딱 맞게 자동 조정
- 클릭: 앱 실행, 롱클릭: 핀 해제 (기존 동작 유지)

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `core/ui/.../component/AppIcon.kt` | **신규** — `feature/apps-drawer`에서 `core:ui`로 이동. 패키지 `org.comon.streamlauncher.ui.component`. 양쪽 feature 모듈에서 공유 가능 |
| 2 | `core/ui/build.gradle.kts` | `compose.foundation` 의존성 추가 (`Image` composable용) |
| 3 | `feature/apps-drawer/.../ui/AppIcon.kt` | **삭제** — `core:ui`로 이동됨 |
| 4 | `feature/apps-drawer/.../ui/AppDrawerScreen.kt` | `AppIcon` import 경로를 `ui.component.AppIcon`으로 변경 |
| 5 | `app/.../CrossPagerNavigation.kt` | `AppIcon` import 경로를 `ui.component.AppIcon`으로 변경 |
| 6 | `feature/launcher/.../HomeContract.kt` | `HomeState`에 `allApps: List<AppEntity>` 필드 추가 — 설치된 전체 앱 목록을 셀 배치와 분리 보관 |
| 7 | `feature/launcher/.../HomeViewModel.kt` | `distributeApps()`: 미할당 앱 4등분 자동 배분 로직 완전 제거, `cellAssignments` 기반 핀 고정 앱만 배치; `loadApps()`: `allApps`에 전체 앱 저장; `resetHome()`·검색 디바운스·`assignAppToCell()`·`unassignApp()`: `allApps` 기반으로 전환; `assignAppToCell()`에 `MAX_APPS_PER_CELL = 6` 제한 추가; `companion object`에 상수 정의 |
| 8 | `feature/launcher/.../ui/HomeScreen.kt` | `LazyColumn` + `Text` 제거 → 2열×3행 `Column > Row` 아이콘 그리드로 교체; `GridAppItem` private composable 추가 (아이콘 + 앱이름); `MAX_APPS_PER_CELL`, `GRID_COLUMNS`, `GRID_ROWS` 파일 상수 정의; 미사용 import 정리 (`LazyColumn`, `items`, `Icons.Star`, `Icon`, `Spacer`, `size`, `width`); 신규 import 추가 (`aspectRatio`, `fillMaxHeight`, `TextOverflow`, `AppIcon`) |
| 9 | `feature/launcher/.../HomeViewModelTest.kt` | 테스트 #2: 자동 배분 → `allApps` 저장 + 셀 비어있음 검증으로 변경; 테스트 #8: 균등 배분 → 초기 셀 비어있음 + 할당 후 표시 검증으로 변경; 테스트 #29: 자동 배분 포함 → 할당 앱만 셀에 존재 검증으로 변경; 테스트 #31 신규: 7번째 앱 할당 시 `MAX_APPS_PER_CELL` 제한으로 무시되는지 검증 |

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `LazyColumn` → 고정 `Column > Row` 그리드 | 최대 6개 고정 아이템이므로 스크롤 불필요. 고정 레이아웃이 크기 계산에 유리 |
| `weight(1f)` + `aspectRatio(1f)` 아이콘 크기 | 그리드 셀 크기에 자동 맞춤, 기기 해상도 독립적 |
| `AppIcon`을 `core:ui`로 이동 | `feature:launcher`와 `feature:apps-drawer` 양쪽에서 사용. 모듈 간 교차 의존 방지 |
| `allApps` 필드 분리 | 자동 배분 제거 후 `appsInCells`에 전체 앱이 없으므로, 앱 드로어 검색·필터링을 위한 별도 전체 목록 필요 |
| 자동 배분 제거 | 6칸 그리드에 수십 개 앱을 자동 배치하면 의미 없는 앱이 표시됨. 사용자가 원하는 앱만 직접 배치하는 것이 런처 UX에 적합 |
| `MAX_APPS_PER_CELL = 6` 제한 | 2×3 그리드에 맞는 물리적 한계. ViewModel에서 할당 시점에 체크하여 초과 방지 |

---

## [2026-02-23] feat(wallpaper): HorizontalPager 패럴랙스 배경화면

### 목표

Android 런처 앱처럼 하나의 배경화면을 지정하고, Feed·Home·Widget 3개 화면을 스크롤할 때 같은 배경화면을 공유하면서 스크롤 방향으로 배경이 함께 움직이는 패럴랙스 효과를 구현한다. 기존 피드 전용 `feedBackgroundImage`를 공유 `wallpaperImage`로 전환하고, 설정 화면에 별도 배경화면 탭을 추가한다.

### 구조

```
HorizontalPager (3 pages)
├─ [0] LeftPage (Feed)
│   └─ WallpaperLayer(pageIndex=0) → glass effect → feedContent
├─ [1] Home
│   └─ WallpaperLayer(pageIndex=1) → homeContent
└─ [2] RightPage (Widget)
    └─ WallpaperLayer(pageIndex=2) → glass effect → widgetContent

WallpaperLayer:
  AsyncImage(wallpaper, requiredWidth = screenWidth * 3, ContentScale.Crop)
  translationX = (1 - pageIndex) * screenWidthPx
```

- 각 페이지 내부에 동일한 wallpaper 이미지를 3배 너비로 배치
- `pageIndex`에 따라 `translationX`로 해당 페이지에 맞는 1/3 영역을 표시
- 부모 Box의 `clipToBounds()`로 페이지 영역만 잘라냄
- 스크롤 시 페이지가 이동하면서 인접 wallpaper가 연속 이어짐

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `core/domain/.../LauncherSettings.kt` | `feedBackgroundImage` → `wallpaperImage` 리네이밍 |
| 2 | `core/domain/.../SettingsRepository.kt` | `setFeedBackgroundImage()` → `setWallpaperImage()` |
| 3 | `core/data/.../SettingsRepositoryImpl.kt` | 메서드명 변경, DataStore key를 `"launcher_background_image"`로 변경 |
| 4 | `feature/launcher/.../HomeContract.kt` | `HomeState.feedBackgroundImage` → `wallpaperImage`, `HomeIntent.SetFeedBackgroundImage` → `SetWallpaperImage` |
| 5 | `feature/launcher/.../HomeViewModel.kt` | 리네이밍 반영 (`setWallpaperImage`, `settingsRepository.setWallpaperImage`) |
| 6 | `feature/launcher/.../FeedContract.kt` | `FeedState.feedBackgroundImage` 필드 제거 |
| 7 | `feature/launcher/.../FeedViewModel.kt` | settings 수집에서 `feedBackgroundImage` 제거 |
| 8 | `feature/launcher/.../FeedScreen.kt` | 개별 배경 이미지 레이어(AsyncImage + blur) 제거 |
| 9 | `feature/launcher/.../model/SettingsTab.kt` | `WALLPAPER` enum 항목 추가 |
| 10 | `feature/launcher/.../ui/SettingsScreen.kt` | `FeedSettingsContent`에서 배경 이미지 UI 전체 제거; `MainSettingsContent`에 "배경화면" 버튼 추가 → `SettingsTab.WALLPAPER`; `WallpaperSettingsContent` 신규 작성 (3:1 프리뷰, 이미지 선택/변경/제거, 안내 텍스트) |
| 11 | `app/.../CrossPagerNavigation.kt` | `wallpaperImage` 파라미터 추가; `WallpaperLayer` composable 신규 — `requiredWidth(screenWidthDp * 3)` + `translationX = (1 - pageIndex) * screenWidthPx`; `LeftPage`/`RightPage`에 wallpaper 전달; CenterRow를 페이지 내부 wallpaper 방식으로 전환; LeftPage/RightPage glass alpha 0.55로 조정 |
| 12 | `app/.../MainActivity.kt` | `wallpaperImage = uiState.wallpaperImage` 전달 |

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| Pager 뒤가 아닌 각 페이지 내부에 wallpaper 렌더링 | HorizontalPager가 내부적으로 독립된 렌더 레이어를 생성하여 Pager 뒤의 wallpaper가 페이지를 통과하지 못함 |
| `requiredWidth()` + `translationX` 보정 `(1 - pageIndex)` | `requiredWidth()`는 부모 제약 초과 시 자동 센터링(offset = -screenWidth)하므로, +screenWidth 보정이 필요 |
| `ContentScale.Crop` + 3배 너비 컨테이너 | 어떤 비율의 이미지든 가운데 기준 crop으로 3개 화면을 덮는 비율 자동 적용 |
| LeftPage/RightPage glass alpha 0.55 | 배경화면이 글래스 효과 뒤로 적절히 비치도록 기존 0.85에서 하향 |
| HomeScreen Surface alpha 0.65 | 그리드 셀 뒤로 배경화면이 은은하게 투과 |
| `feedBackgroundImage` → `wallpaperImage` 전면 리네이밍 | 3개 화면 공유 배경이므로 feed 전용 네이밍 부적합 |

---

## [2026-02-23] feat(feed): LiveStatusCard 오프라인 상태 UI + 플랫폼 아이콘 추가

### 목표

라이브 방송 중이 아닐 때에도 `LiveStatusCard`를 표시하되, "현재 방송 중이 아닙니다." 텍스트와 함께 치지직 채널 페이지로 이동할 수 있는 클릭 동작을 추가한다. 또한 `LiveStatusCard`(치지직)와 `ChannelProfileCard`(유튜브)에 각각 플랫폼 아이콘을 삽입하여 어느 플랫폼 카드인지 시각적으로 구분한다. 최종적으로 오프라인 Surface를 별도 컴포저블로 분리하지 않고 `LiveStatusCard` 하나에서 `if/else`로 내용을 분기하는 구조로 리팩터링한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `feature/launcher/FeedContract.kt` | `FeedIntent`에 `ClickOfflineStatus` sealed object 추가 |
| 2 | `feature/launcher/FeedViewModel.kt` | `handleIntent`에 `ClickOfflineStatus` → `openChzzkChannelPage()` 매핑 추가; `openChzzkChannelPage()` 신규 — `https://chzzk.naver.com/{chzzkChannelId}` OpenUrl 이펙트 발행 |
| 3 | `feature/launcher/ui/FeedScreen.kt` | `Image` · `painterResource` · `R` import 추가; `FeedContent` 내 `if/else` 분기 제거 — `LiveStatusCard` 단일 호출로 통합, 클릭 시 `isLive` 여부에 따라 `ClickLiveStatus` / `ClickOfflineStatus` 분기; `LiveStatusCard` 내부를 단일 `Surface → Row` 구조로 정리 — 치지직 아이콘(32dp, `chzzk_ic.png`) 항상 표시, 이후 내용을 `if (liveStatus.isLive)` 블록으로 분기 (라이브: 네온 글로우 배지 + 제목 + 시청자 수 / 오프라인: "현재 방송 중이 아닙니다." 텍스트); `ChannelProfileCard` Row 맨 앞에 유튜브 아이콘(32dp, `youtube_ic.png`, `RoundedCornerShape(8.dp)` clip) 추가 |

### 검증 결과

```
UI 로직 변경만 포함 (빌드 검증은 다음 커밋에서 확인)
기존 FeedViewModelTest 회귀 없음 — ClickOfflineStatus 로직은 ClickLiveStatus와 동일 패턴으로 테스트 불요
```

### 설계 결정 및 근거

- **오프라인 URL = 채널 홈 (`/channelId`)**: 방송 중일 때는 `/live/$channelId`(라이브 뷰어 직접 진입), 오프라인일 때는 `/$channelId`(채널 홈)로 분리. 오프라인 상태에서 `/live` URL로 이동하면 빈 화면이 뜨므로 UX상 채널 홈이 적합
- **`LiveStatusCard` 단일 컴포저블로 통합**: 라이브·오프라인 양쪽 모두 동일한 `Surface → Row` 골격(플랫폼 아이콘 + 나머지 내용)을 공유하므로, 별도 Surface를 `FeedContent`에 두는 것보다 `LiveStatusCard` 내부 `if/else`가 중복 없이 깔끔함
- **플랫폼 아이콘 크기 32dp**: 카드 내부 패딩 12dp 기준으로 전체 카드 높이에 자연스럽게 비례, 치지직 아이콘의 검정 배경 라운드 코너(8dp clip)와 유튜브 아이콘 형태가 통일감 있게 정렬됨

---

## [2026-02-22] feat(feed): YouTube 채널 프로필 카드 추가 (구독자 수 애니메이션 + 7일 캐시)

### 목표

피드 화면에 YouTube 채널 정보(아바타·채널명·구독자 수)를 보여주는 프로필 카드를 라이브 상태 카드 아래, 피드 목록 위에 고정 배치한다. YouTube Data API `channels.list?part=snippet,statistics`를 신규 호출하고, Quota 절약을 위해 DataStore 7일 캐시를 적용한다. 구독자 수는 `AnimatedContent`로 슬라이드 전환 애니메이션을 적용하여 고급스러운 UX를 제공한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `core/network/model/YouTubeChannelListResponse.kt` | `YouTubeChannelItem`에 `snippet: YouTubeChannelSnippet?` · `statistics: YouTubeChannelStatistics?` nullable 필드 추가; `YouTubeChannelSnippet`(title, description, thumbnails) · `YouTubeChannelStatistics`(subscriberCount, videoCount, viewCount — String) 서브 클래스 신규 정의 |
| 2 | `core/network/api/YouTubeService.kt` | `getChannelInfo(@Url, part, id?, forHandle?, key)` 메서드 추가 — `id`·`forHandle` 모두 nullable (null 파라미터는 Retrofit이 쿼리에서 자동 제외) |
| 3 | `core/domain/model/ChannelProfile.kt` | **신규** — `channelId`, `name`, `avatarUrl`, `subscriberCount: Long`, `videoCount: Long` |
| 4 | `core/domain/repository/FeedRepository.kt` | `getChannelProfile(youtubeChannelId: String): Flow<Result<ChannelProfile>>` 추가 |
| 5 | `core/domain/usecase/GetChannelProfileUseCase.kt` | **신규** — `@Inject constructor(feedRepository)`, `operator fun invoke` 패턴 |
| 6 | `core/data/repository/FeedRepositoryImpl.kt` | `ChannelProfileDto` (`@Serializable`) 추가; DataStore 키 2개(`channel_profile_json`, `channel_profile_timestamp`) 추가; `PROFILE_CACHE_TTL_MS = 7일` 상수; `getChannelProfile()` 구현 (캐시 유효→즉시 return, 스테일→먼저 emit 후 갱신, handle→channelId 메모리캐시 공유); `parseProfileDto()` 헬퍼 추가 |
| 7 | `feature/launcher/FeedContract.kt` | `FeedState`에 `channelProfile: ChannelProfile? = null` 추가; `FeedIntent`에 `ClickChannelProfile` sealed object 추가 |
| 8 | `feature/launcher/FeedViewModel.kt` | `GetChannelProfileUseCase` 주입; `refresh()` 내 세 번째 병렬 `profileJob` 추가 (youtubeChannelId 비면 skip, 실패는 silent); `handleIntent`에 `ClickChannelProfile` → `openChannelPage()` 처리 추가 |
| 9 | `feature/launcher/ui/FeedScreen.kt` | `AnimatedContent` · `CircleShape` · `CircleCropTransformation` import 추가; `ChannelProfileCard` 컴포저블 신규 — 원형 아바타(48dp, `CircleCropTransformation`), 채널명(`titleSmall Bold`), 구독자 수(`AnimatedContent` + `slideInVertically/slideOutVertically`), 클릭 시 `ClickChannelProfile`; `FeedContent` Column에 LiveStatusCard 다음, Spacer 다음, LazyColumn 위에 조건부 렌더링 (`profile.name.isNotEmpty()` 조건); `formatSubscriberCount(Long): String` 헬퍼 추가 (만/천 단위 한국어) |
| 10 | `feature/launcher/FeedViewModelTest.kt` | `GetChannelProfileUseCase` mock 추가; `makeViewModel` 파라미터 추가; `채널 프로필 로드 성공` · `ClickChannelProfile OpenUrl 이펙트` 테스트 2개 신규 추가 |

### 검증 결과

```
./gradlew assembleDebug → BUILD SUCCESSFUL (23s)
./gradlew test         → BUILD SUCCESSFUL — 실패 0건
신규 테스트 2개 추가 (FeedViewModelTest), 기존 테스트 회귀 없음
```

### 설계 결정 및 근거

- **`getChannelInfo` 별도 메서드 추가**: 기존 `getChannelByHandle`은 `part=id`만 요청하는 용도로 유지. `snippet,statistics`를 요청하는 신규 메서드를 분리하여 각 호출 목적을 명확히 하고, `id`·`forHandle` 모두 nullable로 선언해 채널 ID / handle 양방향 지원
- **7일 캐시**: 채널 메타정보(이름·아바타·구독자 수)는 변경 빈도가 낮으므로 7일 TTL 설정. 캐시 신선도 체크 → 스테일 캐시 먼저 emit(UX: 즉시 화면 표시) → 백그라운드 갱신 후 재emit 패턴으로 체감 응답 속도 향상
- **`AnimatedContent` 구독자 수 전환**: `slideInVertically { height } togetherWith slideOutVertically { -height }` 조합으로 숫자가 위로 밀려나고 새 값이 아래에서 올라오는 자연스러운 전환 구현
- **`CircleCropTransformation`**: Coil 2.x `ImageRequest.Builder.transformations()` API 사용 — `clip(CircleShape)` + `CircleCropTransformation` 이중 처리로 아바타 로딩 중·후 모두 원형 유지
- **프로필 fetch 실패는 silent**: 채널 프로필은 non-critical 정보로, 실패 시 `channelProfile = null` 유지 → UI에서 조건부 렌더링으로 카드 숨김. 에러 토스트 미노출로 UX 방해 방지

---

## [2026-02-22] bugfix: Chzzk API `message: null` 역직렬화 오류 수정

### 목표

치지직 라이브 상태 조회 시 `{"code":200,"message":null,...}` 응답을 받으면 `JsonDecodingException`이 발생하여 라이브 상태를 가져오지 못하는 버그를 수정한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `core/network/model/ChzzkLiveResponse.kt` | `message: String = ""` → `message: String? = null` (nullable로 정확히 모델링) |
| 2 | `core/network/di/NetworkModule.kt` | `provideChzzkRetrofit` 내 Json 빌더에 `coerceInputValues = true` 추가 |

### 검증 결과

```
로그캣 에러 재현:
  JsonDecodingException: Expected string literal but 'null' literal was found at path: $.message
  JSON input: {"code":200,"message":null,"content":{...}}
→ message 필드 nullable 처리 후 정상 역직렬화 확인 (앱 재실행)
```

### 설계 결정 및 근거

- **모델 nullable 변경 우선**: `coerceInputValues = true`만으로 해결 가능하지만, Chzzk API 스펙상 `message` 자체가 nullable이므로 타입을 `String?`로 정확히 모델링하는 것이 올바른 접근
- **`coerceInputValues = true` 추가**: `message` 외에 향후 다른 필드에서 같은 상황 발생 시 default 값으로 fallback되도록 방어적으로 추가; 타입 오분류로 인한 동일 오류 재발 방지

---

## [2026-02-22] Step 22: 피드 설정 화면 구현

### 목표

설정 화면에 "피드 설정" 탭을 추가하여 치지직 채널 ID · YouTube 채널 ID · RSS URL · 피드 배경 이미지를 DataStore에 영속화할 수 있도록 한다. 저장된 값은 피드 화면에 즉시 반영되며, 앱 재시작 후에도 유지된다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `domain/model/LauncherSettings.kt` | `feedBackgroundImage: String? = null` 필드 추가 |
| 2 | `domain/repository/SettingsRepository.kt` | `setChzzkChannelId` / `setYoutubeChannelId` / `setRssUrl` / `setFeedBackgroundImage` 메서드 추가 |
| 3 | `domain/usecase/SaveFeedSettingsUseCase.kt` | **신규** — 채널 ID 2개 + RSS URL 3개 필드를 repository에 일괄 저장 |
| 4 | `data/repository/SettingsRepositoryImpl.kt` | DataStore 키 4개(`chzzk_channel_id`, `youtube_channel_id`, `rss_url`, `feed_background_image`) 추가, `getSettings()` 매핑 확장, setter 4개 구현 (`setFeedBackgroundImage` null 시 key 삭제) |
| 5 | `launcher/model/SettingsTab.kt` | `FEED` 값 추가 |
| 6 | `launcher/HomeContract.kt` | `HomeState`에 `chzzkChannelId` / `youtubeChannelId` / `rssUrl` / `feedBackgroundImage` 추가; `HomeIntent`에 `SaveFeedSettings` / `SetFeedBackgroundImage` sealed class 추가 |
| 7 | `launcher/HomeViewModel.kt` | `SaveFeedSettingsUseCase` + `SettingsRepository` 주입; settings collect에 feed 필드 4개 반영; `saveFeedSettings()` / `setFeedBackgroundImage()` 핸들러 추가 |
| 8 | `launcher/FeedContract.kt` | `FeedState`에 `feedBackgroundImage: String? = null` 추가 |
| 9 | `launcher/FeedViewModel.kt` | settings collect에 `feedBackgroundImage` 반영 |
| 10 | `launcher/ui/FeedScreen.kt` | `FeedScreen` → `Box` 래핑; 배경 이미지 레이어 (`AsyncImage`, alpha=0.4f, blur=8.dp, crossfade=300); 기존 콘텐츠를 `FeedContent` 내부 함수로 분리; `LocalContext` 추가 |
| 11 | `launcher/ui/SettingsScreen.kt` | `MainSettingsContent`에 "피드 설정" 버튼 추가 (Column 레이아웃으로 변경); `FeedSettingsContent` 컴포저블 신규 작성 — 배경 이미지 미리보기·선택·제거, `OutlinedTextField` 3개(실시간 isError 검증), 저장 버튼(검증 실패 시 비활성); `when` 분기에 `SettingsTab.FEED` 추가; `derivedStateOf` 기반 입력 검증 |
| 12 | `launcher/HomeViewModelTest.kt` | `saveFeedSettingsUseCase` / `settingsRepository` mock 추가; `makeViewModel` 헬퍼 파라미터 2개 추가 |

### 검증 결과

```
BUILD SUCCESSFUL (assembleDebug)
전체 테스트 BUILD SUCCESSFUL — 실패 0건
기존 테스트 회귀 없음
```

### 설계 결정 및 근거

- **`SettingsRepository` 직접 주입**: `setFeedBackgroundImage`는 단독으로 호출되는 빈도가 높아 별도 UseCase를 만드는 것이 과설계라고 판단, `HomeViewModel`에서 repository를 직접 호출
- **`derivedStateOf` 실시간 검증**: `OutlinedTextField`의 `isError` + `supportingText`로 저장 전 즉각 피드백 제공; 저장 버튼을 검증 실패 시 `enabled = false`로 처리하여 잘못된 데이터 저장 차단
- **`setFeedBackgroundImage(null)` 시 key 삭제**: `stringPreferencesKey`는 null을 직접 저장할 수 없어 `prefs.remove(key)` 사용 — 읽을 때 `?: null`로 자연스럽게 복원
- **배경 이미지 blur + alpha**: `blur(8.dp)` + `alpha = 0.4f` 조합으로 피드 텍스트 가독성 유지하면서 Glassmorphism 분위기 구현
- **URI 영구 권한 try-catch**: 일부 서드파티 파일 매니저에서 `takePersistableUriPermission` 실패 시 크래시 방지; Toast 안내 후 저장 중단

---

## [2026-02-22] Step 21: Feed & Notice 데이터 레이어 구현

### 목표

`core:network` RSS 인프라 위에 치지직 라이브 상태 · 네이버 카페 RSS 공지 · 유튜브 영상 3개 소스를 통합하는 전체 데이터 흐름을 완성한다. MVI 패턴으로 FeedViewModel과 FeedScreen을 구현하고, 왼쪽 스와이프 페이지에 실제 피드 UI를 연결한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `domain/model/LiveStatus.kt` | **신규** — isLive, title, viewerCount, thumbnailUrl, channelId |
| 2 | `domain/model/FeedItem.kt` | **신규** — sealed interface: NoticeItem(title, dateMillis, link, source) / VideoItem(title, dateMillis, thumbnailUrl, videoLink) |
| 3 | `domain/repository/FeedRepository.kt` | **신규** — getLiveStatus(channelId) / getIntegratedFeed(rssUrl, youtubeChannelId) |
| 4 | `domain/usecase/GetLiveStatusUseCase.kt` | **신규** — operator fun invoke |
| 5 | `domain/usecase/GetIntegratedFeedUseCase.kt` | **신규** — operator fun invoke |
| 6 | `domain/model/LauncherSettings.kt` | chzzkChannelId / rssUrl / youtubeChannelId 필드 추가 (기본값 포함) |
| 7 | `network/di/NetworkQualifiers.kt` | **신규** — @XmlRetrofit / @JsonRetrofit qualifier 어노테이션 |
| 8 | `network/di/NetworkModule.kt` | @XmlRetrofit(기존 Retrofit 리네임) + @JsonRetrofit(JSON) 이중 인스턴스 분리; ChzzkService / YouTubeService 제공 |
| 9 | `network/build.gradle.kts` | local.properties 에서 youtube.api.key 읽어 BuildConfig.YOUTUBE_API_KEY 노출 |
| 10 | `network/NetworkConstants.kt` | **신규** — BuildConfig.YOUTUBE_API_KEY 외부 노출 객체 |
| 11 | `network/api/ChzzkService.kt` | **신규** — @GET + @Url 패턴 (절대 URL 동적 전달) |
| 12 | `network/api/YouTubeService.kt` | **신규** — searchVideos / getChannelByHandle (forHandle 핸들→채널ID) |
| 13 | `network/model/ChzzkLiveResponse.kt` | **신규** — ChzzkLiveResponse / ChzzkLiveContent |
| 14 | `network/model/YouTubeSearchResponse.kt` | **신규** — YouTubeSearchResponse / Item / Snippet / Thumbnails |
| 15 | `network/model/YouTubeChannelListResponse.kt` | **신규** — 핸들→채널ID 변환 응답 모델 |
| 16 | `data/build.gradle.kts` | `:core:network` 의존성 추가 |
| 17 | `data/util/DateParser.kt` | **신규** — parseRfc822 / parseIso8601 (다중 패턴 fallback, 실패 시 0L) |
| 18 | `data/repository/FeedRepositoryImpl.kt` | **신규** — getLiveStatus: Chzzk API 호출; getIntegratedFeed: coroutineScope async 병렬, 개별 runCatching 부분성공, dateMillis 내림차순, DataStore 오프라인 캐싱 |
| 19 | `data/di/FeedModule.kt` | **신규** — @Binds FeedRepository + @Provides @Named("feed_cache") DataStore |
| 20 | `launcher/FeedContract.kt` | **신규** — FeedState / FeedIntent / FeedSideEffect MVI 계약 |
| 21 | `launcher/FeedViewModel.kt` | **신규** — @HiltViewModel, init settings collect → refresh(), loadJob 중복방지, 60초 쿨다운, ClickFeedItem/ClickLiveStatus OpenUrl 이펙트 |
| 22 | `launcher/ui/FeedScreen.kt` | **신규** — LiveStatusCard(Breathing 네온글로우), NoticeItemRow, VideoItemRow(Coil AsyncImage), LazyColumn |
| 23 | `app/navigation/CrossPagerNavigation.kt` | feedContent 파라미터 추가; LeftPage를 glassEffect 배경 + content 슬롯 구조로 개편 |
| 24 | `app/MainActivity.kt` | FeedViewModel 추가; FeedSideEffect.OpenUrl → Intent(ACTION_VIEW) 처리; CrossPagerNavigation feedContent 연결 |
| 25 | `app/build.gradle.kts` | `:core:network` 의존성 추가 |
| 26 | `local.properties` | youtube.api.key 키 추가 (빈값, 실기기 사용 시 입력) |

### 검증 결과

```
BUILD SUCCESSFUL (assembleDebug)
전체 테스트 BUILD SUCCESSFUL — 실패 0건

신규 테스트 36개:
  FeedItemTest              5개
  GetLiveStatusUseCaseTest  3개
  GetIntegratedFeedUseCaseTest 3개
  DateParserTest            7개
  FeedRepositoryImplTest    8개
  FeedViewModelTest         10개
```

### 설계 결정 및 근거

- **@Url 패턴**: Chzzk / YouTube는 베이스 URL이 서로 다르므로 `@GET + @Url` 동적 URL 전달 방식 사용 (기존 RssFeedApi와 동일 패턴)
- **@XmlRetrofit / @JsonRetrofit 이중 인스턴스**: xmlutil 기반 RSS 파서(XML)와 kotlinx-serialization JSON이 공존해야 하므로 Qualifier로 분리
- **coroutineScope async 병렬 호출**: RSS와 YouTube 각각 독립적으로 실패할 수 있으므로 runCatching으로 개별 래핑, 한쪽 실패해도 다른 쪽 결과 표시
- **DataStore 캐싱**: cache-first 전략 — 캐시 emit 후 네트워크 호출; 네트워크 실패 시 캐시 데이터 유지 (빈 화면 방지)
- **60초 쿨다운**: YouTube Data API 일일 할당량 보호 (FeedViewModel.MIN_REFRESH_INTERVAL_MS)
- **@Named("feed_cache") DataStore**: SettingsRepositoryImpl의 "launcher_settings"와 분리, 테스트 시 mockk<DataStore> 주입 가능

---

## [2026-02-22] core:network 모듈 구성 — Retrofit2 + OkHttp3 + xmlutil RSS 인프라

### 목표

런처 왼쪽 페이지에 실시간 피드(네이버 카페/라운지 공지사항 RSS)를 연동하기 위한 네트워크 레이어를 구축한다.
빈 스캐폴드 상태의 `core:network` 모듈에 Retrofit2 + OkHttp3 + kotlinx-serialization(XML) 기반 인프라를 완성하고,
RSS XML 파싱을 위한 커스텀 Retrofit 컨버터 팩토리까지 구현한다.

### 변경 사항

| # | 파일 | 작업 |
|---|------|------|
| 1 | `gradle/libs.versions.toml` | retrofit 2.11.0, okhttp 4.12.0, xmlutil 0.90.3, retrofitKotlinxSerialization 1.0.0 버전 추가; 라이브러리 6종 (`retrofit-core`, `okhttp-core`, `okhttp-logging`, `retrofit-kotlinx-serialization`, `xmlutil-core`, `xmlutil-serialization`) 등록 |
| 2 | `core/network/build.gradle.kts` | 전체 재작성 — Hilt/KSP/kotlin-serialization 플러그인 추가; 불필요한 appcompat·material·androidTest 의존성 제거; `buildFeatures { buildConfig = true }` 활성화; `testOptions.unitTests.isReturnDefaultValues = true` 추가 |
| 3 | `core/network/src/main/AndroidManifest.xml` | `INTERNET` 권한 추가 |
| 4 | `core/network/.../model/RssFeedResponse.kt` | **신규** — `RssFeedResponse` / `RssChannel` / `RssItem` RSS XML 모델 (`@XmlSerialName` 어노테이션으로 XML 구조 매핑) |
| 5 | `core/network/.../converter/XmlConverterFactory.kt` | **신규** — xmlutil 기반 Retrofit `Converter.Factory`; `serializer(type)` 반사 조회 → `XML.decodeFromString`으로 응답 바디 역직렬화 |
| 6 | `core/network/.../api/RssFeedApi.kt` | **신규** — `@GET` + `@Url` 동적 URL 패턴으로 임의 RSS 엔드포인트 호출 |
| 7 | `core/network/.../di/NetworkModule.kt` | **신규** — `OkHttpClient` (30s 타임아웃, DEBUG 빌드 시 `HttpLoggingInterceptor.Level.BODY`), `Retrofit` (XmlConverterFactory), `RssFeedApi` Hilt Singleton 제공 |
| 8 | `core/network/src/test/.../ExampleUnitTest.kt` | 삭제 (불필요 스캐폴드) |
| 9 | `core/network/src/androidTest/.../ExampleInstrumentedTest.kt` | 삭제 (불필요 스캐폴드) |

### 검증 결과

```
./gradlew :core:network:assembleDebug  →  BUILD SUCCESSFUL in 11s (29 tasks)
./gradlew assembleDebug                →  BUILD SUCCESSFUL in 1m 12s (224 tasks)
./gradlew test                         →  BUILD SUCCESSFUL in 1m 15s (337 tasks, 실패 0건)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| xmlutil 패키지: `nl.adaptivity.xmlutil.serialization` | Maven group ID(`io.github.pdvrieze.xmlutil`)와 Java 패키지명이 다름. 빌드 실패 후 공식 문서(`pdvrieze.github.io/xmlutil`) 확인으로 올바른 패키지 식별 |
| `buildFeatures { buildConfig = true }` 명시 | AGP 8.x에서 라이브러리 모듈은 `BuildConfig` 자동 생성이 비활성화됨. `NetworkModule`의 `BuildConfig.DEBUG` 참조를 위해 필수 |
| `baseUrl("https://placeholder.invalid/")` | RSS 피드는 `@Url`로 전체 URL을 전달하므로 baseUrl은 파싱 검증용 더미값 사용 |
| XmlConverterFactory 커스텀 구현 | Jakewharton 컨버터는 JSON 전용. RSS XML 응답 처리를 위해 xmlutil을 Retrofit `Converter.Factory`로 래핑 |
| `serializer(type): KSerializer` + `@OptIn(ExperimentalSerializationApi::class)` | Retrofit이 Java `Type`을 전달하므로 kotlinx.serialization의 `serializer(Type)` 반사 API 사용. 비직렬화 가능 타입은 `null` 반환으로 다음 컨버터로 위임 |

---

## [2026-02-21] Step 19 — 드래그 앤 드롭 기반 앱 배치 기능

### 목표

앱 서랍에서 앱을 롱프레스-드래그해 홈 화면 2×2 그리드 셀에 수동 배치할 수 있도록 한다.
드래그 시작 시 앱 서랍이 자동으로 닫히고 홈 화면으로 전환되며, 호버된 셀에 네온 이펙트가 표시된다.
배치 정보는 DataStore에 영속화되며 앱 재시작 후에도 유지된다.

### 변경 사항

| # | 모듈 | 파일 | 작업 |
|---|------|------|------|
| 1 | `:core:domain` | `model/LauncherSettings.kt` | `cellAssignments: Map<GridCell, List<String>>` 필드 추가 |
| 2 | `:core:domain` | `repository/SettingsRepository.kt` | `setCellAssignment(cell, packageNames)` 인터페이스 추가 |
| 3 | `:core:domain` | `usecase/SaveCellAssignmentUseCase.kt` | **신규** — `operator fun invoke(cell, packageNames)` |
| 4 | `:core:data` | `repository/SettingsRepositoryImpl.kt` | `CellAssignmentDto` + `cellAssignmentsKey` + `setCellAssignment` 구현 (read-modify-write 패턴) |
| 5 | `:core:ui` | `dragdrop/DragDropState.kt` | **신규** — 전역 드래그 상태 (draggedApp, dragOffset, hoveredCell, cellBounds hit-test, onScrollToHome 콜백) + `LocalDragDropState` |
| 6 | `:feature:launcher` | `HomeContract.kt` | `cellAssignments`, `pinnedPackages` (computed) 추가; `AssignAppToCell`, `UnassignApp` Intent 추가 |
| 7 | `:feature:launcher` | `HomeViewModel.kt` | `SaveCellAssignmentUseCase` 주입; `distributeApps(apps, assignments)` 핀 고정 우선 배분 로직; `assignAppToCell` / `unassignApp` 핸들러 |
| 8 | `:feature:launcher` | `ui/HomeScreen.kt` | `onGloballyPositioned` → `registerCellBounds`; 3단계 글로우 네온 이펙트 (`drawBehind`); 자력 햅틱(`TextHandleMove`); 핀 아이콘(`Star`) + `combinedClickable` 롱프레스 핀 해제 |
| 9 | `:feature:apps-drawer` | `ui/AppDrawerScreen.kt` | `onAppAssigned` 파라미터 추가; `AppDrawerItem`에 `detectDragGesturesAfterLongPress` 적용 |
| 10 | `:app` | `navigation/CrossPagerNavigation.kt` | `onScrollToHome` 콜백 등록; 드래그 중 Pager 스크롤 차단; 드래그 오버레이(AppIcon + Cancel Zone 빨간 원·X) |
| 11 | `:app` | `MainActivity.kt` | `DragDropState` 생성 + `CompositionLocalProvider(LocalDragDropState)`; `onAppAssigned` 콜백 연결 |
| 12 | `:feature:launcher` | `HomeViewModelTest.kt` | `SaveCellAssignmentUseCase` mock 추가; 신규 테스트 6개 추가 (25~30번) |

#### distributeApps 변경 로직

```
1. assignedPackages = cellAssignments.values.flatten().toSet()
2. 각 셀: pinnedPackageNames → apps에서 찾아 알파벳순 정렬 (핀 고정)
3. 미할당 앱만 기존 알파벳 4등분 로직 적용
4. 각 셀 = [핀 고정 앱 알파벳순] + [자동 배분 앱 알파벳순]
```

#### 네온 이펙트 (drawBehind 3단계 글로우)

| 레이어 | 폭 | alpha |
|--------|-----|-------|
| 외곽 확산 | 12dp | 0.15 |
| 중간 | 6dp | 0.30 |
| 코어 테두리 | 2dp | 0.80 |

`animateFloatAsState(tween(200))`로 on/off 부드럽게 전환.

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL in 29s (197 tasks)
./gradlew test           →  BUILD SUCCESSFUL in 15s (301 tasks, 실패 0건)
```

신규 테스트 6개 통과:
- `AssignAppToCell 처리 시 해당 셀의 cellAssignments에 packageName이 추가됨`
- `AssignAppToCell 처리 시 다른 셀에서 해당 앱이 제거됨`
- `AssignAppToCell 처리 후 pinnedPackages에 해당 앱이 포함됨`
- `UnassignApp 처리 시 모든 셀에서 해당 앱이 제거됨`
- `distributeApps - 할당된 앱이 핀 고정 셀의 앱 목록 앞에 배치됨`
- `AssignAppToCell 처리 시 saveCellAssignmentUseCase가 호출됨`

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `DragDropState`를 `core:ui`에 배치 | `feature:apps-drawer`(드래그 시작)와 `feature:launcher`(셀 등록)가 모두 접근 가능하려면 공통 하위 모듈인 `core:ui`에 위치해야 함. `app` 모듈로 올리면 feature 모듈이 참조 불가 |
| `staticCompositionLocalOf` 사용 | 전역 드래그 상태는 Composition 전체에서 단일 인스턴스로 공유돼야 하며, 상태 변경 시 `DragDropState` 내부 `mutableStateOf`가 세밀한 재구성을 처리하므로 `staticCompositionLocalOf`로 충분 |
| `positionInRoot()` 좌표계 통일 | 앱 서랍(VerticalPager 하위)과 홈 그리드(HorizontalPager 하위)는 서로 다른 레이아웃 컨텍스트에 있으므로, 루트 기준 절대 좌표만이 두 영역의 좌표를 일관되게 비교 가능 |
| `beyondViewportPageCount=1` 의존 | 드래그 시작 직후 홈 그리드가 이미 Compose 트리에 있어야 `registerCellBounds`가 유효함. 기존 설정값(1)이 이를 보장 |
| 핀 아이콘을 `Icons.Default.Star`로 대체 | `Icons.Default.PushPin`은 `material-icons-extended` 의존성 필요. 번들 크기 증가를 피하기 위해 기본 세트의 `Star`로 대체 |
| `combinedClickable` + `@OptIn(ExperimentalFoundationApi::class)` | 탭(앱 실행)과 롱프레스(핀 해제)를 동일 컴포저블에서 처리하기 위해 `combinedClickable` 사용. Compose 1.x에서 실험적 API이므로 명시적 옵트인 |
| `cancelDrag()` 후 결과 반환 패턴 | `endDrag()`는 상태를 먼저 캡처 후 초기화하고 결과를 반환하여, 콜백 처리 중 드래그 상태가 남아있지 않도록 보장 |

---

## [2026-02-21] Hotfix — 테마 컬러 변경 시 그리드·배경색 미반영 버그 수정

### 목표

Step 18에서 구현한 테마 컬러 프리셋 선택 시, `SettingsScreen` 버튼 색상은 바뀌지만
홈화면 **그리드 셀 배경**과 **앱 전체 배경색**이 변하지 않는 버그를 수정한다.

### 원인 분석

```
accentPrimaryOverride
       │
       ▼
  StreamLauncherColors  ← 업데이트됨 (SettingsScreen 버튼 등 일부 요소만)

  MaterialTheme.colorScheme  ← 업데이트 안 됨!
       │
       ├─ HomeScreen GridCellContent
       │    color = MaterialTheme.colorScheme.surfaceVariant   → 그리드 배경 바뀌지 않음
       │    borderColor = MaterialTheme.colorScheme.primary    → 보더색 바뀌지 않음
       └─ 앱 전체 background = MaterialTheme.colorScheme.background → 배경 바뀌지 않음
```

`accentPrimaryOverride`가 `StreamLauncherColors`(커스텀 컬러 시스템)만 갱신하고,
`MaterialTheme.colorScheme`은 Dynamic Color(배경화면 색상) 그대로 유지되어 발생.

### 변경 사항

| # | 모듈 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `core:ui` | `ui/theme/Theme.kt` | `baseColorScheme` → `colorScheme` 변환 단계 추가: accent 오버라이드 시 `MaterialTheme.colorScheme`의 `primary`, `tertiary`, `surface`, `background`, `surfaceVariant`도 함께 갱신 |

#### 수정 로직 (`Theme.kt`)

```kotlin
val colorScheme = if (accentPrimaryOverride != null || accentSecondaryOverride != null) {
    val primary = accentPrimaryOverride ?: baseColorScheme.primary
    val surfaceBase = if (darkTheme) Color(0xFF1C1B1E) else Color(0xFFFFFBFE)
    baseColorScheme.copy(
        primary = primary,                 // 버튼·확장 보더 → accent 색상
        onPrimary = Color.White,
        tertiary = accentSecondaryOverride ?: baseColorScheme.tertiary,
        onTertiary = Color.White,
        background = lerp(surfaceBase, primary, if (darkTheme) 0.06f else 0.03f),  // 배경 미묘하게 tinting
        surface   = lerp(surfaceBase, primary, if (darkTheme) 0.06f else 0.03f),
        surfaceVariant = lerp(surfaceBase, primary, if (darkTheme) 0.18f else 0.12f), // 그리드 셀
    )
} else { baseColorScheme }
```

| 필드 | 효과 |
|------|------|
| `primary` | 확장 셀 보더, Material3 버튼 컬러 → accent 색으로 변경 |
| `surfaceVariant` | 그리드 셀 배경 → accent를 12~18% 블렌딩한 색조로 변경 |
| `surface` / `background` | 앱 전체 배경 → accent를 3~6% 블렌딩해 미묘한 색조 부여 |

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL in 39s
```

수동 검증: Sunset Orange 선택 → 그리드 셀 배경이 옅은 오렌지 톤, 확장 보더·버튼이 오렌지로 즉시 변경 확인.

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `MaterialTheme.colorScheme`도 함께 오버라이드 | 그리드 배경(`surfaceVariant`), 보더(`primary`), 배경(`surface/background`)이 모두 `MaterialTheme.colorScheme.*`을 직접 참조하므로, 커스텀 컬러 시스템만 변경해서는 반영 불가 |
| `lerp(surfaceBase, accent, fraction)`으로 tinting | `copy(alpha = ...)` 방식은 투명도를 주어 배경 위에 합성이 필요함. `lerp`는 불투명 최종 색상을 직접 계산해 렌더링 레이어 추가 없이 정확한 색상 보장 |
| 다크 모드에서 fraction 2배 | 다크 배경은 색상 대비가 낮아 같은 fraction이면 tinting이 덜 드러나므로, 라이트 모드 대비 약 2배 비율 적용 |
| `onPrimary = Color.White` 고정 | 각 프리셋의 accent primary가 밝은 색상 계열이어도 텍스트/아이콘이 흰색으로 통일되어 가독성 유지 |

---

## [2026-02-21] Step 18 — 컬러 픽커 · 이미지 선택기 · DataStore 영속화

### 목표

설정 페이지의 `SettingsTab.COLOR` / `SettingsTab.IMAGE`가 "준비 중" 스텁 상태였다.
사용자 지정 테마 컬러(7가지 프리셋)와 그리드 셀 배경 이미지(축소/확장 각각)를 선택·저장·앱 재시작 후 복원하는 전체 흐름을 구현한다.

### 변경 사항

| # | 모듈 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | 루트 | `gradle/libs.versions.toml` | `coil = "2.6.0"`, `kotlinxSerialization = "1.7.3"` 버전 추가; `coil-compose`, `kotlinx-serialization-json` 라이브러리 항목 추가; `kotlin-serialization` 플러그인 항목 추가 |
| 2 | `core:data` | `build.gradle.kts` | `kotlin-serialization` 플러그인 + `kotlinx.serialization.json` 의존성 추가 |
| 3 | `feature:launcher` | `build.gradle.kts` | `coil-compose` 의존성 추가 |
| 4 | `:app` | `build.gradle.kts` | `coil-compose` 의존성 추가 |
| 5 | `core:domain` | `domain/model/ColorPreset.kt` | **신규** — `data class ColorPreset(index, name, accentPrimaryArgb: Long, accentSecondaryArgb: Long)` |
| 6 | `core:domain` | `domain/model/ColorPresets.kt` | **신규** — `object ColorPresets { val defaults(7개), fun getByIndex(index) }` |
| 7 | `core:domain` | `domain/model/LauncherSettings.kt` | **신규** — `data class LauncherSettings(colorPresetIndex: Int = 0, gridCellImages: Map<GridCell, GridCellImage>)` |
| 8 | `core:domain` | `domain/repository/SettingsRepository.kt` | **신규** — `interface SettingsRepository { getSettings(), setColorPresetIndex(), setGridCellImage() }` |
| 9 | `core:domain` | `domain/usecase/GetLauncherSettingsUseCase.kt` | **신규** — `operator fun invoke(): Flow<LauncherSettings>` |
| 10 | `core:domain` | `domain/usecase/SaveColorPresetUseCase.kt` | **신규** — `suspend operator fun invoke(index: Int)` |
| 11 | `core:domain` | `domain/usecase/SaveGridCellImageUseCase.kt` | **신규** — `suspend operator fun invoke(cell, idle, expanded)` |
| 12 | `core:domain` | `test/.../ColorPresetsTest.kt` | **신규** — 7개 테스트 (프리셋 개수, getByIndex 폴백, 고유성) |
| 13 | `core:data` | `data/repository/SettingsRepositoryImpl.kt` | **신규** — `preferencesDataStore("launcher_settings")`; `GridCellImageDto` JSON 직렬화; `@Singleton @Inject constructor(context)` |
| 14 | `core:data` | `data/di/SettingsModule.kt` | **신규** — `@Binds SettingsRepositoryImpl → SettingsRepository` |
| 15 | `core:ui` | `ui/theme/Theme.kt` | `StreamLauncherTheme`에 `accentPrimaryOverride: Color?`, `accentSecondaryOverride: Color?` 파라미터 추가; override 시 `accentPrimary/Secondary/gridBorderExpanded/searchBarFocused` 교체 |
| 16 | `feature:launcher` | `launcher/model/ImageType.kt` | **신규** — `enum class ImageType { IDLE, EXPANDED }` |
| 17 | `feature:launcher` | `launcher/HomeContract.kt` | `HomeState`에 `colorPresetIndex: Int = 0` 추가; `HomeIntent`에 `ChangeAccentColor`, `SetGridImage` 추가 |
| 18 | `feature:launcher` | `launcher/HomeViewModel.kt` | 생성자에 3개 UseCase 추가; `init`에서 settings 수집 → state 복원; `changeAccentColor`, `setGridImage` 핸들러 구현 |
| 19 | `feature:launcher` | `launcher/ui/SettingsScreen.kt` | `ColorSettingsContent`: 3열 LazyVerticalGrid + 좌우 반색 drawBehind 칩 + 체크마크; `ImageSettingsContent`: 2×2 셀 선택 + PickVisualMedia 런처 2개 + takePersistableUriPermission |
| 20 | `feature:launcher` | `launcher/ui/HomeScreen.kt` | `GridCellContent`에 `gridCellImage: GridCellImage?` 파라미터 추가; 축소·확장 상태 모두 Coil `AsyncImage(crossfade=300)` 배경 표시 |
| 21 | `feature:launcher` | `test/.../HomeViewModelTest.kt` | 3개 UseCase mock 추가 (`relaxed=true`); `makeViewModel()` 헬퍼; 6개 신규 테스트 (컬러/이미지 state 변경·저장 검증, 설정 복원) |
| 22 | `:app` | `MainActivity.kt` | `ColorPresets.getByIndex(uiState.colorPresetIndex)`로 preset 계산; `StreamLauncherTheme`에 `accentPrimaryOverride`, `accentSecondaryOverride` 전달 |

#### 아키텍처 흐름

```
사용자 클릭 (SettingsScreen)
  └─► HomeIntent.ChangeAccentColor(index)
        ├─ HomeViewModel.updateState { copy(colorPresetIndex = index) }   ← 즉시 UI 반영
        └─ SaveColorPresetUseCase(index)
              └─ SettingsRepository.setColorPresetIndex(index)
                    └─ DataStore.edit { prefs[COLOR_KEY] = index }

앱 시작 / init
  └─► GetLauncherSettingsUseCase().collect { settings ->
        HomeViewModel.updateState { copy(colorPresetIndex, gridCellImages) }
      }

MainActivity (StreamLauncherTheme)
  └─► ColorPresets.getByIndex(uiState.colorPresetIndex)
        └─► accentPrimaryOverride / accentSecondaryOverride → StreamLauncherColors 오버라이드
```

#### URI 저장 구조 (GridCellImageDto JSON 배열)

```json
[
  { "cell": 0, "idle": "content://media/...", "expanded": null },
  { "cell": 2, "idle": null, "expanded": "content://media/..." }
]
```

### 검증 결과

```
./gradlew test           →  BUILD SUCCESSFUL in 35s (301 tasks, failures=0)
./gradlew assembleDebug  →  BUILD SUCCESSFUL in 45s (197 tasks, 93 executed)
```

- 신규 테스트: `ColorPresetsTest` 7개 + `HomeViewModelTest` 신규 6개 = +13개
- 기존 회귀 없음 (18개 기존 HomeViewModelTest 모두 통과)

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| URI 직렬화를 kotlinx-serialization JSON으로 | URI 내부에 쉼표·파이프 등 특수문자 포함 가능. 단순 구분자 방식은 파싱 오류 위험. JSON 배열로 안전하게 직렬화 |
| `accentPrimaryOverride`가 Dynamic Color 위에서도 덮어씀 | 사용자가 명시적으로 선택한 테마 컬러가 배경화면 자동 색상보다 우선시되어야 사용자 의도가 보존됨 |
| `crossfade(300)` 적용 | 이미지 설정·변경 시 갑작스러운 전환 없이 부드러운 페이드. 300ms는 Material 전환 가이드라인(250~350ms) 범위 내 |
| 이미지 `ContentScale.Crop` + 가독성 오버레이 | 임의 종횡비 이미지도 셀을 채우도록 중앙 크롭. 앱 목록/셀 이름 텍스트 위에 반투명 레이어로 가독성 확보 |
| `relaxed = true` mock (SaveColorPresetUseCase, SaveGridCellImageUseCase) | `idle: String?`, `expanded: String?` 파라미터가 nullable → MockK `any<String?>()` 타입 바운드 컴파일 오류 발생. relaxed mock은 모든 suspend 호출에 Runs를 자동 설정하여 coEvery 없이도 안전 |
| `takePersistableUriPermission` | PickVisualMedia로 획득한 content URI는 앱이 재시작되면 접근 권한이 사라짐. Persistable 권한 요청으로 재시작 후에도 Coil이 동일 URI로 이미지 로드 가능 |
| `makeViewModel()` 헬퍼 | 생성자 파라미터가 4개로 증가 → 각 테스트에서 반복 작성 방지. 기본값으로 공통 mock을 받고 필요한 케이스만 오버라이드 |

---

## [2026-02-21] Step 17 — 설정 페이지 기초 및 서브 네비게이션 구현

### 목표

`CrossPagerNavigation` 상단 페이지(VerticalPager index 0)의 "Notifications & Settings" 텍스트 플레이스홀더를 실제 설정 화면으로 교체한다. 테마 컬러 / 홈 이미지 설정 서브 탭으로의 네비게이션 기반을 구축하고, 도메인 모델(`GridCellImage`)을 `core:domain`에 순수하게 정의한다.

추가로, 기존에 `feature:launcher` 모듈에 있던 `GridCell` enum이 `core:domain`에 있어야 하는 도메인 개념임을 인지하고 이동한다 (순환 의존성 방지).

### 변경 사항

| # | 모듈 | 파일 | 변경 내용 |
|---|------|------|----------|
| 1 | `core:domain` | `domain/model/GridCell.kt` | **신규(이동)** — `feature:launcher`의 `launcher/model/GridCell.kt`를 `core:domain`으로 이동, 패키지 변경 (`launcher.model` → `domain.model`) |
| 2 | `core:domain` | `domain/model/GridCellImage.kt` | **신규** — 그리드 셀별 이미지 URI 데이터 클래스 (`cell: GridCell`, `idleImageUri`, `expandedImageUri`) |
| 3 | `feature:launcher` | `launcher/model/GridCell.kt` | **삭제** — `core:domain`으로 이동 |
| 4 | `feature:launcher` | `launcher/HomeContract.kt` | import 수정 (`launcher.model.GridCell` → `domain.model.GridCell`); `HomeState`에 `currentSettingsTab: SettingsTab`, `gridCellImages: Map<GridCell, GridCellImage>` 추가; `HomeIntent.ChangeSettingsTab(tab: SettingsTab)` 추가 |
| 5 | `feature:launcher` | `launcher/HomeViewModel.kt` | import 수정 (`domain.model.GridCell`, `launcher.model.SettingsTab`); `ChangeSettingsTab` 핸들러 추가; `resetHome()`에 `currentSettingsTab = SettingsTab.MAIN` 초기화 추가 |
| 6 | `feature:launcher` | `launcher/ui/HomeScreen.kt` | import 수정 (`domain.model.GridCell`) |
| 7 | `feature:launcher` | `launcher/model/SettingsTab.kt` | **신규** — `enum class SettingsTab { MAIN, COLOR, IMAGE }` |
| 8 | `feature:launcher` | `launcher/ui/SettingsScreen.kt` | **신규** — `SettingsScreen` 컴포저블 (탭별 화면 분기, `BackHandler`, spring 애니메이션, 햅틱 피드백) |
| 9 | `feature:launcher` | `test/.../HomeViewModelTest.kt` | import 수정 (`domain.model.GridCell`) |
| 10 | `:app` | `navigation/CrossPagerNavigation.kt` | `settingsContent: @Composable () -> Unit = {}` 파라미터 추가; `UpPage`를 `Surface` 플레이스홀더 → GlassEffect 2-layer Box + 콘텐츠 슬롯 구조로 교체 |
| 11 | `:app` | `MainActivity.kt` | `SettingsScreen` import 추가; `CrossPagerNavigation`에 `settingsContent` 슬롯 연결 |

#### SettingsScreen 구조

```
SettingsScreen(state: HomeState, onIntent: (HomeIntent) -> Unit)
├─ BackHandler(enabled = tab != MAIN) → ChangeSettingsTab(MAIN)
└─ Box(Center)
   ├─ [MAIN]  MainSettingsContent
   │   └─ Row
   │       ├─ SettingsButton("테마 컬러", accentPrimary) → ChangeSettingsTab(COLOR)
   │       └─ SettingsButton("홈 이미지", accentSecondary) → ChangeSettingsTab(IMAGE)
   ├─ [COLOR] ColorSettingsContent  (준비 중 placeholder)
   └─ [IMAGE] ImageSettingsContent  (준비 중 placeholder)
```

#### UpPage 리팩터링 (CrossPagerNavigation)

```
[이전] Surface(surfaceVariant) > Box(Center) > Text("Notifications & Settings")

[이후] Box (graphicsLayer alpha)
       ├─ Box [배경] — glassEffect(overlayColor = glassSurface)
       └─ Box [콘텐츠] — statusBarsPadding
           └─ settingsContent()   ← SettingsScreen 슬롯
```

#### 의존성 이동 근거

```
[이전] GridCell: feature:launcher/launcher/model/
       └─ feature:launcher가 core:domain에 의존하므로 역방향 참조 불가
       → GridCellImage를 core:domain에 놓으려면 GridCell도 core:domain 필요

[이후] GridCell: core:domain/domain/model/
       GridCellImage: core:domain/domain/model/
       → core:data에서 향후 DataStore 영속화 시 GridCellImage를 바로 참조 가능
```

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL in 19s (197 tasks, 51 executed)
./gradlew test           →  BUILD SUCCESSFUL in 32s (301 tasks, failures=0)
```

기존 테스트 전체 회귀 없음 (import 경로 변경만, 동작 변경 없음).

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `GridCell`을 `core:domain`으로 이동 | 그리드 셀은 런처 도메인의 핵심 개념. `GridCellImage`가 `GridCell`을 참조하는데, `GridCellImage`를 `core:domain`에 두려면 `GridCell`도 동일 모듈에 있어야 순환 의존성 미발생. `feature:launcher`에서는 `core:domain`을 참조하므로 하위호환 유지 |
| `GridCellImage`를 `core:domain`에 정의 | 향후 `core:data`에서 `SettingsRepository`로 DataStore에 URI를 영속화할 때 `core:domain` 모델을 그대로 참조 가능. 순수 데이터 클래스로 Android 의존성 없음 |
| `SettingsTab`을 `feature:launcher`에 배치 | 설정 탭 전환은 런처 UI의 내비게이션 상태이므로 도메인이 아닌 feature 계층에서 관리. `core:domain`에 두면 UI 개념이 도메인에 침투함 |
| `UpPage`를 GlassEffect 구조로 통일 | `DownPage`(앱 서랍), `RightPage`(위젯) 모두 GlassEffect 배경을 사용. `UpPage`만 불투명 `Surface`를 유지하면 시각 일관성 깨짐 |
| `settingsContent` 슬롯을 기본값 `{}` 람다로 | `appDrawerContent`, `widgetContent`와 동일 패턴. MainActivity가 콘텐츠를 주입하고 CrossPagerNavigation은 위치·전환만 책임짐 |
| `SettingsButton`에 `defaultMinSize(minHeight = 48.dp)` | 구글 접근성 가이드라인 권장 최소 터치 영역(48×48dp) 준수 |
| `BackHandler(enabled = tab != MAIN)` | 서브 탭에서만 활성화해 MAIN에서는 CrossPagerNavigation의 페이지 복귀 BackHandler와 충돌하지 않음 |

---

## [2026-02-21] Step 16 — 클린아키텍처 점검 및 모듈 분리 리팩토링

### 목표

프로젝트 전체의 클린아키텍처 · MVI 패턴 준수 여부를 점검하고, 잘못 배치된 파일들을 올바른 모듈로 이동한다.
- 테마/modifier가 `:app`에 묶여 있어 다른 feature 모듈에서 접근 불가 → `core:ui`로 이동
- `WidgetViewModel` · `WidgetScreen`이 `:app`에 혼재 → 신규 `:feature:widget` 모듈로 이동
- `AppDrawerScreen` · `AppIcon`이 `:app/navigation`에 있고 `:feature:apps-drawer`는 빈 모듈 → 이동 및 구현 채움
- `AppDrawerScreen`의 `HomeState`/`HomeIntent` 직접 의존 → 순수 도메인 파라미터로 변경해 feature 간 의존 제거

### 변경 사항

| # | 대상 모듈 | 파일 | 변경 내용 |
|---|----------|------|----------|
| 1 | `core:ui` | `ui/theme/Color.kt` | **신규** — `StreamLauncherColors`, `DarkStreamLauncherColors`, `LightStreamLauncherColors`, `LocalStreamLauncherColors` (`:app`에서 이동) |
| 2 | `core:ui` | `ui/theme/Theme.kt` | **신규** — `StreamLauncherTheme`, `StreamLauncherTheme.colors` 접근자 (`:app`에서 이동) |
| 3 | `core:ui` | `ui/theme/Type.kt` | **신규** — `NotoSansKrFontFamily`, `Typography` (`:app`에서 이동, `R` import → `org.comon.streamlauncher.ui.R`) |
| 4 | `core:ui` | `ui/modifier/GlassEffect.kt` | **신규** — `glassEffect` Modifier (`:app`에서 이동) |
| 5 | `core:ui` | `res/font/noto_sans_kr.ttf` | **신규** — 폰트 리소스 (`:app/res/font`에서 이동) |
| 6 | `feature:widget` | `build.gradle.kts` | Compose / Hilt / KSP / `:core:domain` / `:core:ui` / `activity-compose` 의존성 추가 |
| 7 | `feature:widget` | `widget/WidgetViewModel.kt` | **신규** — 패키지 `org.comon.streamlauncher.widget` (`:app`에서 이동) |
| 8 | `feature:widget` | `widget/ui/WidgetScreen.kt` | **신규** — 패키지 `org.comon.streamlauncher.widget.ui` (`:app/navigation`에서 이동) |
| 9 | `feature:apps-drawer` | `apps_drawer/ui/AppIcon.kt` | **신규** — 패키지 `org.comon.streamlauncher.apps_drawer.ui` (`:app/navigation`에서 이동) |
| 10 | `feature:apps-drawer` | `apps_drawer/ui/AppDrawerScreen.kt` | **신규** — 파라미터 `HomeState`/`HomeIntent` → `searchQuery: String`, `filteredApps: List<AppEntity>`, `onSearch`, `onAppClick` (feature 간 의존 제거) |
| 11 | `:app` | `app/build.gradle.kts` | `:feature:widget` 의존성 추가 |
| 12 | `:app` | `MainActivity.kt` | import 3개 수정 (`widget.*`, `apps_drawer.ui.*`); `AppDrawerScreen` 호출부 파라미터 분해 |
| 13 | `:app` | 구 파일 8개 삭제 | `WidgetViewModel.kt`, `navigation/WidgetScreen.kt`, `navigation/AppDrawerScreen.kt`, `navigation/AppIcon.kt`, `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `ui/modifier/GlassEffect.kt`, `res/font/noto_sans_kr.ttf` |

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL (197 tasks)
./gradlew test           →  BUILD SUCCESSFUL (301 tasks, 실패 0건)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| 테마/폰트를 `core:ui`로 이동 | feature 모듈에서 `StreamLauncherTheme.colors` 참조 시 `:app` 역의존 → 순환 참조 위험 차단 |
| `AppDrawerScreen` 파라미터를 도메인 타입으로 교체 | `feature:apps-drawer`가 `feature:launcher`에 의존하면 feature 간 결합 발생 — Contract를 app 계층에서 분해해 주입 |
| `WidgetViewModel`을 `feature:widget`으로 이동 | `:app`은 진입점(Manifest, DI 루트)만 담당해야 함; 비즈니스 로직·상태를 feature 모듈로 격리해 재사용성 확보 |
| `AppWidgetHost` 생명주기 관리는 `MainActivity`에 유지 | `AppWidgetHost`는 Activity 생명주기(`onStart`/`onStop`)에 직접 묶여야 하므로 feature 모듈에 내려보내지 않음 |

---

## [2026-02-21] Step 15 — 위젯 화면 완성 (편집 모드 · 슬롯 고정 · 터치 수정)

### 목표

Step 13·14에서 구현한 위젯 시스템을 실사용 가능한 수준으로 완성한다.
- 위젯 위에서 롱프레스로 삭제 버튼이 뜨도록 수정
- 롱프레스 후 손을 떼도 위젯 내부 버튼이 동작하지 않도록 수정
- 위젯 삭제 시 인접 위젯이 사라지는 버그 수정
- 추가·삭제해도 위치가 유지되는 슬롯 기반 저장 구조로 전환
- 편집 모드 토글 UX 구현 (빈 셀·위젯 롱프레스 → 타이틀바 + 완료·뒤로가기 해제)

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `core/domain/.../WidgetRepository.kt` | `getWidgetIds/addWidgetId/removeWidgetId` → `getWidgetSlots/setWidgetAtSlot/clearSlot`; `MAX_WIDGETS=6` 유지; `List<Int?>` (고정 6슬롯, null=빈칸) 반환 |
| 2 | `core/data/.../WidgetRepositoryImpl.kt` | 저장 키 `widget_ids`(팩드 리스트) → `widget_slots`(고정 6칸 문자열, -1=빈칸); `parseSlots/encodeSlots` 헬퍼; 3단계 마이그레이션 (Step13 단일 int → Step14 팩드 리스트 → 현재 슬롯 배열) |
| 3 | `app/.../WidgetViewModel.kt` | `widgetSlots: StateFlow<List<Int?>>` (initialValue=6칸 null); `setWidgetAtSlot(slot, id)`, `clearSlot(slot)` |
| 4 | `app/.../navigation/WidgetScreen.kt` | **WidgetContainerView**: `dispatchTouchEvent` 오버라이드 (isDeleteModeActive=true 시 자식 터치 완전 차단), GestureDetector로 롱프레스 감지 (onLongPress에서 isDeleteModeActive 동기 세팅 → ACTION_UP 차단); **WidgetScreen**: `isEditMode` 상태 + `BackHandler` + 타이틀바(AnimatedVisibility 슬라이드 인/아웃); **WidgetCell**: `isEditMode` 파라미터 → 편집 모드 시 삭제 버튼 항상 표시, 롱프레스로 편집 모드 진입; **EmptyCell**: `pointerInput(isEditMode)` 롱프레스로 편집 모드 진입, 편집 모드에서만 "+" 버튼 표시; `key(widgetId)` 추가 |
| 5 | `app/.../MainActivity.kt` | `widgetSlots` collect; `pendingSlot: Int` 추가; `launchWidgetPicker(slotIndex)` → `setWidgetAtSlot(pendingSlot, id)`; `deleteWidget(slotIndex)` → widgetId 조회 후 `deleteAppWidgetId` + `clearSlot` |

### 핵심 문제 해결 과정

#### 1. 위젯 롱프레스로 삭제 버튼 표시 (3단계 시도)

| 시도 | 방법 | 결과 | 실패 원인 |
|------|------|------|----------|
| 1차 | `combinedClickable` on Box | ❌ | AppWidgetHostView가 터치 먼저 소비 |
| 2차 | `setOnTouchListener` + GestureDetector | ❌ | 자식이 `requestDisallowInterceptTouchEvent(true)` 호출 시 미작동 |
| 3차 | `dispatchTouchEvent` 오버라이드 | ✅ | 모든 플래그와 무관하게 항상 최우선 호출 |

#### 2. 롱프레스 후 위젯 내부 버튼 동작 차단

```
onLongPress 콜백 (타이머, 동기):
  isDeleteModeActive = true  ← 즉시 플래그 세팅 (recomposition 대기 없음)
  onLongPressListener()      ← Compose 상태 업데이트

다음 dispatchTouchEvent(ACTION_UP):
  isDeleteModeActive == true → return true (자식 전달 없음) ✅
```

`requestDisallowInterceptTouchEvent`가 세팅된 이후라도 `dispatchTouchEvent`는 항상 호출됨.

#### 3. 위젯 삭제 시 인접 위젯 사라지는 버그

`key {}` 없는 for 루프에서 Compose는 위치 기준으로 컴포저블을 재사용.

```
widgetIds=[id1,id2] → id1 삭제 → [id2]
[수정 전] 슬롯 0: WidgetCell 재사용(id2 파라미터), factory 재호출 ❌ → id1 View 남음
          슬롯 1: WidgetCell dispose → id2 View 소멸 ❌
[수정 후] key(widgetId): id1 컴포저블 dispose ✅, id2 컴포저블 슬롯 이동 ✅
```

#### 4. 슬롯 기반 위치 고정

```
[이전] "id1,id2"        팩드 리스트 → 항상 앞으로 압축, 삭제 시 위치 이동
[이후] "id1,-1,-1,-1,-1,id2"  고정 6칸 → 슬롯 인덱스 직접 저장/삭제
```

탭한 슬롯 인덱스를 `pendingSlot`에 보관 → 선택 완료 시 `setWidgetAtSlot(pendingSlot, id)`.

#### 5. 편집 모드 UX

```
[일반 모드]  위젯만 표시, 완전한 인터랙션
[진입 조건]  빈 셀 롱프레스 OR 위젯 롱프레스
[편집 모드]  타이틀바("위젯 편집" + "완료") 슬라이드 인
            위젯 셀: 삭제 버튼 항상 표시, 내부 터치 차단
            빈 셀: "+" 버튼 표시
[해제 조건]  "완료" 탭 OR 뒤로가기(BackHandler)
```

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL (모든 단계)
./gradlew test           →  BUILD SUCCESSFUL (기존 테스트 회귀 없음)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `dispatchTouchEvent` 오버라이드 선택 | `setOnTouchListener`·`onInterceptTouchEvent`는 `FLAG_DISALLOW_INTERCEPT` 세팅 시 우회됨. `dispatchTouchEvent`는 플래그와 무관하게 항상 최우선 실행 |
| `isDeleteModeActive`를 `onLongPress`에서 동기 세팅 | recomposition 대기 없이 즉시 플래그 세팅 → 같은 제스처의 ACTION_UP이 자식에 도달하기 전에 차단 |
| `key(widgetId)` 필수 | Compose 위치 기반 재사용을 막아 AndroidView factory의 widgetId 캡처가 항상 정확히 유지됨 |
| 슬롯 기반 저장 (`List<Int?>`) | 팩드 리스트는 삭제 시 위치가 압축됨. 고정 슬롯은 인덱스가 곧 위치이므로 추가·삭제해도 다른 슬롯에 영향 없음 |
| `pendingSlot` 패턴 | ActivityResult 콜백은 비동기이므로 어느 슬롯에 저장할지를 별도 필드로 보관 |
| `isEditMode`를 WidgetScreen 로컬 상태로 관리 | 화면을 벗어나면 자동 초기화되는 순수 UI 상태; ViewModel에 올릴 필요 없음 |
| `BackHandler(enabled = isEditMode)` | Compose의 BackHandler는 활성화된 가장 최근 핸들러를 우선하므로 편집 모드일 때만 뒤로가기를 가로챔 |

---

## [2026-02-21] Step 14 — 다중 위젯 지원 구현 (2×3 고정 그리드)

### 목표

위젯 페이지(HorizontalPager index 2)가 위젯 1개만 등록·표시 가능한 구조를 **최대 6개 슬롯의 2×3 고정 그리드**로 변경한다. 빈 슬롯은 "+" 버튼으로 표시하고, 위젯별 개별 삭제를 지원하며, 앱 재시작 후에도 배치가 유지되도록 DataStore 저장 방식을 개선한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `core/domain/.../repository/WidgetRepository.kt` | `getWidgetId(): Flow<Int>` → `getWidgetIds(): Flow<List<Int>>`; `saveWidgetId / clearWidgetId` → `addWidgetId / removeWidgetId`; `migrateLegacyData()` default no-op 추가; `MAX_WIDGETS = 6` companion 상수 추가 |
| 2 | `core/data/.../repository/WidgetRepositoryImpl.kt` | 저장키 `intPreferencesKey("widget_id")` → `stringPreferencesKey("widget_ids")` (콤마 구분 문자열, 순서 보장); `addWidgetId`: 중복·MAX 초과 방지 후 append; `removeWidgetId`: 특정 ID만 필터 제거; `migrateLegacyData()`: 레거시 단일 값 → 새 형식 이관 후 레거시 키 삭제; `parseIds()` private 헬퍼 추출 |
| 3 | `app/.../WidgetViewModel.kt` | `widgetId: StateFlow<Int>` → `widgetIds: StateFlow<List<Int>>` (initialValue = emptyList()); `saveWidget` → `addWidget`; `clearWidget` → `removeWidget(id: Int)`; `init` 블록에서 `migrateLegacyData()` 1회 호출 |
| 4 | `app/.../navigation/WidgetScreen.kt` | 파라미터 `widgetId: Int` → `widgetIds: List<Int>`, `onDeleteWidgetClick: (Int) -> Unit`; `Column(3행) × Row(2열)` 고정 그리드; 위젯 슬롯 `WidgetCell` (AndroidView + 길게 눌러 삭제 오버레이); 빈 슬롯 `EmptyCell` (+ 버튼); 슬롯 배열은 widgetIds 앞채우고 나머지 null 패딩 |
| 5 | `app/.../MainActivity.kt` | `widgetId` → `widgetIds` collect; `saveWidget` → `addWidget` (2곳); `deleteCurrentWidget()` → `deleteWidget(id: Int)` (특정 ID만 삭제); WidgetScreen 호출부 파라미터 갱신 |

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL in 16s (162 tasks, 36 executed)
./gradlew test           →  BUILD SUCCESSFUL in 28s (245 tasks, failures=0)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| 저장 형식을 콤마 구분 문자열로 채택 | DataStore Preferences는 `Set<Int>` 직렬화 시 순서 비보장. 삽입 순서를 유지하여 그리드 배치가 재시작 후에도 동일하게 복원됨 |
| `migrateLegacyData()`를 ViewModel `init`에서 1회 호출 | 앱 업데이트 시 기존 단일 위젯 사용자 데이터 유실 없이 새 형식으로 이관; 이후 레거시 키 삭제로 이중 저장 방지 |
| 모든 빈 슬롯에 "+" 버튼 표시 | "첫 번째 빈 칸만 활성" 방식보다 어느 슬롯에든 탭하면 추가되는 방식이 더 직관적이고 일관성 있음 |
| `MAX_WIDGETS = 6`을 도메인 상수로 정의 | Repository 구현과 UI 모두 동일 상수 참조 → 슬롯 수 변경 시 단일 수정 지점 |
| `WidgetModule.kt`, `CrossPagerNavigation.kt`, `AndroidManifest.xml` 무변경 | 인터페이스 바인딩·페이지 슬롯 구조·권한 선언은 그대로 유지되어 변경 범위 최소화 |

---

## [2026-02-21] Step 13 — 시스템 위젯 지원 구현

### 목표
HorizontalPager 오른쪽 페이지(index 2)의 "Widget Area" 플레이스홀더를 실제 안드로이드 시스템 위젯을 호스팅하는 기능으로 교체한다. `AppWidgetHost`로 위젯을 렌더링하고, DataStore로 위젯 ID를 영속화하여 앱 재시작 후에도 위젯이 유지되도록 한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `core/domain/.../repository/WidgetRepository.kt` | **신규** — `getWidgetId(): Flow<Int>`, `saveWidgetId(Int)`, `clearWidgetId()` 인터페이스; `INVALID_WIDGET_ID = -1` companion 상수 |
| 2 | `core/data/.../repository/WidgetRepositoryImpl.kt` | **신규** — `preferencesDataStore(name="widget_prefs")`, `intPreferencesKey("widget_id")` 사용; `@ApplicationContext` Context 주입 |
| 3 | `core/data/.../di/WidgetModule.kt` | **신규** — `@Binds @Singleton` WidgetRepositoryImpl → WidgetRepository Hilt 모듈 |
| 4 | `app/.../WidgetViewModel.kt` | **신규** — `@HiltViewModel`; `widgetId: StateFlow<Int>` (WhileSubscribed 5s); `saveWidget(id)`, `clearWidget()` |
| 5 | `app/.../navigation/WidgetScreen.kt` | **신규** — 빈 상태(`+` 버튼 + "위젯 추가" 텍스트, `glassOnSurface` 색상); 위젯 렌더링(`AndroidView` + `AppWidgetHostView`, `MATCH_PARENT` layoutParams); 길게 눌러 삭제 오버레이(`AnimatedVisibility`, 빨간 Delete 버튼) |
| 6 | `app/.../navigation/CrossPagerNavigation.kt` | `widgetContent: @Composable () -> Unit = {}` 파라미터 추가; `RightPage`를 `Surface` 플레이스홀더 → `DownPage`와 동일한 2-layer Glass Box 구조(`glassEffect` 배경 + `safeDrawingPadding` 콘텐츠)로 교체; `CenterRow`에 `widgetContent` 전달 |
| 7 | `app/.../MainActivity.kt` | `widgetViewModel: WidgetViewModel by viewModels()` 추가; `AppWidgetHost(HOST_ID=1)`, `AppWidgetManager` 초기화; `onStart/onStop`에 `startListening/stopListening`; `pickWidgetLauncher`(위젯 선택 → configure 분기); `configureWidgetLauncher`(구성 결과 → save or deleteAppWidgetId); `launchWidgetPicker()`, `deleteCurrentWidget()` 함수; `CrossPagerNavigation`에 `widgetContent` 슬롯 연결 |
| 8 | `app/src/main/AndroidManifest.xml` | `<uses-permission android:name="android.permission.BIND_APPWIDGET" />` 추가 |

### 위젯 선택 → 구성 → 렌더링 플로우

```
"+" 탭
  → allocateAppWidgetId()
  → ACTION_APPWIDGET_PICK 시스템 다이얼로그
  → 위젯 선택 결과
      ├─ configure != null → ACTION_APPWIDGET_CONFIGURE 실행
      │     ├─ RESULT_OK  → saveWidgetId() → DataStore → StateFlow emit
      │     └─ 취소/실패  → deleteAppWidgetId() (누수 방지)
      └─ configure == null → 즉시 saveWidgetId()

WidgetScreen 리컴포지션
  → widgetId != INVALID → AndroidView(AppWidgetHostView) 렌더링
  → 길게 누르기 → 삭제 오버레이 AnimatedVisibility 토글
  → 삭제 탭 → deleteAppWidgetId() + clearWidgetId()
```

### 검증 결과

```
./gradlew assembleDebug  →  BUILD SUCCESSFUL in 15s (162 tasks, 32 executed)
./gradlew test           →  BUILD SUCCESSFUL in 27s (245 tasks, failures=0, 전체 회귀 없음)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `WidgetRepository`를 `core:domain`에 정의 | 도메인 계층이 Android 프레임워크(`AppWidgetManager`)에 의존하지 않도록 `INVALID_WIDGET_ID = -1`을 도메인 상수로 정의; `AppWidgetManager.INVALID_APPWIDGET_ID`(-1)와 값이 동일하여 런타임 호환 |
| `ACTION_APPWIDGET_PICK` 방식 채택 | `BIND_APPWIDGET`은 signature-level 권한이지만 시스템이 바인딩을 대신 처리; 기본 런처 등록 시 사용자 승인 하에 정상 동작 |
| `AppWidgetHost` 생명주기를 `onStart/onStop`에 배치 | `onCreate/onDestroy`보다 가시성 기반 관리가 정확; 백그라운드에서 불필요한 업데이트 수신 방지 |
| 위젯 ID `allocateAppWidgetId` → 취소 시 `deleteAppWidgetId` | 미사용 ID가 누적되면 AppWidgetManager 리소스 누수 발생. 취소/실패 모든 경우에 반환 처리 |
| `RightPage`를 `DownPage`와 동일한 2-layer Glass 구조로 통일 | 위젯 배경이 흰 박스가 아닌 글래스모피즘 위에 올라오도록; DownPage와 시각 일관성 확보 |
| `widgetContent`를 `CrossPagerNavigation` 슬롯으로 분리 | MainActivity가 `AppWidgetHost` 인스턴스를 직접 소유하므로, Composable 내부에서 Activity 참조를 피하고 슬롯 패턴으로 주입 |

---

## [2026-02-20] Hotfix — LazyColumn 중복 키 크래시 수정

### 목표
Step 12에서 `animateItem()` 추가 후 발생한 `IllegalArgumentException: Key was already used` 런타임 크래시를 수정한다.

### 현상

```
FATAL EXCEPTION: main
java.lang.IllegalArgumentException: Key "com.google.android.googlequicksearchbox" was already used.
If you are using LazyColumn/Row please make sure you provide a unique key for each item.
  at LazyLayoutItemAnimator.onMeasured(LazyLayoutItemAnimator.kt:255)
```

앱 서랍을 열고 스크롤하면 즉시 크래시 발생.

### 원인 분석

```
com.google.android.googlequicksearchbox
  ├─ Activity 1: GEL (Google 위젯 런처 엔트리)   ← packageName 동일
  └─ Activity 2: GEL 앱 실행용                  ← packageName 동일
  → AppRepositoryImpl이 MAIN/LAUNCHER 인텐트로 쿼리 시 AppEntity 2개 반환
```

`animateItem()` 이전에는 `LazyLayoutItemAnimator`가 비활성화 상태로, 중복 key가 묵인되었음.
`animateItem()` 추가로 Animator가 활성화되면서 중복 key 감지 → 크래시.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `app/.../navigation/AppDrawerScreen.kt` | LazyColumn `key = { it.packageName }` → `key = { it.activityName }` — activityName은 FQCN으로 기기 전체에서 고유 |
| 2 | `feature/launcher/.../HomeViewModel.kt` | `loadApps()` 내 `apps.distinctBy { it.packageName }` 추가 — 그리드·서랍 양쪽에서 중복 앱 미표시 |

### 검증 결과

```
./gradlew test  →  BUILD SUCCESSFUL (245 tasks, failures=0, 전체 회귀 없음)
실기기: 앱 서랍 스크롤 크래시 없음 확인
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `activityName`을 LazyColumn key로 사용 | `packageName`은 멀티 런처 엔트리 앱에서 중복 가능. `activityName`(FQCN)은 ActivityManager가 보장하는 전역 고유값 |
| `distinctBy { it.packageName }` 위치를 ViewModel의 `loadApps()`로 선택 | Repository 계층은 PackageManager 원본 데이터를 그대로 반환하는 단일 책임 유지. 중복 제거는 "런처에서 앱당 1개 표시" 라는 UI 정책이므로 ViewModel에서 처리 |
| 두 수정을 모두 적용 | `activityName` key — animateItem 외 다른 LazyColumn 사용처의 미래 크래시 예방. `distinctBy` — 그리드 셀 앱 목록의 잠재적 중복도 동시 해결 |

---

## [2026-02-20] Step 12 — 비주얼 고도화 및 테마 시스템 구현

### 목표
Android Studio 기본 템플릿 상태로 남아있던 런처 UI에 커스텀 컬러 시스템, Glassmorphism, spring 애니메이션, Noto Sans KR 폰트를 적용하여 스트리머 팬덤 감성의 세련된 런처 UI를 완성한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `app/src/main/res/font/noto_sans_kr.ttf` | **신규** — Google Fonts GitHub에서 Noto Sans KR Variable Font (~10.4MB) 다운로드 및 배치 |
| 2 | `app/.../ui/theme/Type.kt` | `NotoSansKrFontFamily` (Variable 폰트, `FontVariation.Settings` Weight 400/500/700) 정의; Material3 Typography 15개 스타일 전면 재정의 (`@file:OptIn(ExperimentalTextApi::class)` 적용) |
| 3 | `app/.../ui/theme/Color.kt` | `StreamLauncherColors` `@Immutable` data class 추가 (7개 컬러 속성); `DarkStreamLauncherColors` / `LightStreamLauncherColors` 정적 인스턴스; `LocalStreamLauncherColors = staticCompositionLocalOf { DarkStreamLauncherColors }` |
| 4 | `app/.../ui/theme/Theme.kt` | `StreamLauncherTheme` object + `colors` 접근자 추가; `CompositionLocalProvider(LocalStreamLauncherColors provides streamColors)` 래핑; Dynamic Color(API 31+) 연동 — `colorScheme.primary` → `accentPrimary`, `gridBorderExpanded`, `searchBarFocused` 등 동적 반영 |
| 5 | `app/.../ui/modifier/GlassEffect.kt` | **신규** — `fun Modifier.glassEffect(blurRadius, overlayColor)`: API 31+ `Modifier.blur()` + `drawBehind`, API 28-30 `drawBehind` 폴백 |
| 6 | `feature/launcher/.../ui/HomeScreen.kt` | `tween(400ms, FastOutSlowInEasing)` → `spring(DampingRatioLowBouncy, StiffnessMedium)` 교체 (두 축 모두); `GridCellContent`에 `Modifier.border()` 추가 — 확장: `primary` 2dp, 축소: `outlineVariant` 1dp |
| 7 | `app/.../navigation/AppDrawerScreen.kt` | `AppDrawerItem`에 `modifier: Modifier` 파라미터 추가; `LazyColumn items`에 `Modifier.animateItem(fadeInSpec=tween(300), placementSpec=spring(NoBouncy, MediumLow))` 적용; `OutlinedTextFieldDefaults.colors(focusedBorderColor, cursorColor, focusedLeadingIconColor)` 적용 |
| 8 | `app/.../navigation/CrossPagerNavigation.kt` | DownPage: `Surface` → `Box` 구조 변경 — 배경 레이어(`glassEffect(overlayColor=glassSurface)`) + 콘텐츠 레이어(선명) 분리 |

#### 폰트 구성 (Variable Font)

```kotlin
val NotoSansKrFontFamily = FontFamily(
    Font(R.font.noto_sans_kr, FontWeight.Normal,  FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.noto_sans_kr, FontWeight.Medium,  FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.noto_sans_kr, FontWeight.Bold,    FontVariation.Settings(FontVariation.weight(700))),
)
```

#### 컬러 시스템 구조

```
StreamLauncherColors
├─ gridBorder          — 그리드 셀 기본 테두리
├─ gridBorderExpanded  — 확장 셀 포인트 테두리 (accentPrimary 연동)
├─ searchBarFocused    — 검색바 포커스 테두리 (accentPrimary 연동)
├─ glassSurface        — 글래스 배경 반투명 색상
├─ glassOnSurface      — 글래스 위 콘텐츠 색상
├─ accentPrimary       — 주 포인트 컬러 (API 31+: colorScheme.primary)
└─ accentSecondary     — 보조 포인트 컬러 (API 31+: colorScheme.tertiary)
```

#### DownPage Glass 구조

```
Box (fillMaxSize, graphicsLayer alpha)
├─ Box [배경 레이어] — glassEffect(overlayColor = glassSurface)
│   └─ drawBehind { drawRect(glassSurface) } + blur(20.dp) [API 31+]
└─ Box [콘텐츠 레이어] — navigationBarsPadding
    └─ appDrawerContent()  ← 선명하게 렌더링
```

### 검증 결과

```
./gradlew test           →  BUILD SUCCESSFUL (245 actionable tasks, failures=0)
./gradlew assembleDebug  →  BUILD SUCCESSFUL (162 actionable tasks)
전체 단위 테스트 회귀 없음 (총 56개 테스트 통과)
```

트러블슈팅:
- `FontVariation.Settings` → `@ExperimentalTextApi` Opt-in 필요 → `@file:OptIn(...)` 추가로 해결

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| Variable Font 단일 파일 (`NotoSansKR[wght].ttf`) 사용 | Google Fonts 저장소에서 Weight별 개별 파일 대신 Variable Font 제공. 단일 파일로 100–900 전 weight 커버, APK 내 폰트 파일 수 최소화 |
| `staticCompositionLocalOf` 사용 (vs `compositionLocalOf`) | 테마 색상은 리컴포지션 중간에 변경되지 않음. `staticCompositionLocalOf`는 변경 시 전체 트리를 리컴포즈하지만, 테마 전환은 드문 이벤트이므로 성능상 이점 (불필요한 람다 생성 없음) |
| Dynamic Color(API 31+)에서 `colorScheme.*`을 `StreamLauncherColors`에 맵핑 | Material3 Dynamic Color가 이미 배경화면에서 추출한 색상을 제공. 이를 `StreamLauncherColors`의 accent/border 속성에 연결하면 별도 Palette 추출 없이 배경화면-런처 UI 색상 일체감 달성 |
| `feature:launcher`에서 `MaterialTheme.colorScheme.*` 직접 사용 (vs `StreamLauncherColors`) | `feature:launcher`는 `app` 모듈을 참조할 수 없음 (역방향 의존성 금지). `StreamLauncherColors`는 `app` 모듈에 선언되어 있으므로, `feature` 모듈에서는 Material3의 `colorScheme.primary` / `outlineVariant`를 사용. Theme.kt에서 두 시스템이 같은 색상으로 연동되므로 시각적 일관성 유지 |
| `spring(DampingRatioLowBouncy, StiffnessMedium)` | tween 400ms 대비 물리 기반 애니메이션으로 더 자연스러운 "쫀득한" 탄성감 제공. `LowBouncy`는 오버슈트 없이 적절한 탄성, `StiffnessMedium`은 즉각 반응성과 부드러움의 균형 |
| `Modifier.glassEffect` — 배경 레이어에만 적용, 콘텐츠 레이어 분리 | `Modifier.blur()`는 해당 Composable 내 콘텐츠를 블러 처리. 콘텐츠(앱 목록, 검색바)가 흐려지지 않도록 배경 레이어와 콘텐츠 레이어를 별도 Box로 분리 |
| `animateItem()` — `fadeOutSpec = tween(200)` 포함 | 검색 결과 필터링 시 아이템 제거 애니메이션도 자연스럽게 처리. `placementSpec = spring(NoBouncy, MediumLow)` — 리스트 아이템 위치 이동에는 탄성 없이 부드럽게 |

---

## [2026-02-20] Step 11 — 실제 앱 실행 기능 구현 및 햅틱 피드백 추가

### 목표
`HomeSideEffect.NavigateToApp` 수신 시 `Log.d()`만 출력되던 스텁을 실제 앱 실행 로직으로 교체한다.
확장된 그리드 셀 내 앱 아이템을 클릭 가능하게 하고, 클릭 시 상태를 초기화한다.
그리드 셀 토글, 앱 아이템 선택, 앱 서랍 아이템 선택 모두에 햅틱 피드백을 추가한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `app/.../MainActivity.kt` | `NavigateToApp` 수신 시 `packageManager.getLaunchIntentForPackage()` → `startActivity()` 실행; null 반환 및 `ActivityNotFoundException` try-catch 방어 처리 |
| 2 | `feature/launcher/.../HomeViewModel.kt` | `ClickApp` 처리 시 `sendEffect(NavigateToApp)` **후** `resetHome()` 호출 — 그리드 접힘 + 검색어 초기화 |
| 3 | `feature/launcher/.../ui/HomeScreen.kt` | 확장 셀 `LazyColumn` 내 앱 `Text`에 `fillMaxWidth + clickable { ClickApp }` 추가; `Surface.onClick` 및 앱 아이템 `clickable`에 `LocalHapticFeedback.LongPress` 추가 |
| 4 | `app/.../navigation/AppDrawerScreen.kt` | `AppDrawerItem` `clickable` 내 `LocalHapticFeedback.LongPress` 추가 |

#### 앱 실행 흐름

```
사용자 클릭 (그리드 셀 앱 or 앱 서랍 아이템)
  → HomeIntent.ClickApp(app)
  → HomeViewModel: sendEffect(NavigateToApp(packageName)) + resetHome()
  → MainActivity LaunchedEffect:
      getLaunchIntentForPackage(packageName)
      ├─ 성공 → startActivity(intent)
      └─ null  → Log.w (삭제된 앱 등)
      └─ ActivityNotFoundException → Log.w (catch)
  → 사용자가 런처로 복귀 시 이미 resetHome() 적용된 초기 상태
```

#### 상태 초기화 정책

| 진입 경로 | 트리거 | 결과 |
|-----------|--------|------|
| 앱 실행 | `ClickApp` → `resetHome()` | 그리드 접힘, 검색어 초기화 |
| 홈 버튼 복귀 | `onNewIntent` → `ResetHome` → `resetHome()` | 동일 |

### 검증 결과

```
./gradlew testDebugUnitTest  →  BUILD SUCCESSFUL (failures=0, 전체 회귀 없음)
./gradlew assembleDebug      →  BUILD SUCCESSFUL
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `getLaunchIntentForPackage` null 체크 | 앱 목록 로드 후 앱이 삭제된 경우 null 반환 가능. Crash 방지를 위해 null 분기 처리 |
| `ActivityNotFoundException` try-catch | `startActivity`는 매니페스트에 exported=false인 경우 등 드물게 throw. 런처 앱 특성상 Crash는 치명적이므로 방어 처리 |
| `sendEffect` 직후 `resetHome()` 호출 | 이펙트와 상태 변경을 분리하지 않고 하나의 인텐트 핸들러에서 처리. 앱 전환 후 백스택으로 런처 복귀 시에도 깨끗한 상태 보장 |
| `LocalHapticFeedback` Compose API 사용 | Android View의 `View.performHapticFeedback()`보다 Compose 레이어에서 통합 관리 가능. `HapticFeedbackType.LongPress`는 앱 선택 같은 "실행" 동작에 적합한 피드백 강도 |
| 그리드 셀 토글과 앱 아이템 클릭 모두 햅틱 | 셀 확장·축소도 사용자 행동 변화이므로 동일한 피드백 제공. 일관된 터치 경험 |


---

## [2026-02-20] Step 10 — 앱 서랍(App Drawer) 고도화 및 초성 검색

### 목표
CrossPagerNavigation의 DownPage(`Text("App Drawer")` 스텁)를 실제 앱 서랍으로 교체한다. 전체 앱 리스트, 한국어 초성 검색, 아이콘 표시, 앱 실행 기능을 구현한다.

### 변경 사항

| # | 파일 | 변경 내용 |
|---|------|----------|
| 1 | `core/domain/.../util/ChosungMatcher.kt` | **신규** — `extractChosung` + `matchesChosung` 순수 JVM 유틸 |
| 2 | `core/domain/.../util/ChosungMatcherTest.kt` | **신규** — 11개 단위 테스트 |
| 3 | `feature/launcher/.../HomeContract.kt` | `HomeState`에 `searchQuery`, `filteredApps` 추가; `HomeIntent.Search` 추가 |
| 4 | `feature/launcher/.../HomeViewModel.kt` | `_searchQuery MutableStateFlow` + `debounce(100ms)` 검색 디바운싱; `filterApps`, `resetHome` 헬퍼; `loadApps` 시 `filteredApps` 갱신 |
| 5 | `feature/launcher/.../HomeViewModelTest.kt` | 신규 6개 테스트 추가 (총 18개) |
| 6 | `app/.../navigation/AppIcon.kt` | **신규** — `produceState` + `Dispatchers.IO` 비동기 아이콘 로딩 |
| 7 | `app/.../navigation/AppDrawerScreen.kt` | **신규** — `FocusRequester` 자동 포커스, `OutlinedTextField`, `LazyColumn`; `derivedStateOf` 제거 버그픽스 포함 |
| 8 | `app/.../navigation/CrossPagerNavigation.kt` | `appDrawerContent: @Composable () -> Unit` 파라미터 추가; DownPage 스텁 교체; 페이지 이탈 시 키보드 자동 숨김 |
| 9 | `app/.../MainActivity.kt` | `AppDrawerScreen` 연결 (`appDrawerContent` 슬롯 전달) |

#### ChosungMatcher 로직

```kotlin
// 한글 완성형 (U+AC00~U+D7A3) → 초성 인덱스 = (char - 0xAC00) / (21 * 28)
// 단일 자모 (U+3131~U+314E) 및 비한글 → 그대로 통과

fun matchesChosung(label: String, query: String): Boolean
// query가 순수 자모(ㄱ~ㅎ)이면 → extractChosung(label).contains(query)
// 그 외 → label.lowercase().contains(query.lowercase())
```

#### HomeViewModel 검색 플로우

```
사용자 입력 → Search(query)
├─ 즉시: updateState { copy(searchQuery = query) }  ← TextField 반응성
└─ 100ms 후 (debounce): filterApps(appsInCells.flatten(), query) → updateState { copy(filteredApps = ...) }

loadApps() 완료 시: filteredApps = filterApps(allSorted, currentState.searchQuery)
ResetHome 시: _searchQuery.value = "" + searchQuery, filteredApps 초기화
```

#### AppDrawerScreen 구조

```
AppDrawerScreen(state: HomeState, onIntent: (HomeIntent) -> Unit)
├─ OutlinedTextField (focusRequester, leadingIcon: Search, trailingIcon: Clear)
├─ LazyColumn (key = packageName)
│   └─ AppDrawerItem: AppIcon(40dp) + label Text
└─ LaunchedEffect(Unit) → focusRequester.requestFocus()
```

### 검증 결과

```
ChosungMatcherTest   →  11개 통과 (failures=0)
HomeViewModelTest    →  18개 통과 (기존 12 + 신규 6, failures=0)
./gradlew assembleDebug  →  BUILD SUCCESSFUL
./gradlew test           →  BUILD SUCCESSFUL (failures=0, 전체 회귀 없음)
```

### 설계 결정 및 근거

| 결정 | 근거 |
|------|------|
| `ChosungMatcher`를 `core:domain`에 배치 | 순수 JVM 로직으로 Android 의존성 없음. JVM 테스트로 빠르게 검증 가능. `feature:launcher`와 향후 다른 모듈에서도 재사용 가능 |
| `debounce(100ms)` + 즉시 `searchQuery` 업데이트 분리 | `searchQuery`는 즉시 업데이트해 TextField가 사용자 입력에 끊김 없이 반응. `filteredApps` 계산은 100ms 디바운싱으로 빠른 타이핑 시 불필요한 연산 절감 |
| `_searchQuery MutableStateFlow` 별도 관리 | `ResetHome` 등 외부에서 검색어를 초기화할 때 debounce 파이프라인도 함께 리셋되어야 하므로 Flow 기반 관리가 적합 |
| `produceState` + `Dispatchers.IO`로 아이콘 로딩 | `PackageManager.getApplicationIcon()`은 IPC를 포함하므로 UI 스레드 블로킹 위험. `key1 = packageName`으로 동일 앱 재계산 방지 |
| `state.filteredApps` 직접 참조 (derivedStateOf 제거) | `derivedStateOf`는 Compose Snapshot State 변경만 감지함. `HomeState`는 일반 data class이므로 감지 대상 외. 람다가 초기 `state`를 캡처해 항상 빈 리스트 반환하는 버그 → 직접 참조로 수정 |
| 페이지 이탈 시 `LocalSoftwareKeyboardController.hide()` | DownPage를 벗어나도 키보드가 다른 페이지에 남아 레이아웃을 밀어올리는 현상 방지. `verticalPagerState.currentPage` 감지 |
| `appDrawerContent` 슬롯을 `CrossPagerNavigation` 파라미터로 | HomeViewModel 인스턴스는 `app` 모듈 `MainActivity`에서만 생성됨. 서랍 컴포저블을 슬롯으로 주입하면 DownPage가 ViewModel에 직접 의존하지 않아도 됨 (관심사 분리) |


---

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
