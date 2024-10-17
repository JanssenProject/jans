package com.example.fido2.repository.fidoAssertion

import com.example.fido2.database.local.LocalDataSource
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.example.fido2.model.BackendError
import com.example.fido2.model.fido.assertion.option.AssertionOptionRequest
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.assertion.result.AssertionResultResponse
import com.example.fido2.model.fido.config.FidoConfiguration
import com.example.fido2.model.fido.assertion.option.AssertionOptionResponse
import com.example.fido2.retrofit.ApiClient

class FidoAssertionRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): FidoAssertionRepository {
    private var assertionOptionResponse: AssertionOptionResponse? =
        AssertionOptionResponse(null, null, null, null, null, null, null)

    override suspend fun assertionOption(username: String): AssertionOptionResponse? {
        try {
            val fidoConfiguration: FidoConfiguration? = localDataSource.getFidoConfiguration()
            if (fidoConfiguration == null) {
                assertionOptionResponse?.isSuccessful = false
                assertionOptionResponse?.errorMessage = "Fido configuration not found in database."
                return assertionOptionResponse
            }
            val req = AssertionOptionRequest(username, null, null, null, null, null)
            val response = apiService.assertionOption(req, fidoConfiguration.assertionOptionsEndpoint ?: "")
            if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
                val backendError: BackendError = response.body()
                assertionOptionResponse?.isSuccessful = false
                assertionOptionResponse?.errorMessage =
                    "Error in assertion option. Reason: ${backendError.error_description}"
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
            println("assertionOptionResponse: ${assertionOptionResponse.toString()}")
            return assertionOptionResponse
        } catch (e: Exception) {
            println("Error in fetching assertion Option Response : ${e.message}")
            assertionOptionResponse?.isSuccessful = false
            assertionOptionResponse?.errorMessage =
                "Error in fetching assertion Option Response : ${e.message}"
            return assertionOptionResponse
        }
    }

    override suspend fun assertionResult(assertionResultRequest: AssertionResultRequest): AssertionResultResponse {
        val assertionResultResponse = AssertionResultResponse()
        try {
            val fidoConfiguration: FidoConfiguration? = localDataSource.getFidoConfiguration()
            if (fidoConfiguration == null) {
                assertionResultResponse.isSuccessful = false
                assertionResultResponse.errorMessage = "Fido configuration not found in database."
                return assertionResultResponse
            }

            val oidcClient = localDataSource.getOIDCClient()
            if (oidcClient == null) {
                assertionResultResponse.isSuccessful = false
                assertionResultResponse.errorMessage = "OpenID client not found in database."
                return assertionResultResponse
            }

            val response = apiService.assertionResult(assertionResultRequest, fidoConfiguration.assertionResultEndpoint ?: "")
            if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
                val backendError: BackendError = response.body()
                assertionResultResponse.isSuccessful = false
                assertionResultResponse.errorMessage =
                    "Error in assertion result. Error: ${backendError.error_description}"
                return assertionResultResponse
            }
            assertionResultResponse.isSuccessful = true
            return assertionResultResponse
        } catch (e: Exception) {
            println("Error in fetching assertion result Response : ${e.message}")
            assertionResultResponse.isSuccessful = false
            assertionResultResponse.errorMessage =
                "Error in fetching assertion result Response : ${e.message}"
            return assertionResultResponse
        }
    }
}