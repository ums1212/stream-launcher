# core:domain

순수 JVM 모듈. Android 의존성 금지. 비즈니스 로직의 중심.

## 패키지 구조

```
org.comon.streamlauncher.domain
├── model/         # 도메인 모델 (data class)
├── repository/    # Repository 인터페이스
├── usecase/       # UseCase 클래스
└── util/          # 순수 유틸리티 (ChosungMatcher 등)
```

## 규칙

### 모델

- 불변 data class 사용 (val 필드)
- Android 타입(`Context`, `Bitmap` 등) 사용 금지
- 기본값 제공으로 테스트 편의성 확보

### Repository 인터페이스

- 반환 타입: `Flow<T>` (스트림) 또는 `suspend fun` (단건)
- 구현체는 `core:data` 에 위치, 여기엔 인터페이스만

### UseCase

- 파일 하나 = 책임 하나
- 생성자: `@Inject constructor(private val repository: XxxRepository)`
- 진입점: `operator fun invoke(...)` 한 개
- 복잡한 로직은 private 함수로 분리
- 예시:
  ```kotlin
  class GetInstalledAppsUseCase @Inject constructor(
      private val repository: AppRepository
  ) {
      operator fun invoke(): Flow<List<AppEntity>> = repository.getInstalledApps()
  }
  ```

### 유틸리티

- Android API 미사용 순수 Kotlin 함수만 허용
- 한국어 처리(`ChosungMatcher`) 등 도메인 특화 로직 포함 가능

## 테스트

- 위치: `src/test/kotlin/`
- 외부 의존 없이 순수 단위 테스트
- Repository는 MockK `mockk<XxxRepository>()` 로 모킹
- Flow 검증은 Turbine 사용
