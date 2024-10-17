package com.example.fido2.usecase

import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.repository.dcr.DCRRepository

class DCRClientUseCase(private val repository: DCRRepository, private val dpoPProofFactory: DPoPProofFactoryProvider) {

    suspend operator fun invoke() = runCatching { repository.getOIDCClient(dpoPProofFactory) }
}