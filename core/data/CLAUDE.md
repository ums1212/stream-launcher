# core:data

Repository 구현, 로컬 저장소(Room/DataStore), Firebase, Hilt DI 모듈.

## 패키지 구조

```
org.comon.streamlauncher.data
├── di/            # Hilt 모듈 (@Module, @InstallIn)
├── repository/    # Repository 구현체 (*RepositoryImpl)
├── local/
│   └── room/      # AppDatabase, DAO, Entity
├── paging/        # PagingSource 구현체 (MarketPresetPagingSource, SearchMarketPresetPagingSource)
│                  # ※ Pager 인터페이스/UseCase 래퍼는 :core:paging 모듈에 위치
├── remote/
│   └── firestore/ # Firebase DTO 클래스 (MarketPresetDto 등)
├── slp/           # 커스텀 프리셋 포맷 (.slp) — 패커/언패커
├── util/          # DateParser, ImageCompressor, WallpaperHelperImpl 등
└── worker/        # WorkManager Worker 클래스 (FeedSyncWorker)
```

## 규칙

### Repository 구현체

- `core:domain` 의 인터페이스를 구현 (`... : XxxRepository`)
- I/O 작업: `flowOn(Dispatchers.IO)` 또는 `withContext(Dispatchers.IO)` 필수
- 에러 처리: `catch { emit(fallback) }` 또는 호출자에게 예외 전파 (UseCase에서 처리)
- DataStore 캐시 패턴: stale-먼저-emit (캐시 → 네트워크 순)

### Hilt DI 모듈

- `@Binds`: 인터페이스 → 구현체 바인딩 (abstract fun)
- `@Provides`: 외부 객체(`PackageManager`, `FirebaseFirestore` 등) 제공
- 한 모듈에 `@Binds`와 `@Provides` 혼용 시 `abstract class + companion object` 구조:
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  abstract class RepositoryModule {
      @Binds @Singleton
      abstract fun bindXxx(impl: XxxImpl): XxxRepository

      companion object {
          @Provides @Singleton
          fun provideExternalDep(): ExternalDep = ...
      }
  }
  ```

### Room

- Entity 클래스명: `*Entity` (도메인 모델과 구분)
- DAO: suspend fun (단건) / Flow (스트림)
- DB 마이그레이션은 `Migration` 객체로 명시적 처리

### DataStore

- JSON 직렬화: `kotlinx-serialization-json`
- 키 상수는 파일 최상단 `private val` 으로 선언
- 여러 DataStore 인스턴스 사용 시 `@Named("...")` Qualifier로 구분

### Firebase

- BOM 34.x: `firebase-firestore`, `firebase-auth`, `firebase-storage` (접미사 `-ktx` 없음)
- Coroutine 연동: `.await()` (ktx 확장) 또는 Task → Flow 변환

### SLP 포맷 (.slp)

- `SlpManifest` → `SlpPacker` → `.slp` zip 파일
- `SlpUnpacker` → `SlpMapper` → 도메인 모델
- 바이너리 처리는 `data/slp/` 패키지 내에서만

## 테스트

- `unitTests { isReturnDefaultValues = true }` 설정 적용됨
- Android 의존(`Context` 등) mock 시 `@RunWith(MockitoJUnit4Runner::class)` 또는 Robolectric 대신 `mockk(relaxed = true)` 권장
- WorkManager 테스트: `TestListenableWorkerBuilder` 사용
