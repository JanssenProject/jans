package io.jans.chip.repository

import android.content.Context
import android.util.Log
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.AppDatabase
import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.fido.attestation.option.AttestationOptionRequest
import io.jans.chip.model.fido.attestation.option.AttestationOptionResponse
import io.jans.chip.model.fido.attestation.result.AttestationResultRequest
import io.jans.chip.model.fido.attestation.result.AttestationResultResponse
import io.jans.chip.model.fido.config.FidoConfiguration
import okhttp3.ResponseBody
import retrofit2.Response

class FidoAttestationRepository(context: Context) {
    private val TAG = "FidoAttestationRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    var obtainedContext: Context = context

    suspend fun attestationOption(username: String): AttestationOptionResponse? {
        val req = AttestationOptionRequest(username, username, "none")
        var attestationOptionResponse: AttestationOptionResponse? =
            AttestationOptionResponse(null, null, null, null, null, null)
        try {
            val fidoConfigurationList: List<FidoConfiguration> =
                appDatabase.fidoConfigurationDao().getAll()
            if (fidoConfigurationList.isEmpty()) {
                attestationOptionResponse?.isSuccessful = false
                attestationOptionResponse?.errorMessage = "Fido configuration not found in database."
                return attestationOptionResponse
            }
            val fidoConfiguration: FidoConfiguration = fidoConfigurationList[0]

            val response: Response<AttestationOptionResponse> = ApiAdapter.getInstance(fidoConfiguration.issuer)
                    .attestationOption(req, fidoConfiguration.attestationOptionsEndpoint)
            if (response.code() != 200 && response.code() != 201) {
                attestationOptionResponse?.isSuccessful = false
                attestationOptionResponse?.errorMessage =
                    "Error in attestation Option Response. Error code ${response.code()}"
                return attestationOptionResponse
            }
            attestationOptionResponse = response.body();
            if (attestationOptionResponse?.challenge == null) {
                attestationOptionResponse?.isSuccessful = false
                attestationOptionResponse?.errorMessage =
                    "Challenge field in attestationOptionResponse is null."
                return attestationOptionResponse
            }
            attestationOptionResponse?.isSuccessful = true
            return attestationOptionResponse
        } catch(e: Exception) {
            Log.e(TAG,"Error in fetching AttestationOptionResponse : ${e.message}")
            attestationOptionResponse?.isSuccessful = false
            attestationOptionResponse?.errorMessage = "Error in fetching AttestationOptionResponse : ${e.message}"
            return attestationOptionResponse
        }
    }

    suspend fun attestationResult(attestationResultRequest: AttestationResultRequest?): AttestationResultResponse {
        val fidoConfigurationList = appDatabase.fidoConfigurationDao().getAll()
        val attestationResultResponse = AttestationResultResponse()
        try {
            if (fidoConfigurationList.isEmpty()) {
                attestationResultResponse.isSuccessful = false
                attestationResultResponse.errorMessage = "Fido configuration not found in database."
                return attestationResultResponse
            }
            val fidoConfiguration = fidoConfigurationList[0]

            val response: Response<ResponseBody> = ApiAdapter.getInstance(fidoConfiguration.issuer)
                .attestationResult(
                    attestationResultRequest,
                    fidoConfiguration.attestationResultEndpoint
                )
            if (response.code() != 200 && response.code() != 201) {
                attestationResultResponse.isSuccessful = false
                attestationResultResponse.errorMessage =
                    "Error in assertion option. Error code ${response.code()}"
                return attestationResultResponse
            }

            val responseFromAPI = response.body()

            Log.d(TAG, responseFromAPI.toString())
            attestationResultResponse.isSuccessful = true

            val opConfigurationList: List<OPConfiguration> = appDatabase.opConfigurationDao().getAll()
            if (opConfigurationList.isEmpty()) {
                attestationResultResponse.isSuccessful = false
                attestationResultResponse.errorMessage = "OpenID configuration not found in database."
                return attestationResultResponse
            }
            val opConfiguration: OPConfiguration = opConfigurationList[0]
            opConfiguration.biometricEnrolled = true
            appDatabase.opConfigurationDao().update(opConfiguration)

            return attestationResultResponse
        } catch(e: Exception) {
            Log.e(TAG,"Error in fetching AttestationResultRequest : ${e.message}")
            e.printStackTrace()
            attestationResultResponse.isSuccessful = false
            attestationResultResponse.errorMessage = "Error in fetching AttestationResultRequest : ${e.message}"
            return attestationResultResponse
        }

    }
}