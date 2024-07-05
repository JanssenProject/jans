package io.jans.chip.repository

import android.content.Context
import android.util.Log
import io.jans.chip.AppDatabase
import io.jans.chip.model.fido.assertion.option.AssertionOptionRequest
import io.jans.chip.model.fido.assertion.option.AssertionOptionResponse
import io.jans.chip.model.fido.assertion.result.AssertionResultRequest
import io.jans.chip.model.fido.assertion.result.AssertionResultResponse
import io.jans.chip.model.fido.config.FidoConfiguration
import io.jans.chip.retrofit.ApiAdapter
import okhttp3.ResponseBody
import retrofit2.Response

class FidoAssertionRepository(context: Context) {
    private val TAG = "FidoAssertionRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    var obtainedContext: Context = context
    private var assertionOptionResponse: AssertionOptionResponse? =
        AssertionOptionResponse(null, null, null, null, null, null, null)

    suspend fun assertionOption(username: String): AssertionOptionResponse? {
        try {
            val fidoConfigurationList: List<FidoConfiguration> =
                appDatabase.fidoConfigurationDao().getAll()
            if (fidoConfigurationList == null || fidoConfigurationList.isEmpty()) {
                assertionOptionResponse?.isSuccessful = false
                assertionOptionResponse?.errorMessage = "Fido configuration not found in database."
                return assertionOptionResponse
            }
            val fidoConfiguration: FidoConfiguration = fidoConfigurationList[0]
            val req = AssertionOptionRequest(username, null, null, null, null, null)

            val response: Response<AssertionOptionResponse> =
                ApiAdapter.getInstance(fidoConfiguration.issuer)
                    .assertionOption(req, fidoConfiguration.assertionOptionsEndpoint)
            if (response.code() != 200 && response.code() != 201) {
                assertionOptionResponse?.isSuccessful = false
                assertionOptionResponse?.errorMessage =
                    "Error in assertion option. Error code ${response.code()}"
                return assertionOptionResponse
            }
            assertionOptionResponse = response.body();
            if (assertionOptionResponse?.challenge == null) {
                assertionOptionResponse?.isSuccessful = false
                assertionOptionResponse?.errorMessage =
                    "Challenge field in assertion Option Response is null."
                return assertionOptionResponse
            }
            assertionOptionResponse?.isSuccessful = true
            return assertionOptionResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetching assertion Option Response : ${e.message}")
            assertionOptionResponse?.isSuccessful = false
            assertionOptionResponse?.errorMessage =
                "Error in fetching assertion Option Response : ${e.message}"
            return assertionOptionResponse
        }
    }

    suspend fun assertionResult(assertionResultRequest: AssertionResultRequest?): AssertionResultResponse {
        val assertionResultResponse = AssertionResultResponse()
        try {
            val fidoConfigurationList: List<FidoConfiguration> =
                appDatabase.fidoConfigurationDao().getAll()
            if (fidoConfigurationList.isEmpty()) {
                assertionResultResponse.isSuccessful = false
                assertionResultResponse.errorMessage = "Fido configuration not found in database."
                return assertionResultResponse
            }
            val fidoConfiguration: FidoConfiguration = fidoConfigurationList[0]

            val oidcClientList = appDatabase.oidcClientDao().getAll()
            if (oidcClientList.isEmpty()) {
                assertionResultResponse.isSuccessful = false
                assertionResultResponse.errorMessage = "OpenID client not found in database."
                return assertionResultResponse
            }
            //oidcClientList[0]

            val response: Response<ResponseBody> =
                ApiAdapter.getInstance(fidoConfiguration.issuer)
                    .assertionResult(
                        assertionResultRequest,
                        fidoConfiguration.assertionResultEndpoint
                    )
            if (response.code() != 200 && response.code() != 201) {
                assertionResultResponse.isSuccessful = false
                assertionResultResponse.errorMessage =
                    "Error in assertion result. Error code ${response.code()}"
                return assertionResultResponse
            }
            //val responseFromAPI = response.body();
            assertionResultResponse.isSuccessful = true
            return assertionResultResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetching assertion result Response : ${e.message}")
            assertionResultResponse.isSuccessful = false
            assertionResultResponse.errorMessage =
                "Error in fetching assertion result Response : ${e.message}"
            return assertionResultResponse
        }
    }
}