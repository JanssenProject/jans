package com.example.fido2.repository.settings

import com.example.fido2.database.local.LocalDataSource
import com.example.fido2.utils.AppConfig

class SettingsRepositoryImpl(private val localDataSource: LocalDataSource): SettingsRepository {
    override suspend fun getServerUrl(): String {
        return localDataSource.getServerUrl() ?: AppConfig.SERVER_BASE_URL
    }

    override suspend fun saveServerUrl(serverUrl: String) {
        localDataSource.saveServerUrl(serverUrl)
    }
}