package services

import android.content.Context
import com.example.fido2.authAdaptor.AuthenticationProvider
import com.example.fido2.model.KtPublicKeyCredentialSource
import com.example.fido2.model.fido.assertion.option.AssertionOptionResponse
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.attestation.option.AttestationOptionResponse
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest
import shared.common.AuthenticationAdaptor

class AuthAdaptor(context: Context): AuthenticationProvider {

    private var authAdaptor: AuthenticationAdaptor? = null

    init {
        authAdaptor = AuthenticationAdaptor(context)
    }

    override fun isCredentialsPresent(username: String): Boolean {
        return authAdaptor?.isCredentialsPresent(username) == true
    }

    override fun getAllCredentials(): List<KtPublicKeyCredentialSource>? {
        return authAdaptor?.getAllCredentials()
    }

    override fun deleteAllKeys() {
        authAdaptor?.deleteAllKeys()
    }

    override suspend fun register(
        responseFromAPI: AttestationOptionResponse?,
        origin: String?
    ): AttestationResultRequest? {
        return authAdaptor?.register(responseFromAPI, origin)
    }

    override suspend fun authenticate(
        assertionOptionResponse: AssertionOptionResponse,
        origin: String?
    ): AssertionResultRequest? {
        return authAdaptor?.authenticate(assertionOptionResponse, origin)
    }
}