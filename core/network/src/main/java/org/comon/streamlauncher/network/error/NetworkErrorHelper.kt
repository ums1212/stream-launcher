package org.comon.streamlauncher.network.error

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** 와이파이/모바일 데이터가 꺼진 경우 (DNS 실패, 연결 불가, Firebase 네트워크 없음) */
fun Throwable.isNetworkDisconnected(): Boolean {
    if (matchesDisconnected(this)) return true
    var cause = this.cause
    while (cause != null) {
        if (matchesDisconnected(cause)) return true
        cause = cause.cause
    }
    return false
}

private fun matchesDisconnected(t: Throwable): Boolean =
    t is UnknownHostException ||
        t is ConnectException ||
        t.javaClass.simpleName == "FirebaseNetworkException"

/** 서버는 살아있지만 응답이 늦은 경우 */
fun Throwable.isTimeout(): Boolean {
    if (this is SocketTimeoutException) return true
    var cause = this.cause
    while (cause != null) {
        if (cause is SocketTimeoutException) return true
        cause = cause.cause
    }
    return false
}

fun Throwable.getErrorMessage(actionName: String): String = when {
    isNetworkDisconnected() -> "인터넷 연결을 확인해주세요"
    isTimeout() -> "요청 시간이 초과됐습니다. 잠시 후 다시 시도해주세요"
    else -> "${actionName}에 실패했습니다. 잠시 후 다시 시도해주세요"
}
