package com.example.fido2.retrofit

import com.example.fido2.model.fido.assertion.option.AssertionOptionRequest
import io.ktor.client.statement.HttpResponse
import com.example.fido2.model.DCRequest
import com.example.fido2.model.SSARegRequest
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.attestation.option.AttestationOptionRequest
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest

interface APIInterface {

    suspend fun getOPConfiguration(url: String): HttpResponse
    suspend fun doDCR(dcrRequest: DCRequest, url: String): HttpResponse
    suspend fun doDCR(dcrRequest: SSARegRequest, url: String): HttpResponse
    suspend fun getAuthorizationChallenge(clientId: String, username: String, password: String, state: String, nonce: String, useDeviceSession: Boolean, acrValues: String, authMethod: String, assertionResultRequest: String, url: String): HttpResponse
    suspend fun getUserInfo(accessToken: String, clientId: String, clientSecret: String, url: String): HttpResponse
    suspend fun getToken(clientId: String, code: String, grantType: String, redirectUri: String, scope: String, clientSecret: String, dpopJwt: String, url: String): HttpResponse
    suspend fun logout(token: String, tokenTypeHint: String, clientId: String, clientSecret: String, url: String): HttpResponse
    suspend fun getFidoConfiguration(url: String): HttpResponse
    suspend fun attestationOption(request: AttestationOptionRequest, url: String): HttpResponse
    suspend fun attestationResult(request: AttestationResultRequest, url: String): HttpResponse
    suspend fun assertionOption(request: AssertionOptionRequest, url: String): HttpResponse
    suspend fun assertionResult(request: AssertionResultRequest, url: String): HttpResponse
    suspend fun verifyIntegrityTokenOnAppServer(url: String): HttpResponse
}