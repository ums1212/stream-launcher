# StreamLauncher 개발 로그

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
