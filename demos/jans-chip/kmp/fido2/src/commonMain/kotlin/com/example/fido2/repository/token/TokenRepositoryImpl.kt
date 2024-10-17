package com.example.fido2.repository.token

import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.database.local.LocalDataSource
import com.example.fido2.model.OPConfiguration
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import com.example.fido2.model.BackendError
import com.example.fido2.model.OIDCClient
import com.example.fido2.model.TokenResponse
import com.example.fido2.retrofit.ApiClient
import kotlin.io.encoding.ExperimentalEncodingApi

class TokenRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): TokenRepository {

    private var tokenResponse: TokenResponse = TokenResponse(null, null, null)

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getToken(authorizationCode: String?, dpoPProofFactory: DPoPProofFactoryProvider?): TokenResponse {
        // Get OPConfiguration and OIDCClient
        val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
        val oidcClient: OIDCClient? = localDataSource.getOIDCClient()

        // Create a call to request a token
        val response: HttpResponse =
            apiService.getToken(
                oidcClient?.clientId ?: "",
                authorizationCode ?: "",
                "authorization_code",
                opConfiguration?.issuer ?: "",
                oidcClient?.scope ?: "",
                oidcClient?.clientSecret ?: "",
                opConfiguration?.issuer?.let { dpoPProofFactory?.issueDPoPJWTToken("POST", it) }.toString(),
                opConfiguration?.tokenEndpoint ?: ""
            )

        if (response.status != HttpStatusCode.OK) {
            val backendError: BackendError = response.body()
            tokenResponse.isSuccessful = false
            tokenResponse.errorMessage = "Error in Token generation. Error code: ${response.status} Error message: ${backendError.error_description}"
            return tokenResponse
        }

        val tokenResponse: TokenResponse? = response.body()
        println("getToken Response :: getIdToken ::" + tokenResponse?.idToken)
        println("getToken Response :: getTokenType ::" + tokenResponse?.tokenType)

        if (tokenResponse == null) {
            tokenResponse?.isSuccessful = false
            tokenResponse?.errorMessage =
                "Error in Token generation. Error code: ${response.status} Error message: $response"
            return tokenResponse!!
        }

        oidcClient?.recentGeneratedIdToken = tokenResponse.idToken
        oidcClient?.recentGeneratedAccessToken = tokenResponse.accessToken
        if (oidcClient != null) {
            localDataSource.updateOIDCClient(oidcClient)
        }
        tokenResponse.isSuccessful = true

        return tokenResponse
    }
}