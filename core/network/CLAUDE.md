# core:network

Retrofit + OkHttp 기반 네트워크 레이어. API 서비스, 모델, DI 모듈.

## 패키지 구조

```
org.comon.streamlauncher.network
├── api/           # Retrofit 서비스 인터페이스
├── di/            # NetworkModule, NetworkQualifiers
└── model/         # API 응답 모델 (JSON/XML 직렬화)
```

## Retrofit 인스턴스 구분

| Qualifier | 용도 | 컨버터 |
|-----------|------|--------|
| `@JsonRetrofit` | YouTube Data API 등 JSON | kotlinx-serialization |
| `@ChzzkRetrofit` | Chzzk API | kotlinx-serialization |
| (기본 / XML) | RSS 피드 | `XmlConverterFactory` (xmlutil) |

- 새 API 추가 시 적절한 Qualifier의 Retrofit 인스턴스에 바인딩

## API 서비스 규칙

- 인터페이스 메서드: `suspend fun` (단건) 또는 반환 타입 직접 사용
- 동적 URL: `@GET @Url` 패턴 (`RssFeedApi`)
- 응답 모델은 같은 패키지(`network/model/`) 에 위치

## XML 파싱 (xmlutil)

- 패키지: `nl.adaptivity.xmlutil.serialization` (**nl.adaptsoft 아님**)
- XML 클래스: `nl.adaptivity.xmlutil.serialization.XML`
- 어노테이션: `@XmlSerialName`
- xmlutil 버전: `0.91.3` (libs.versions.toml 참고)

## NetworkConstants

- `YOUTUBE_API_KEY` → `local.properties` 의 `youtube.api.key` 에서 BuildConfig 경유
- `buildFeatures { buildConfig = true }` 라이브러리 모듈에서도 필수

## BuildConfig 사용

- 라이브러리 모듈 BuildConfig 접근: 모듈 namespace 기준으로 생성됨
  - 예: `org.comon.streamlauncher.network.BuildConfig.YOUTUBE_API_KEY`

## 의존성 추가 규칙

- 네트워크 관련 라이브러리는 이 모듈에만 추가 (`core:network`)
- feature 모듈에서 직접 Retrofit/OkHttp 의존 금지
