# 개인 서버 API 전환 시 ConnectivityInterceptor 도입 가이드

Firebase → 개인 서버 API로 전환하는 시점에 `NetworkConnectivityChecker`를 OkHttp
인터셉터 레이어로 옮겨, 각 ViewModel이 직접 네트워크 상태를 주입받지 않도록 정리하는
작업 가이드다.

---

## 배경 및 현재 구조 (작성 시점: 2026-04-03)

현재 네트워크 오류 처리 방식:

- **Retrofit 호출** (Chzzk, YouTube): 예외 타입(`UnknownHostException`, `ConnectException`)으로
  네트워크 단절 여부 추정 → 단, `ConnectException`은 "서버 다운"과 "네트워크 없음"을
  구분하지 못하는 한계가 있다.
- **Firebase 호출**: `FirebaseNetworkException` 타입으로 구분.
- 각 ViewModel이 `NetworkConnectivityChecker`를 생성자 주입받아
  `connectivityChecker.isUnavailable()`로 정확한 네트워크 상태를 직접 확인.

Firebase를 개인 서버 API(Retrofit)로 교체하면 **모든 외부 호출이 OkHttp를 경유**하게
되므로, 인터셉터 한 곳에서 네트워크 단절을 감지하고 전용 예외를 던지는 방식이 더
깔끔하다.

---

## 변경 목표

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 네트워크 단절 감지 위치 | 각 ViewModel | OkHttp Interceptor |
| ViewModel 생성자 | `NetworkConnectivityChecker` 주입 | 제거 |
| 오류 구분 방식 | `connectivityChecker.isUnavailable()` | `error.isNetworkDisconnected()` |
| `ConnectivityChecker` 사용처 | 10개 ViewModel | Interceptor 1곳 |

---

## 구현 단계

### Step 1 — NoNetworkException 정의

`core/network/.../error/` 패키지에 추가한다.

```kotlin
// core/network/.../error/NoNetworkException.kt
package org.comon.streamlauncher.network.error

import java.io.IOException

/** ConnectivityInterceptor가 네트워크 미연결 상태에서 throw하는 예외 */
class NoNetworkException : IOException("No network connection")
```

`IOException`을 상속해야 Retrofit이 정상적으로 `onFailure`로 전달한다.

---

### Step 2 — ConnectivityInterceptor 구현

`core/network/.../connectivity/` 패키지에 추가한다.

```kotlin
// core/network/.../connectivity/ConnectivityInterceptor.kt
package org.comon.streamlauncher.network.connectivity

import okhttp3.Interceptor
import okhttp3.Response
import org.comon.streamlauncher.network.error.NoNetworkException
import javax.inject.Inject

class ConnectivityInterceptor @Inject constructor(
    private val connectivityChecker: NetworkConnectivityChecker,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (connectivityChecker.isUnavailable()) throw NoNetworkException()
        return chain.proceed(chain.request())
    }
}
```

---

### Step 3 — NetworkModule에 인터셉터 추가

`provideOkHttpClient`(기본 클라이언트)와 개인 서버용 클라이언트 모두에 추가한다.
Chzzk / YouTube 전용 클라이언트에는 필요에 따라 선택 적용.

```kotlin
// core/network/.../di/NetworkModule.kt
@Provides
@Singleton
fun provideOkHttpClient(
    @ApplicationContext context: Context,
    connectivityInterceptor: ConnectivityInterceptor,   // 추가
): OkHttpClient {
    val packageName = context.packageName
    val sha1 = getSigningCertSha1(context)
    return OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(connectivityInterceptor)        // 추가 (logging보다 먼저)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("X-Android-Package", packageName)
                .apply { if (sha1 != null) header("X-Android-Cert", sha1) }
                .build()
            chain.proceed(request)
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }
        .build()
}
```

> **주의**: `addInterceptor` 순서 — ConnectivityInterceptor를 **logging interceptor보다 앞에**
> 추가해야 네트워크 없을 때 OkHttp 로그에 불필요한 요청 시도가 찍히지 않는다.

---

### Step 4 — NetworkErrorHelper에 NoNetworkException 추가

`FirebaseNetworkException` 항목을 제거하고 `NoNetworkException`으로 교체한다.

```kotlin
// core/network/.../error/NetworkErrorHelper.kt
private fun matchesDisconnected(t: Throwable): Boolean =
    t is NoNetworkException ||          // 추가
    t is UnknownHostException ||
    t is ConnectException
    // FirebaseNetworkException 제거 — Firebase SDK 더 이상 사용 안 함
```

`getErrorMessage()`와 `isTimeout()`은 변경 없음.

---

### Step 5 — ViewModel에서 NetworkConnectivityChecker 제거

각 ViewModel에서 다음 작업을 반복한다.

