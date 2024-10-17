package com.example.fido2.retrofit

import com.example.fido2.model.fido.assertion.option.AssertionOptionRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.InternalAPI
import kotlinx.serialization.json.Json
import com.example.fido2.model.DCRequest
import com.example.fido2.model.SSARegRequest
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.attestation.option.AttestationOptionRequest
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest

class ApiClient: APIInterface {
    private fun httpClient(username: String? = null, password: String? = null): HttpClient {
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            if (username != null && password != null) {
                install(Auth) {
                    basic {
                        sendWithoutRequest { true }
                        credentials {
                            BasicAuthCredentials(
                                username,
                                password
                            )
                        }
                        realm = "jans-auth"
                    }
                }
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        print(message)
                    }
                }
                level = LogLevel.ALL
            }
        }
        return httpClient
    }

    override suspend fun getOPConfiguration(
        url: String
    ): HttpResponse {
        val result: HttpResponse =
            httpClient().get(url)
        return result
    }

    override suspend fun doDCR(dcrRequest: DCRequest, url: String): HttpResponse {
        val result: HttpResponse = httpClient().post {
            url(url)
            setBody("$dcrRequest")
        }
        return result
    }

    override suspend fun doDCR(dcrRequest: SSARegRequest, url: String): HttpResponse {
        val result: HttpResponse = httpClient().post {
            url(url)
            setBody("${dcrRequest.toJson()}")
        }
        return result
    }

    override suspend fun getFidoConfiguration(
        url: String
    ): HttpResponse {
        val result: HttpResponse =
            httpClient().get(url)
        return result
    }

    @OptIn(InternalAPI::class)
    override suspend fun attestationOption(
        request: AttestationOptionRequest,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient().post {
            url(url)
            body = request.toJson()
            contentType(ContentType.Application.Json)
        }
        return result
    }

    @OptIn(InternalAPI::class)
    override suspend fun attestationResult(
        request: AttestationResultRequest,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient().post {
            url(url)
            body = request.toJson()
            contentType(ContentType.Application.Json)
        }
        return result
    }

    @OptIn(InternalAPI::class)
    override suspend fun assertionOption(
        request: AssertionOptionRequest,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient().post {
            url(url)
            body = request.toJson()
            contentType(ContentType.Application.Json)
        }
        return result
    }

    override suspend fun assertionResult(
        request: AssertionResultRequest,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient().post {
            url(url)
            setBody("$request")
        }
        return result
    }

    @OptIn(InternalAPI::class)
    override suspend fun getUserInfo(
        accessToken: String,
        clientId: String,
        clientSecret: String,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient(clientId, clientSecret).post {
            url(url)
            body = FormDataContent(Parameters.build {
                append("access_token", accessToken)
            })
        }
        return result
    }

    @OptIn(InternalAPI::class)
    override suspend fun getToken(
        clientId: String,
        code: String,
        grantType: String,
        redirectUri: String,
        scope: String,
        clientSecret: String,
        dpopJwt: String,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient(clientId, clientSecret).post {
            url(url)
            body = FormDataContent(Parameters.build {
                append("client_id", clientId)
                append("code", code)
                append("grant_type", grantType)
                append("redirect_uri", redirectUri)
                append("scope", scope)
            })
            contentType(ContentType.Application.FormUrlEncoded)
        }
        return result
    }

    @OptIn(InternalAPI::class)
    override suspend fun getAuthorizationChallenge(
        clientId: String,
        username: String,
        password: String,
        state: String,
        nonce: String,
        useDeviceSession: Boolean,
        acrValues: String,
        authMethod: String,
        assertionResultRequest: String,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient().post {
            url(url)
            body = FormDataContent(Parameters.build {
                append("client_id", clientId)
                append("username", username)
                if (password.isNotEmpty()) {
                    append("password", password)
                }
                append("state", state)
                append("nonce", nonce)
                append("use_device_session", useDeviceSession.toString())
                append("acr_values", acrValues)
                append("auth_method", authMethod)
                if (assertionResultRequest.isNotEmpty()) {
                    append("assertion_result_request", assertionResultRequest)
                }
            })
            contentType(ContentType.Application.FormUrlEncoded)
        }
        return result
    }

    @OptIn(InternalAPI::class)
    override suspend fun logout(
        token: String,
        tokenTypeHint: String,
        clientId: String,
        clientSecret: String,
        url: String
    ): HttpResponse {
        val result: HttpResponse = httpClient(clientId, clientSecret).post {
            url(url)
            body = FormDataContent(Parameters.build {
                append("token", token)
                append("token_type_hint", tokenTypeHint)
            })
            contentType(ContentType.Application.FormUrlEncoded)
        }
        return result
    }

    override suspend fun verifyIntegrityTokenOnAppServer(
        url: String
    ): HttpResponse {
        val result: HttpResponse =
            httpClient().get(url)
        return result
    }
}