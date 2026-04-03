package org.comon.streamlauncher.network.connectivity

interface NetworkConnectivityChecker {
    /** 와이파이/모바일 데이터가 모두 꺼진 경우 true */
    fun isUnavailable(): Boolean
}