```kotlin
// Before
@HiltViewModel
class FooViewModel @Inject constructor(
    private val connectivityChecker: NetworkConnectivityChecker,
    private val fooUseCase: FooUseCase,
) : BaseViewModel<...>(...) {

    private fun doSomething() {
        viewModelScope.launch {
            fooUseCase().onFailure { error ->
                if (connectivityChecker.isUnavailable()) {
                    sendEffect(SideEffect.ShowNetworkError)
                } else {
                    sendEffect(SideEffect.ShowError(error.getErrorMessage("작업명")))
                }
            }
        }
    }
}

// After
@HiltViewModel
class FooViewModel @Inject constructor(
    private val fooUseCase: FooUseCase,   // connectivityChecker 제거
) : BaseViewModel<...>(...) {

    private fun doSomething() {
        viewModelScope.launch {
            fooUseCase().onFailure { error ->
                // getErrorMessage가 NoNetworkException → "인터넷 연결을 확인해주세요" 처리
                sendEffect(SideEffect.ShowError(error.getErrorMessage("작업명")))
            }
        }
    }
}
```

ShowNetworkError SideEffect가 별도로 존재한다면 ShowError로 통합하거나 유지해도 된다.
`getErrorMessage`가 "인터넷 연결을 확인해주세요" 메시지를 반환하므로 동작은 동일하다.

---

### Step 6 — ConnectivityModule 정리 (선택)

`NetworkConnectivityChecker`가 인터셉터에서만 쓰인다면 `ConnectivityModule`은 그대로
유지한다. 더 이상 ViewModel에 직접 노출할 필요가 없으므로 internal로 범위를 좁혀도 된다.

---

### Step 7 — 테스트 수정

ViewModel 테스트에서 `connectivityChecker` mock을 제거한다.

```kotlin
// Before
private fun makeViewModel() = FooViewModel(
    connectivityChecker = mockk { every { isUnavailable() } returns false },
    fooUseCase = mockUseCase,
)

// After
private fun makeViewModel() = FooViewModel(
    fooUseCase = mockUseCase,
)
```

인터셉터 자체 테스트는 별도로 추가한다.

```kotlin
class ConnectivityInterceptorTest {
    private val checker = mockk<NetworkConnectivityChecker>()
    private val interceptor = ConnectivityInterceptor(checker)

    @Test
    fun `네트워크 없을 때 NoNetworkException throw`() {
        every { checker.isUnavailable() } returns true
        assertThrows<NoNetworkException> {
            interceptor.intercept(mockChain())
        }
    }

    @Test
    fun `네트워크 있을 때 정상 진행`() {
        every { checker.isUnavailable() } returns false
        val response = mockk<Response>()
        val chain = mockChain(response)
        assertEquals(response, interceptor.intercept(chain))
    }

    private fun mockChain(response: Response = mockk()): Interceptor.Chain = mockk {
        every { request() } returns mockk(relaxed = true)
        every { proceed(any()) } returns response
    }
}
```

---

## 전환 후 파일별 변경 요약

| 파일 | 변경 |
|------|------|
| `error/NoNetworkException.kt` | 신규 생성 |
| `connectivity/ConnectivityInterceptor.kt` | 신규 생성 |
| `di/NetworkModule.kt` | `provideOkHttpClient`에 인터셉터 파라미터+등록 추가 |
| `error/NetworkErrorHelper.kt` | `matchesDisconnected`에 `NoNetworkException` 추가, `FirebaseNetworkException` 제거 |
| `*ViewModel.kt` (10개) | `NetworkConnectivityChecker` 생성자 파라미터 제거, 오류 처리 단순화 |
| `*ViewModelTest.kt` (10개) | `connectivityChecker` mock 제거 |
| `ConnectivityModule.kt` | 유지 (인터셉터가 여전히 사용) |

---

## 주의사항

- **인터셉터 적용 범위**: `ConnectivityInterceptor`는 추가된 `OkHttpClient`를 사용하는
  Retrofit 인스턴스에만 적용된다. 개인 서버용 클라이언트를 별도 `@Qualifier`로 만들 경우
  해당 클라이언트에도 동일하게 추가해야 한다.
- **인터셉터 순서**: `addInterceptor` 호출 순서가 실행 순서다.
  ConnectivityInterceptor → HeaderInterceptor → LoggingInterceptor 순을 권장한다.
- **Race condition**: 인터셉터가 네트워크 상태를 확인하는 순간과 실제 요청이 나가는 순간
  사이에 네트워크가 끊길 수 있다. 이 경우 `ConnectException`/`UnknownHostException`으로
  떨어지며 `isNetworkDisconnected()`가 동일하게 처리하므로 실용상 문제없다.
