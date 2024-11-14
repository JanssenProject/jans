package com.example.fido2.usecase

import com.example.fido2.repository.settings.SettingsRepository

class SettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke() = runCatching { repository.getServerUrl() }
    suspend operator fun invoke(serverUrl: String) = runCatching { repository.saveServerUrl(serverUrl) }
}