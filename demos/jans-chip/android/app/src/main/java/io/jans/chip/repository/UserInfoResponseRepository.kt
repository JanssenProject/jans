package io.jans.chip.repository

import android.content.Context
import android.util.Log
import io.jans.chip.model.OPConfiguration
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.AppDatabase
import io.jans.chip.model.UserInfoResponse
import retrofit2.Response

class UserInfoResponseRepository(context: Context) {
    private val TAG = "UserInfoResponseRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    var obtainedContext: Context = context
    private var userInfoResponse: UserInfoResponse = UserInfoResponse(null)
    suspend fun getUserInfo(accessToken: String?): UserInfoResponse {
        try {
            val opConfigurationList: List<OPConfiguration> =
                appDatabase.opConfigurationDao().getAll()
            if (opConfigurationList.isEmpty()) {
                userInfoResponse.isSuccessful = false
                userInfoResponse.errorMessage = "OpenID configuration not found in database."
                return userInfoResponse
            }
            val opConfiguration: OPConfiguration = opConfigurationList[0]
            val response: Response<Any> = ApiAdapter.getInstance(opConfiguration.issuer)
                .getUserInfo(
                    accessToken,
                    "Bearer $accessToken",
                    opConfiguration.userinfoEndpoint
                )

            if (response.code() != 200) {
                userInfoResponse.isSuccessful = false
                userInfoResponse.errorMessage =
                    "Error in fetching getUserInfo. Error code: ${response.code()}, Error Message: ${response.message()} "
                return userInfoResponse
            }
            val responseFromAPI = response.body()
            Log.d("getUserInfo Response :: getUserInfo ::", responseFromAPI.toString())
            if (!response.isSuccessful || responseFromAPI == null) {
                userInfoResponse.isSuccessful = true
                userInfoResponse.errorMessage =
                    "Error in fetching getUserInfo. Error code: ${response.code()}, Error Message: ${response.message()} "
                return userInfoResponse
            }

            val userInfoResponse = UserInfoResponse()
            userInfoResponse.response = responseFromAPI
            userInfoResponse.isSuccessful = true

            return userInfoResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetching getUserInfo :: " + e.message)
            userInfoResponse.isSuccessful = false
            userInfoResponse.errorMessage = "Error in fetching getUserInfo :: " + e.message
            return userInfoResponse
            //e.printStackTrace()
        }
    }
}