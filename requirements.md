# 프로젝트 명세서: StreamFan Minimal Launcher

## 1. 제품 비전 및 타겟
- **컨셉**: 4방향 스와이프 내비게이션과 동적인 그리드/리스트 애니메이션을 결합한 미니멀리스트 런처.
- **타겟**: 스트리머 팬 및 시청자 (공지사항 확인 및 군더더기 없는 UI 선호).
- **핵심 가치**: 애니메이션을 통한 시각적 즐거움, 효율적인 정보 접근성.

## 2. 기술 스택 및 아키텍처
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture + MVI (Model-View-Intent)
- **Module Strategy**: Multi-module (app, core:domain, core:data, core:ui, feature:launcher 등)
- **Dependency Injection**: Hilt
- **Persistence**: DataStore (향후 Room 확장 고려)
- **Min SDK**: API 26 (Android 8.0) 이상 (2026년 한국 사용자 95% 커버 타겟)

## 3. UI/UX 상세 기능
### A. 홈 화면 (Dynamic Layout)
- **Dynamic Grid**: 2x2 그리드에서 한 칸 클릭 시 그리드 선이 이동하며 해당 영역 확장, 나머지는 축소. 확장된 영역 내 앱 아이콘 리스트 노출.
- **Dynamic List**: 4개 Column/Row 리스트에서 클릭 시 해당 바(Bar) 확대 및 앱 아이콘 노출, 나머지는 최소화.

### B. 4방향 스와이프 내비게이션
- **Up (위)**: 상단(시스템 알림바) / 하단(런처 설정바) 레이아웃.
- **Down (아래)**: 수직 스크롤 앱 서랍 (한글 초성 검색 지원).
- **Left (왼쪽)**: 최근 사용한 앱 + 스트리머/런처 공지사항 피드.
- **Right (오른쪽)**: 안드로이드 위젯 전용 배치 공간.

## 4. 핵심 기능 및 권한
- **App Management**: PackageManager를 이용한 설치 앱 로드 및 실행.
- **Search**: 한글 초성 검색 알고리즘 (예: 'ㄴㅂ' -> '네이버').
- **Permissions**: `QUERY_ALL_PACKAGES`, `HOME` 카테고리 설정, 알림 접근 권한 등 런처 필수 권한 일체.

## 5. 개발 및 테스트 전략 (TDD)
- **Domain**: UseCase 로직에 대한 단위 테스트 필수.
- **Presentation**: MVI State Reducer 및 SideEffect 흐름 검증 (Turbine 사용).
- **Data**: DataStore 및 PackageManager 래퍼 클래스 검증.
- **UI**: 주요 애니메이션 및 스와이프 인터랙션에 대한 Compose UI Test.