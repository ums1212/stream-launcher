package org.comon.streamlauncher.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.comon.streamlauncher.domain.repository.WidgetRepository
import org.comon.streamlauncher.domain.repository.WidgetRepository.Companion.MAX_WIDGETS
import javax.inject.Inject

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetRepository: WidgetRepository,
) : ViewModel() {

    val widgetSlots = widgetRepository.getWidgetSlots()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = List(MAX_WIDGETS) { null },
        )

    private val _isEditMode = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isEditMode = _isEditMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)


    init {
        viewModelScope.launch {
            widgetRepository.migrateLegacyData()
        }
    }

    fun setWidgetAtSlot(slot: Int, id: Int) {
        viewModelScope.launch {
            widgetRepository.setWidgetAtSlot(slot, id)
        }
    }

    fun clearSlot(slot: Int) {
        viewModelScope.launch {
            widgetRepository.clearSlot(slot)
        }
    }

    fun setEditMode(isEdit: Boolean) {
        _isEditMode.value = isEdit
    }
}
