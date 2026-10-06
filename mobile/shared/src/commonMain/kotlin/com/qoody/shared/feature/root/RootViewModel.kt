package com.qoody.shared.feature.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qoody.shared.core.stateInViewModel
import com.qoody.shared.domain.model.CustomCategory
import com.qoody.shared.domain.repository.CategoryRepository
import com.qoody.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Which top-level experience the app shows. */
enum class RootUiState {
    Loading,
    Onboarding,
    Main,
}

class RootViewModel(
    settings: SettingsRepository,
    categories: CategoryRepository,
) : ViewModel() {
    val uiState: StateFlow<RootUiState> =
        settings.settings
            .map { if (it.onboardingCompleted) RootUiState.Main else RootUiState.Onboarding }
            .distinctUntilChanged()
            .stateInViewModel(viewModelScope, RootUiState.Loading)

    /** The user's own categories, so every screen can show their names and emojis. */
    val customCategories: StateFlow<List<CustomCategory>> =
        categories.custom.stateInViewModel(viewModelScope, emptyList())
}
