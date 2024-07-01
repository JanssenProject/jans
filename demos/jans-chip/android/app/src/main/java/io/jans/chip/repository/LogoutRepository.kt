package io.jans.chip.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import io.jans.chip.model.OPConfiguration
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.AppDatabase
import io.jans.chip.model.LogoutResponse
import io.jans.chip.model.OIDCClient
import retrofit2.Response

class LogoutRepository(context: Context) {
    val TAG = "LogoutRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    var obtainedContext: Context = context
    private var logoutResponse: LogoutResponse = LogoutResponse()

    suspend fun logout(): LogoutResponse {
        val opConfigurationList: List<OPConfiguration> = appDatabase.opConfigurationDao().getAll()
        if (opConfigurationList == null || opConfigurationList.isEmpty()) {
            logoutResponse.isSuccessful = false
            logoutResponse.errorMessage = "OpenID configuration not found in database."
            return logoutResponse
        }
        val oidcClientList: List<OIDCClient> = appDatabase.oidcClientDao().getAll()
        if (oidcClientList == null || oidcClientList.isEmpty()) {
            logoutResponse.isSuccessful = false
            logoutResponse.errorMessage = "OpenID client not found in database."
            return logoutResponse
        }
        val opConfiguration: OPConfiguration = opConfigurationList[0]
        val oidcClient: OIDCClient = oidcClientList[0]
        Log.d("oidcClient.recentGeneratedAccessToken", oidcClient.recentGeneratedAccessToken.toString())
        // Create a call to perform the logout
        val response: Response<Void> =
            ApiAdapter.getInstance(opConfiguration.issuer).logout(
                oidcClient.recentGeneratedAccessToken,
                "access_token",
                "Basic " + Base64.encodeToString(
                    (oidcClient.clientId + ":" + oidcClient.clientSecret).toByteArray(),
                    Base64.NO_WRAP
                ),
                opConfiguration.revocationEndpoint
            )

        if (response.code() != 200) {
            logoutResponse.isSuccessful = false
            logoutResponse.errorMessage = "Error in logout. Error code: ${response.code()} Error message: ${response.message()}"
            return logoutResponse
        }

        oidcClient.recentGeneratedAccessToken = null
        appDatabase.oidcClientDao().update(oidcClient)
        val logoutResponse = LogoutResponse()
        logoutResponse.isSuccessful = true

        return logoutResponse
    }
}