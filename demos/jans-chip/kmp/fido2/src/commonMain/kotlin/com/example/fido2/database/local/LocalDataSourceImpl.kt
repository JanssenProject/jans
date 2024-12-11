package com.example.fido2.database.local

import com.example.fido2.database.AppDatabase
import com.example.fido2.model.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.example.fido2.model.OIDCClient
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.fido.config.FidoConfiguration

class LocalDataSourceImpl(
    private val db: AppDatabase,
    private val dispatcher: CoroutineDispatcher
): LocalDataSource {

    override suspend fun getOIDCClient(): OIDCClient? {
        return withContext(dispatcher) {
            val allEntities = db.oidcClientDao().getAll()
            if (allEntities.isEmpty()) {
                null
            } else {
                allEntities.first()
            }
        }
    }

    override suspend fun saveOIDCClient(client: OIDCClient) {
        withContext(dispatcher) {
            db.oidcClientDao().insert(client)
        }
    }

    override suspend fun updateOIDCClient(client: OIDCClient) {
        withContext(dispatcher) {
            db.oidcClientDao().update(client)
        }
    }

    override suspend fun deleteAllOIDCClients() {
        withContext(dispatcher) {
            db.oidcClientDao().deleteAll()
        }
    }

    override suspend fun getOPConfiguration(): OPConfiguration? {
        return withContext(dispatcher) {
            val allEntities = db.opConfigurationDao().getAll()
            if (allEntities.isEmpty()) {
                null
            } else {
                allEntities.first()
            }
        }
    }

    override suspend fun saveOPConfiguration(opConfiguration: OPConfiguration) {
        withContext(dispatcher) {
            db.opConfigurationDao().insert(opConfiguration)
        }
    }

    override suspend fun updateOPConfiguration(opConfiguration: OPConfiguration) {
        withContext(dispatcher) {
            db.opConfigurationDao().update(opConfiguration)
        }
    }

    override suspend fun getFidoConfiguration(): FidoConfiguration? {
        return withContext(dispatcher) {
            val allEntities = db.fidoConfigurationDao().getAll()
            if (allEntities.isEmpty()) {
                null
            } else {
                allEntities.first()
            }
        }
    }

    override suspend fun saveFidoConfiguration(fidoConfiguration: FidoConfiguration) {
        withContext(dispatcher) {
            db.fidoConfigurationDao().insert(fidoConfiguration)
        }
    }

    override suspend fun getServerUrl(): String {
        return withContext(dispatcher) {
            val settings = db.serverUrlDao().getAll()
            if (settings.isEmpty()) {
                ""
            } else {
                settings.first().serverUrl
            }
        }
    }

    override suspend fun saveServerUrl(serverUrl: String) {
        withContext(dispatcher) {
            val settings = db.serverUrlDao().getAll()
            if (settings.isEmpty()) {
                db.serverUrlDao().insert(AppSettings(serverUrl))
            } else {
                db.serverUrlDao().deleteAll()
                db.serverUrlDao().insert(AppSettings(serverUrl))
            }
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcher) {
            db.oidcClientDao().deleteAll()
            db.serverUrlDao().deleteAll()
            db.appIntegrityDao().deleteAll()
            db.opConfigurationDao().deleteAll()
            db.fidoConfigurationDao().deleteAll()
        }
    }
}