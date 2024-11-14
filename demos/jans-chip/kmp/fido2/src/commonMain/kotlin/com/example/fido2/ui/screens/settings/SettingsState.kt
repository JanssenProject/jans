package com.example.fido2.ui.screens.settings

import com.example.fido2.ui.common.state.ErrorState

data class SettingsState(
    var serverUrl: String? = null,
    var isLoading: Boolean = false,
    val errorState: SettingsErrorState = SettingsErrorState(),
)

data class SettingsErrorState(
    val urlErrorState: ErrorState = ErrorState()
)