package com.example.fido2.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.fido2.usecase.SettingsUseCase
import com.example.fido2.utils.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SettingsScreenViewModel(
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {
    val scope = MainScope()

    var settingsState by mutableStateOf(SettingsState())

    suspend fun getServerUrl(): String {
        return settingsUseCase.invoke().getOrNull() ?: AppConfig.SERVER_BASE_URL
    }

    suspend fun saveServerUrl() {
        settingsState.serverUrl?.let { settingsUseCase.invoke(it) }
    }
}