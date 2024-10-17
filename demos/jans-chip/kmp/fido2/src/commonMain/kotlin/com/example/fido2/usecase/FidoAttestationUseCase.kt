package com.example.fido2.usecase

import com.example.fido2.model.fido.attestation.result.AttestationResultRequest
import com.example.fido2.repository.fidoAttestation.FidoAttestationRepository

class FidoAttestationUseCase(private val repository: FidoAttestationRepository) {

    suspend operator fun invoke(username: String) = runCatching { repository.attestationOption(username) }

    suspend operator fun invoke(attestationResultRequest: AttestationResultRequest) = runCatching { repository.attestationResult(attestationResultRequest) }
}