package com.qoody.shared.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.capture.UnparsedReason
import com.qoody.shared.core.DateProvider
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.repository.UnparsedCaptureRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toLocalDateTime

/** One notification that could not be read, as shown in the "Failed to parse" list. */
data class UnparsedCaptureItem(
    val id: Long,
    val appName: String,
    /** Title and body as the user saw them, one per line. */
    val text: String,
    val date: LocalDate,
    val time: LocalTime,
    val reason: UnparsedReason,
)

sealed interface UnparsedCapturesUiState {
    data object Loading : UnparsedCapturesUiState

    data class Content(
        val items: List<UnparsedCaptureItem>,
    ) : UnparsedCapturesUiState
}

class UnparsedCapturesViewModel(
    private val repository: UnparsedCaptureRepository,
    private val dates: DateProvider,
) : ViewModel() {
    val uiState: StateFlow<UnparsedCapturesUiState> =
        repository.captures
            .map { captures ->
                UnparsedCapturesUiState.Content(
                    captures.map { capture ->
                        val local = capture.postedAt.toLocalDateTime(dates.zone)
                        UnparsedCaptureItem(
                            id = capture.id,
                            appName = capture.appName,
                            text = listOf(capture.title, capture.text).filter { it.isNotBlank() }.joinToString("\n"),
                            date = local.date,
                            time = local.time,
                            reason = capture.reason,
                        )
                    },
                )
            }.stateInViewModel(viewModelScope, UnparsedCapturesUiState.Loading)

    fun onDismiss(id: Long) {
        viewModelScope.launch { repository.dismiss(id) }
    }

    /** The user typed the payment in by hand, so the unreadable notification is resolved. */
    fun onAddedManually(id: Long) = onDismiss(id)

    fun onClearAll() {
        viewModelScope.launch { repository.clear() }
    }
}
