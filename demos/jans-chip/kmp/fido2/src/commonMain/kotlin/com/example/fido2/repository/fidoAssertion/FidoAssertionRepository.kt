package com.example.fido2.repository.fidoAssertion

import com.example.fido2.model.fido.assertion.option.AssertionOptionResponse
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.assertion.result.AssertionResultResponse

interface FidoAssertionRepository {
    suspend fun assertionOption(username: String): AssertionOptionResponse?
    suspend fun assertionResult(assertionResultRequest: AssertionResultRequest): AssertionResultResponse
}