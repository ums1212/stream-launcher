# 앱 드로어 아이콘 배열 전략

## 고민 (Issue)
현재 앱 드로어에서 페이지별 아이콘을 4*6 배열로 보여주고 있으나, 안드로이드 기기마다 해상도와 화면 비율이 다양하여 해상도가 낮은 기기에서는 아이콘 배열이 깨지거나 잘려 보일 수 있는 문제가 있습니다. 
이를 해결하기 위해 
1) 기기 해상도에 맞춰 자동으로 적절한 배열과 아이콘 크기로 보여줄지
2) 설정 화면에서 사용자가 직접 배열과 아이콘 크기를 조절하게 할지 
방향성에 대한 고민입니다.

## 해결 방안 (Solution)
가장 이상적이고 표준적인 런처 앱들의 방식인 **하이브리드(Hybrid)** 방식을 채택합니다.

* **Step 1: 최소한의 자동 적응 (Responsive Design)**
  * **내용**: 4x6 배열을 유지하되 화면 크기(screenWidthDp 등)나 가용 영역에 맞춰 아이콘의 크기나 여백(padding/spacing)을 동적으로 조절합니다.
  * **목적**: 사용자가 처음 앱을 설치했을 때 어떤 기기에서든 UI가 깨지지 않고 깔끔하게 보이도록(Out-of-the-box 경험) 기본 완성도를 높입니다.

* **Step 2: 커스터마이징 기능 추가 (User Customization)**
  * **내용**: 앱 안정화 후 설정(Settings) 메뉴를 통해 사용자가 직접 그리드 배열(예: 4x5, 4x6, 5x5 등)과 아이콘 크기(예: 80%~120%)를 조절할 수 있도록 합니다.
  * **목적**: 런처 앱 사용자들의 핵심 니즈인 '개인화'를 충족시킵니다. (DataStore 등을 활용하여 전역 상태로 관리)

## 진행 계획
먼저 안전장치로서 **Step 1 (화면 해상도에 맞춰 4x6 배열 유지 및 동적 크기 조절)** 부터 구현을 시작합니다.

---

## 구현 완료 내역 (Implementation Status)

### Step 1: Responsive Design 적용 완료 (2026-02-25)
* **대상 파일**: `feature/apps-drawer/.../AppDrawerScreen.kt`
* **구현 방식**:
  * `AppGridPage` 컴포저블 내부를 `BoxWithConstraints`로 감쌌습니다.
  * 한 페이지에 표시될 실제 가상 영역의 `maxWidth`와 `maxHeight`를 4열(`COLUMNS`), 6행(`ROWS`)으로 나누어 한 셀이 차지할 수 있는 최대 가용 영역(`itemWidth`, `itemHeight`)을 동적으로 계산합니다.
* **안전장치 (Constraints)**:
  * **아이콘 크기**: `iconSize`는 태블릿처럼 큰 화면에서 너무 비대해지는 것을 막기 위해 상한값을 64dp로 제한하고(`minOf(64.dp, ...)`), 아주 작은 기기에서 터치 영역을 확보하기 위해 하한값을 36dp 이상으로 보장합니다(`coerceAtLeast(36.dp)`).
  * **텍스트 크기 및 너비**: 폰트 사이즈(`fontSize`)는 가로 너비(`maxWidth`)가 360dp 미만인 좁은 기기에서는 `10.sp`, 그 외에는 `11.sp`로 유동적으로 변경되도록 처리했습니다. 텍스트 요소 길이 또한 가용 영역의 90% 공간(`textWidth = itemWidth * 0.9f`)만 사용하도록 너비를 제한하여 이름이 긴 앱 아이콘 간의 겹침 현상을 방지했습니다. 
* **결과**:
  * 이제 어떠한 Android 기기(또는 멀티 윈도우 환경, 폴더블 디스플레이)에서도 4x6 배열 격자가 화면 밖으로 짤리거나 깨지지 않고 자연스럽게 렌더링됩니다.

### Step 2: 커스터마이징 기능 추가 완료 (2026-02-25)
* **대상 파일**: `core/domain/.../LauncherSettings.kt`, `feature/launcher/.../SettingsScreen.kt`, `feature/apps-drawer/.../AppDrawerScreen.kt` 등
* **구현 방식**:
  * **Domain & Data**: `LauncherSettings` 데이터 클래스에 `appDrawerGridColumns`, `appDrawerGridRows`, `appDrawerIconSizeRatio` 속성을 추가하고 DataStore 연동 로직(`SaveAppDrawerSettingsUseCase`) 추가.
  * **Settings UI**: 설정 화면에 '앱 서랍' 탭을 신설하고, 사용자가 슬라이더를 통해 행(4~8), 열(3~6), 아이콘 크기 배율(50%~150%)을 직관적으로 조절할 수 있는 UI 구현 (초기화 버튼 포함).
  * **App Drawer UI**: 사용자의 설정값을 `AppDrawerScreen` 및 `AppGridPage` 파라미터로 주입받아, 레이아웃의 동적 계산(행/열 분할 및 아이콘 렌더링 배율 적용)에 즉각 반영되도록 파이프라인 연동.
* **결과**:
  * Step 1의 기본 화면 적응형(Responsive) 처리를 바탕으로, 사용자가 기피하는 비율이나 글씨 크기 제약 없이 취향에 맞게 자유롭게 배열과 크기를 커스터마이징할 수 있는 설정 환경을 제공함.
