package org.comon.streamlauncher.network.error

import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.net.SocketTimeoutException

fun Throwable.isNetworkDisconnected(): Boolean {
    if (this is UnknownHostException || 
        this is ConnectException || 
        this is SocketTimeoutException || 
        this is IOException) {
        return true
    }
    
    // Check class name directly for Firebase exceptions without adding explicit dependency
    if (this.javaClass.simpleName == "FirebaseNetworkException") {
        return true
    }
    
    // Check causes for wrapped exceptions (e.g. StorageException -> FirebaseNetworkException)
    var currentCause = this.cause
    while (currentCause != null) {
        if (currentCause is UnknownHostException || 
            currentCause is ConnectException || 
            currentCause is SocketTimeoutException || 
            currentCause is IOException ||
            currentCause.javaClass.simpleName == "FirebaseNetworkException") {
            return true
        }
        currentCause = currentCause.cause
    }
    
    return false
}

fun Throwable.getErrorMessage(actionName: String): String {
    return if (isNetworkDisconnected()) {
        "네트워크 연결을 확인해주세요"
    } else {
        "${actionName}에 실패했습니다. 잠시 후 다시 시도해주세요."
    }
}
