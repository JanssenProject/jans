package com.example.fido2.database.local

import com.example.fido2.database.AppDatabase
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
}