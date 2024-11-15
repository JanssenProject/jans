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
            if (oidcClient == null) {
                loginResponse.isSuccessful = false
                loginResponse.errorMessage =
                    "Error in login. oidcClient is null"
                return loginResponse
            }
            println("oidcClient --> $oidcClient")
            val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
            println("opConfiguration --> $opConfiguration")
            println("authMethod --> $authMethod")
            // Create a call to request an authorization challenge
            val response = apiService.getAuthorizationChallenge(
                oidcClient?.clientId ?: "",
                usernameText,
                passwordText ?: "Gluu1234.",
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
                    "Error in login. Reason ${backendError.error}"
                println(
                    "Error in login. ErrorCode : ${backendError.error} and ErrorMessage: ${backendError.error_description}"
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