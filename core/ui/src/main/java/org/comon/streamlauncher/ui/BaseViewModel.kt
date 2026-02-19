package org.comon.streamlauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI의 상태를 정의 (Data Class)
interface UiState

// 사용자의 행위 (Sealed Class)
interface UiIntent

// 단발성 이벤트 (Toast, Navigation 등 - Sealed Class)
interface UiSideEffect

// MVI의 핵심 로직을 담은 베이스 클래스
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(
    initialState: S
) : ViewModel() {

    // 1. UI State 관리 (Single Source of Truth)
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    // 2. Side Effect 관리 (단발성 이벤트)
    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // 현재 상태 가져오기 (Helper)
    protected val currentState: S get() = uiState.value

    // Intent 수신부
    abstract fun handleIntent(intent: I)

    // State 업데이트 (Helper)
    protected fun updateState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    // Effect 발생 (Helper)
    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }
}