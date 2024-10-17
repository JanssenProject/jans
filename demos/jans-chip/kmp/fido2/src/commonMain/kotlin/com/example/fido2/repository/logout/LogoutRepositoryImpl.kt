package com.example.fido2.repository.logout

import com.example.fido2.database.local.LocalDataSource
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.example.fido2.model.BackendError
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.LogoutResponse
import com.example.fido2.model.OIDCClient
import com.example.fido2.retrofit.ApiClient

class LogoutRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): LogoutRepository {
    private var logoutResponse: LogoutResponse = LogoutResponse()

    override suspend fun logout(): LogoutResponse {
        val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
        if (opConfiguration == null) {
            logoutResponse.isSuccessful = false
            logoutResponse.errorMessage = "OPConfiguration configuration not found in database."
            return logoutResponse
        }
        val oidcClient: OIDCClient? = localDataSource.getOIDCClient()
        if (oidcClient == null) {
            logoutResponse.isSuccessful = false
            logoutResponse.errorMessage = "OpenID client not found in database."
            return logoutResponse
        }
        // Create a call to perform the logout
        println("oidcClient --> $oidcClient")
        val response =
            apiService.logout(
                oidcClient.recentGeneratedAccessToken ?: "",
                "access_token",
                oidcClient.clientId ?: "",
                oidcClient.clientSecret ?: "",
                opConfiguration.revocationEndpoint ?: ""
            )

        if (response.status != HttpStatusCode.OK) {
            val backendError: BackendError = response.body()
            logoutResponse.isSuccessful = false
            logoutResponse.errorMessage = "Error in logout. Error code: ${response.status} Error message: ${backendError.error_description}"
            return logoutResponse
        }

        oidcClient.recentGeneratedAccessToken = null
        localDataSource.updateOIDCClient(oidcClient)
        val logoutResponse = LogoutResponse()
        logoutResponse.isSuccessful = true

        return logoutResponse
    }
}