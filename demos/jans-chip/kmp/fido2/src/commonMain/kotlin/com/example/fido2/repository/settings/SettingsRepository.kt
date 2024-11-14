package com.example.fido2.repository.settings

interface SettingsRepository {
    suspend fun getServerUrl(): String
    suspend fun saveServerUrl(serverUrl: String)
}