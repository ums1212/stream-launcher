# AppDrawer 스크롤 성능(Jank) 이슈 원인 및 해결 과정

## 현상
앱 드로어 화면에서 뷰페이저(HorizontalPager)의 페이지를 넘길 때, 특히 10페이지 이상이거나 빠르게 스와이프할 때 화면이 뚝뚝 끊기는(Jank) 프레임 드랍 현상이 심하게 발생했습니다.

## 현상 분석 및 원인
명확히 확인된 핵심 병목 원인은 두 가지입니다.

### 1. 앱 아이콘(`AppIcon`)의 매번 새로 로딩 및 변환 (가장 큰 원인)
- `AppGridPage`가 렌더링될 때 화면에 보이는 `AppIcon` 내부에서 `PackageManager`를 통해 앱 아이콘을 로드하고 수동으로 Bitmap 변환하는 연산이 스와이프 내내 끝없이 반복되었습니다.
- 로딩된 아이콘에 대한 메모리 캐시(Memory Cache)가 없어 엄청난 양의 객체 생성 작업이 발생하고, 가비지 컬렉션(GC)을 과도하게 유발하여 프레임 드랍이 발생했습니다.

### 2. `pagerState.currentPage` 읽기로 인한 화면 전체 불필요한 재구성(Recomposition)
- `AppDrawerScreen` 안에서 하단 페이지 인디케이터(점선)를 렌더링하기 위해 `pagerState.currentPage` 값을 직접 읽어왔습니다.
- 이로 인해 인디케이터만 다시 그려지는 것이 아니라 무거운 `AppDrawerScreen` 뷰 상위부터 통째로 Recomposition 되면서 성능이 저하되었습니다.

## 해결 과정 (Coil 전용 파이프라인 도입)

기존에 메모리 캐시(LruCache)를 수동 적용하여 임시로 완화했었으나, 진정한 런처 급의 스크롤 퍼포먼스를 위해 프로젝트에 포함된 **`Coil` 라이브러리의 캐싱 파이프라인과 하드웨어 가속**에 편입시키는 아키텍처를 적용했습니다.

### 1. `AppIconData` 및 Custom `AppIconFetcher` 구현
- `PackageManager`에서 비트맵을 뽑아오는 작업을 Compose 밖으로 빼내어 Coil `Fetcher` 인터페이스를 구현한 `AppIconFetcher`를 만들었습니다.
- Coil 내부 기본 String 매퍼(URI, 파일 경로 등)와의 충돌로 모든 아이콘이 실패(Error)로 간주되어 기본 UI로 덮이는 문제가 발생하여, String 대신 전용 데이터 클래스 **`AppIconData`**를 생성하여 커스텀 Fetcher만 이를 100% 매핑하여 처리하도록 분리했습니다.

### 2. 하드웨어 가속 및 메모리 캐시 연동
- Coil 컴포넌트 레지스트리에 위 Fetcher를 등록하여, Coil의 강력한 `MemoryCache` 및 `DiskCache`를 코딩 한 줄 없이 적용시켰습니다.
- Background Thread Pool 운용 및 `Hardware Bitmap` 포맷 가속 효과로 GC 압박이 근본적으로 사라졌습니다.

### 3. 화면 재구성(Recomposition) 격리
- 페이지 인디케이터(Canvas Dot)를 별도의 격리된 컴포저블로 분리하여, 페이지가 넘어갈 때 `pagerState.currentPage` 변경에 의한 Recomposition이 전체 뷰를 타격하지 않고 하단 점(Dot)만 다시 그리도록 최적화했습니다.

## 결과
위조치로 인해 10페이지가 넘는 다량의 앱 아이콘 리스트를 아주 빠르게 스와이프하더라도, 첫 캐싱 이후에는 **전혀 버벅임 없는 60/120Hz 풀 프레임의 부드러운 스와이프**가 가능해졌습니다. 또한 에러(삭제된 앱 등) 발생 시 죽지 않고 기본 아이콘(AccountBox)이 표시되도록 안정성도 확보되었습니다.
