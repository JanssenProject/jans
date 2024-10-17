package com.example.fido2.repository.fidoAttestation

import com.example.fido2.model.fido.attestation.option.AttestationOptionResponse
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest
import com.example.fido2.model.fido.attestation.result.AttestationResultResponse

interface FidoAttestationRepository {
    suspend fun attestationOption(username: String): AttestationOptionResponse?
    suspend fun attestationResult(attestationResultRequest: AttestationResultRequest): AttestationResultResponse
}