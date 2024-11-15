package com.example.fido2.database.local

import com.example.fido2.model.OIDCClient
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.fido.config.FidoConfiguration

interface LocalDataSource {
    suspend fun getOIDCClient(): OIDCClient?
    suspend fun saveOIDCClient(client: OIDCClient)
    suspend fun updateOIDCClient(client: OIDCClient)
    suspend fun deleteAllOIDCClients()

    suspend fun getOPConfiguration(): OPConfiguration?
    suspend fun saveOPConfiguration(opConfiguration: OPConfiguration)
    suspend fun updateOPConfiguration(opConfiguration: OPConfiguration)

    suspend fun getFidoConfiguration(): FidoConfiguration?
    suspend fun saveFidoConfiguration(fidoConfiguration: FidoConfiguration)

    suspend fun getServerUrl(): String?
    suspend fun saveServerUrl(serverUrl: String)

    suspend fun clearAll()
}