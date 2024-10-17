package com.example.fido2.usecase

import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.repository.fidoAssertion.FidoAssertionRepository

class FidoAssertionUseCase(private val repository: FidoAssertionRepository) {

    suspend operator fun invoke(username: String) = runCatching { repository.assertionOption(username) }

    suspend operator fun invoke(assertionResultRequest: AssertionResultRequest) = runCatching { repository.assertionResult(assertionResultRequest) }
}