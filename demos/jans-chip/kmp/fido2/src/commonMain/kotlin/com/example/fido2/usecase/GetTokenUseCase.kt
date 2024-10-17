package com.example.fido2.usecase

import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.repository.token.TokenRepository

class GetTokenUseCase(private val repository: TokenRepository, private val dpoPProofFactory: DPoPProofFactoryProvider) {

    suspend operator fun invoke(authorizationCode: String?) = runCatching { repository.getToken(authorizationCode, dpoPProofFactory) }
}