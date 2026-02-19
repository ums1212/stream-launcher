# StreamLauncher 개발 로그

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
