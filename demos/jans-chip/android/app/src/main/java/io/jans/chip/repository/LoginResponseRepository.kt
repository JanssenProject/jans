package io.jans.chip.repository

import android.content.Context
import android.util.Log
import io.jans.chip.AppDatabase
import io.jans.chip.model.LoginResponse
import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.UserInfoResponse
import io.jans.chip.retrofit.ApiAdapter
import retrofit2.Response
import java.util.UUID

class LoginResponseRepository(context: Context) {
    private val TAG = "LoginResponseRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    var obtainedContext: Context = context
    private var loginResponse: LoginResponse? = LoginResponse(null)
    suspend fun processLogin(
        usernameText: String,
        passwordText: String?,
        authMethod: String,
        assertionResultRequest: String?
    ): LoginResponse? {
        // Get OPConfiguration and OIDCClient
        try {
            val opConfigurationList: List<OPConfiguration>? =
                appDatabase.opConfigurationDao().getAll()
            if (opConfigurationList == null || opConfigurationList.isEmpty()) {
                loginResponse?.isSuccessful = false
                loginResponse?.errorMessage = "OpenID configuration not found in database."
                return loginResponse
            }
            val oidcClientList = appDatabase.oidcClientDao().getAll()
            if (oidcClientList == null || oidcClientList.isEmpty()) {
                loginResponse?.isSuccessful = false
                loginResponse?.errorMessage = "OpenID client not found in database."
                return loginResponse
            }
            val opConfiguration: OPConfiguration = opConfigurationList[0]
            val oidcClient = oidcClientList[0]
            Log.d(
                TAG,
                "Authorization Challenge Endpoint :: " + opConfiguration.authorizationChallengeEndpoint
            )
            // Create a call to request an authorization challenge
            val response: Response<LoginResponse> =
                ApiAdapter.getInstance(opConfiguration.issuer).getAuthorizationChallenge(
                    oidcClient.clientId,
                    usernameText,
                    passwordText,
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    true,
                    "passkey",
                    authMethod,
                    assertionResultRequest,
                    opConfiguration.authorizationChallengeEndpoint
                )

            if (response.code() != 200) {
                loginResponse?.isSuccessful = false
                loginResponse?.errorMessage =
                    "Error in login. Check Username/ Password."
                Log.d(
                    TAG,
                    "Error in login. Check Username/ Password. ErrorCode : ${response.code()} and ErrorMessage: ${response.message()}"
                )
                return loginResponse
            }
            loginResponse = response.body()
            if (!response.isSuccessful || loginResponse == null) {
                loginResponse?.isSuccessful = false
                loginResponse?.errorMessage =
                    "Error in login. ErrorCode : ${response.code()} and ErrorMessage: ${response.message()}"
                return loginResponse
            }

            Log.d(
                TAG,
                "processlogin Response :: getAuthorizationCode :: ${loginResponse?.authorizationCode}"
            )
            loginResponse?.isSuccessful = true
            return loginResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error in  login. ${e.message}".trimIndent())
            loginResponse?.isSuccessful = false
            loginResponse?.errorMessage =
                "Error in login. Error message: ${e.message}"
            return loginResponse
        }
    }

    suspend fun isAuthenticated(accessToken: String?): Boolean {
        var userInfoResponseRepository: UserInfoResponseRepository =
            UserInfoResponseRepository(obtainedContext)
        var userInfoResponse: UserInfoResponse? =
            userInfoResponseRepository.getUserInfo(accessToken)
        return userInfoResponse?.let { it -> it?.isSuccessful } ?: false
    }

    suspend fun getUserInfoWithAccessToken(accessToken: String?): UserInfoResponse? {
        var userInfoResponseRepository: UserInfoResponseRepository =
            UserInfoResponseRepository(obtainedContext)
        return userInfoResponseRepository.getUserInfo(accessToken)

    }
}