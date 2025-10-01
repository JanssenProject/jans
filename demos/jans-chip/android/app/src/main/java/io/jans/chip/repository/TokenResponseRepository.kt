package io.jans.chip.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import io.jans.chip.model.OPConfiguration
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.AppDatabase
import io.jans.chip.factories.DPoPProofFactory
import io.jans.chip.model.OIDCClient
import io.jans.chip.model.TokenResponse
import retrofit2.Response

class TokenResponseRepository(context: Context) {
    val TAG = "TokenResponseRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    var obtainedContext: Context = context
    private var tokenResponse: TokenResponse = TokenResponse(null, null, null)

    suspend fun getToken(
        authorizationCode: String?,
    ): TokenResponse? {
        // Get OPConfiguration and OIDCClient
        val opConfigurationList: List<OPConfiguration> = appDatabase.opConfigurationDao().getAll()
        if (opConfigurationList.isEmpty()) {
            tokenResponse.isSuccessful = false
            tokenResponse.errorMessage = "OpenID configuration not found in database."
            return tokenResponse
        }
        val oidcClientList: List<OIDCClient> = appDatabase.oidcClientDao().getAll()
        if (oidcClientList.isEmpty()) {
            tokenResponse.isSuccessful = false
            tokenResponse.errorMessage = "OpenID client not found in database."
            return tokenResponse
        }
        val opConfiguration: OPConfiguration = opConfigurationList[0]
        val oidcClient: OIDCClient = oidcClientList[0]

        Log.d(
            TAG,
            "dpop token" + opConfiguration.issuer?.let {
                DPoPProofFactory.issueDPoPJWTToken("POST",
                    it
                )
            }
        )

        // Create a call to request a token
        val response: Response<TokenResponse> =
            ApiAdapter.getInstance(opConfiguration.issuer).getToken(
                oidcClient.clientId,
                authorizationCode,
                "authorization_code",
                opConfiguration.issuer,
                oidcClient.scope,
                "Basic " + Base64.encodeToString(
                    (oidcClient.clientId + ":" + oidcClient.clientSecret).toByteArray(),
                    Base64.NO_WRAP
                ),
                opConfiguration.issuer?.let { DPoPProofFactory.issueDPoPJWTToken("POST", it) },
                opConfiguration.tokenEndpoint
            )

        if (response.code() != 200) {
            tokenResponse.isSuccessful = false
            tokenResponse.errorMessage =
                "Error in Token generation. Error code: ${response.code()} Error message: ${response.message()}"
            return tokenResponse
        }

        val tokenResponse: TokenResponse? = response.body()
        Log.d(TAG, "getToken Response :: getIdToken ::" + tokenResponse?.idToken)
        Log.d(TAG, "getToken Response :: getTokenType ::" + tokenResponse?.tokenType)

        if (!response.isSuccessful || tokenResponse == null) {
            tokenResponse?.isSuccessful = false
            tokenResponse?.errorMessage =
                "Error in Token generation. Error code: ${response.code()} Error message: ${response.message()}"
            return tokenResponse
        }

        oidcClient.recentGeneratedIdToken = tokenResponse.idToken
        oidcClient.recentGeneratedAccessToken = tokenResponse.accessToken
        appDatabase.oidcClientDao().update(oidcClient)
        tokenResponse.isSuccessful = true

        return tokenResponse
    }
}