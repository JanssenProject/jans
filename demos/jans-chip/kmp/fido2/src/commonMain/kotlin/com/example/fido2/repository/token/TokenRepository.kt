package com.example.fido2.repository.token

import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.model.TokenResponse

interface TokenRepository {
    suspend fun getToken(authorizationCode: String?, dpoPProofFactory: DPoPProofFactoryProvider?): TokenResponse
}