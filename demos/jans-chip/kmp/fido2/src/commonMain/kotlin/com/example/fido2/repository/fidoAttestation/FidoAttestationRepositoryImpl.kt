package com.example.fido2.repository.fidoAttestation

import com.example.fido2.database.local.LocalDataSource
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import com.example.fido2.model.BackendError
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.fido.attestation.option.AttestationOptionRequest
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest
import com.example.fido2.model.fido.attestation.result.AttestationResultResponse
import com.example.fido2.model.fido.config.FidoConfiguration
import com.example.fido2.model.fido.attestation.option.AttestationOptionResponse
import com.example.fido2.retrofit.ApiClient

class FidoAttestationRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): FidoAttestationRepository {

    override suspend fun attestationOption(username: String): AttestationOptionResponse? {
        val req = AttestationOptionRequest(username, username, "none")
        var attestationOptionResponse: AttestationOptionResponse? =
            AttestationOptionResponse(null, null, null, null, null, null)
        try {
            val fidoConfiguration: FidoConfiguration? = localDataSource.getFidoConfiguration()
            println("fidoConfiguration ---> $fidoConfiguration")
            val response = apiService.attestationOption(req, fidoConfiguration?.attestationOptionsEndpoint ?: "")
            if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
                val backendError: BackendError = response.body()
                attestationOptionResponse?.isSuccessful = false
                attestationOptionResponse?.errorMessage =
                    "Error in attestation Option Response. Error: ${backendError.error_description}"
                return attestationOptionResponse
            }
            attestationOptionResponse = response.body()
            if (attestationOptionResponse?.challenge == null) {
                attestationOptionResponse?.isSuccessful = false
                attestationOptionResponse?.errorMessage =
                    "Challenge field in attestationOptionResponse is null."
                return attestationOptionResponse
            }
            attestationOptionResponse.isSuccessful = true
            return attestationOptionResponse
        } catch(e: Exception) {
            println("Error in fetching AttestationOptionResponse : ${e.message}")
            attestationOptionResponse?.isSuccessful = false
            attestationOptionResponse?.errorMessage = "Error in fetching AttestationOptionResponse : ${e.message}"
            return attestationOptionResponse
        }
    }

    override suspend fun attestationResult(attestationResultRequest: AttestationResultRequest): AttestationResultResponse {
        val fidoConfiguration: FidoConfiguration? = localDataSource.getFidoConfiguration()
        val attestationResultResponse = AttestationResultResponse()
        try {
            if (fidoConfiguration == null) {
                attestationResultResponse.isSuccessful = false
                attestationResultResponse.errorMessage = "Fido configuration not found in database."
                return attestationResultResponse
            }

            val response = apiService.attestationResult(attestationResultRequest, fidoConfiguration.attestationResultEndpoint ?: "")
            if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
                val backendError: BackendError = response.body()
                attestationResultResponse.isSuccessful = false
                attestationResultResponse.errorMessage =
                    "Error in attestation result. Error: ${backendError.error_description}"
                return attestationResultResponse
            }

            attestationResultResponse.isSuccessful = true

            val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
            if (opConfiguration == null) {
                attestationResultResponse.isSuccessful = false
                attestationResultResponse.errorMessage = "OpenID configuration not found in database."
                return attestationResultResponse
            }
            opConfiguration.biometricEnrolled = true
            localDataSource.updateOPConfiguration(opConfiguration)

            return attestationResultResponse
        } catch(e: Exception) {
            println("Error in fetching AttestationResultRequest : ${e.message}")
            e.printStackTrace()
            attestationResultResponse.isSuccessful = false
            attestationResultResponse.errorMessage = "Error in fetching AttestationResultRequest : ${e.message}"
            return attestationResultResponse
        }

    }
}