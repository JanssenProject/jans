package com.example.fido2.repository.userinfo

import com.example.fido2.database.local.LocalDataSource
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode.Companion.OK
import com.example.fido2.model.BackendError
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.UserInfoResponse
import com.example.fido2.retrofit.ApiClient

class UserInfoRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): UserInfoRepository {
    private var userInfoResponse: UserInfoResponse = UserInfoResponse(null)

    override suspend fun getUserInfo(accessToken: String?, clientId: String, clientSecret: String): UserInfoResponse {
        try {
            val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
            if (opConfiguration == null) {
                userInfoResponse.isSuccessful = false
                userInfoResponse.errorMessage = "OPConfiguration configuration not found in database."
                return userInfoResponse
            }

            if (accessToken != null) {
                opConfiguration.userinfoEndpoint?.let {
                    val response = apiService.getUserInfo(accessToken, clientId, clientSecret, it)
                    if (response.status != OK) {
                        val backendError: BackendError = response.body()
                        userInfoResponse.isSuccessful = false
                        userInfoResponse.errorMessage =
                            "Error in fetching getUserInfo. Error code: ${response.status}, Error Message: ${backendError.error_description}"
                        return userInfoResponse
                    }

                    val responseFromAPI: String = response.body()
                    println("getUserInfo Response :: getUserInfo :: $responseFromAPI")
                    if (responseFromAPI.isEmpty()) {
                        userInfoResponse.isSuccessful = true
                        userInfoResponse.errorMessage =
                            "Error in fetching getUserInfo. Error code: ${response.status}, Error Message: $response"
                        return userInfoResponse
                    }

                    val userInfoResponse = UserInfoResponse()
                    userInfoResponse.response = responseFromAPI
                    userInfoResponse.isSuccessful = true

                    return userInfoResponse
                }

                userInfoResponse.isSuccessful = false
                userInfoResponse.errorMessage = "Error in fetching getUserInfo :: "
                return userInfoResponse
            } else {
                userInfoResponse.isSuccessful = false
                userInfoResponse.errorMessage = "Error in fetching getUserInfo :: "
                return userInfoResponse
            }
        } catch (e: Exception) {
            println("Error in fetching getUserInfo :: " + e.message)
            userInfoResponse.isSuccessful = false
            userInfoResponse.errorMessage = "Error in fetching getUserInfo :: " + e.message
            return userInfoResponse
        }
    }
}