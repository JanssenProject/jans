package com.example.fido2.authAdaptor

import com.example.fido2.model.KtPublicKeyCredentialSource
import com.example.fido2.model.fido.assertion.option.AssertionOptionResponse
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.attestation.option.AttestationOptionResponse
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest


interface AuthenticationProvider {
    fun isCredentialsPresent(username: String): Boolean
    fun getAllCredentials(): List<KtPublicKeyCredentialSource>?
    fun deleteAllKeys()
    suspend fun register(responseFromAPI: AttestationOptionResponse?, origin: String?): AttestationResultRequest?
    suspend fun authenticate(assertionOptionResponse: AssertionOptionResponse, origin: String?): AssertionResultRequest?
}