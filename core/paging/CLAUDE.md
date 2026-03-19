# core:paging

Paging 3 추상화 계층. `PagerProvider` 인터페이스와 UseCase 래퍼를 제공하여
`feature:preset-market` 이 `core:data` 에 직접 의존하지 않도록 분리.

## 존재 이유

`feature:preset-market` 은 Paging 기능이 필요하지만 `core:data` 에 직접 의존하면
아키텍처 계층 위반 (feature → data 직접 결합). `core:paging` 이 인터페이스를 정의하고
`core:data` 가 구현체를 바인딩함으로써 의존 방향을 유지.

```
feature:preset-market → core:paging (인터페이스)
core:data              → core:paging (인터페이스 구현 + Hilt 바인딩)
```

## 패키지 구조

```
org.comon.streamlauncher.paging
├── RecentPresetsPagerProvider.kt   # Flow<PagingData<MarketPreset>> 제공 인터페이스
├── SearchPresetsPagerProvider.kt   # query 기반 검색 Flow<PagingData<MarketPreset>> 인터페이스
├── GetRecentPresetsPagerUseCase.kt # RecentPresetsPagerProvider 래퍼 UseCase
└── SearchPresetsPagerUseCase.kt    # SearchPresetsPagerProvider 래퍼 UseCase
```

## 인터페이스

```kotlin
interface RecentPresetsPagerProvider {
    fun provide(): Flow<PagingData<MarketPreset>>
}

interface SearchPresetsPagerProvider {
    fun provide(query: String): Flow<PagingData<MarketPreset>>
}
```

## UseCase 패턴

```kotlin
class GetRecentPresetsPagerUseCase @Inject constructor(
    private val provider: RecentPresetsPagerProvider,
) {
    operator fun invoke(): Flow<PagingData<MarketPreset>> = provider.provide()
}
```

## 의존성

- `:core:domain` (MarketPreset 모델 참조)
- `paging-runtime` (PagingData, PagingSource)
- `hilt-android` (UseCase @Inject constructor)

## 규칙

- `PagingSource` 구현체는 이 모듈에 두지 않음 → `core:data/paging/` 에 위치
- Hilt 바인딩(`@Binds` — 인터페이스 → 구현체) 은 `core:data` 의 DI 모듈에서 처리
- 이 모듈은 Android 뷰/Compose 의존성 없이 최소한으로 유지
- namespace: `dev.comon.streamlauncher.paging` (build.gradle.kts 기준)
