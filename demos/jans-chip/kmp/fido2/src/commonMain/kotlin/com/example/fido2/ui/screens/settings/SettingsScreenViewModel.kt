package com.example.fido2.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.fido2.usecase.AllDataUseCase
import com.example.fido2.usecase.SettingsUseCase
import com.example.fido2.utils.AppConfig
import kotlinx.coroutines.MainScope

class SettingsScreenViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val allDataUseCase: AllDataUseCase
) : ViewModel() {
    val scope = MainScope()

    var settingsState by mutableStateOf(SettingsState())

    suspend fun getServerUrl(): String {
        return settingsUseCase.invoke().getOrNull() ?: AppConfig.SERVER_BASE_URL
    }

    suspend fun saveServerUrl() {
        removeAllData()
        settingsState.serverUrl?.let { settingsUseCase.invoke(it) }
    }

    private suspend fun removeAllData() {
        allDataUseCase.invoke()
    }
}