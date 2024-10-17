package com.example.fido2.repository.login

import com.example.fido2.database.local.LocalDataSource
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.example.fido2.model.BackendError
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.LoginResponse
import com.example.fido2.retrofit.ApiClient

class LoginRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): LoginRepository {

    override suspend fun processLogin(usernameText: String, passwordText: String?, authMethod: String, assertionResultRequest: String?): LoginResponse {
        var loginResponse = LoginResponse(null)
        // Get OPConfiguration and OIDCClient
        try {
            val oidcClient = localDataSource.getOIDCClient()
            println("oidcClient --> $oidcClient")
            val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
            println("opConfiguration --> $opConfiguration")
            // Create a call to request an authorization challenge
            val response = apiService.getAuthorizationChallenge(
                oidcClient?.clientId ?: "",
                usernameText,
                passwordText ?: "",
                "",
                "",
                true,
                "passkey_auth_challenge", // "passkey"
                authMethod,
                assertionResultRequest ?: "",
                opConfiguration?.authorizationChallengeEndpoint ?: ""
            )

            println("assertionResultRequest -- " + assertionResultRequest.toString())
            if (response.status != HttpStatusCode.OK) {
                val backendError: BackendError = response.body()
                loginResponse.isSuccessful = false
                loginResponse.errorMessage =
                    "Error in login. Reason ${backendError.reason}"
                println(
                    "Error in login. ErrorCode : ${backendError.reason} and ErrorMessage: ${backendError.error_description}"
                )
                return loginResponse
            }
            loginResponse = response.body()

            println(
                "processlogin Response :: getAuthorizationCode :: ${loginResponse.authorizationCode}"
            )
            loginResponse.isSuccessful = true
            return loginResponse
        } catch (e: Exception) {
            println("Error in  login. ${e.message}".trimIndent())
            loginResponse.isSuccessful = false
            loginResponse.errorMessage =
                "Error in login. Error message: ${e.message}"
            return loginResponse
        }
    }
}