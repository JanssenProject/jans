package io.jans.chip.retrofit

import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.DCRequest
import io.jans.chip.model.DCResponse
import io.jans.chip.model.LoginResponse
import io.jans.chip.model.SSARegRequest
import io.jans.chip.model.TokenResponse
import io.jans.chip.model.appIntegrity.AppIntegrityResponse
import io.jans.chip.model.fido.assertion.option.AssertionOptionRequest
import io.jans.chip.model.fido.assertion.option.AssertionOptionResponse
import io.jans.chip.model.fido.assertion.result.AssertionResultRequest
import io.jans.chip.model.fido.attestation.option.AttestationOptionRequest
import io.jans.chip.model.fido.attestation.option.AttestationOptionResponse
import io.jans.chip.model.fido.attestation.result.AttestationResultRequest
import io.jans.chip.model.fido.config.FidoConfigurationResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface APIInterface {
    @GET
    suspend  fun getOPConfiguration(@Url url: String): Response<OPConfiguration>

    @POST
    suspend fun doDCR(@Body dcrRequest: DCRequest, @Url url: String?): Response<DCResponse>

    @POST
    suspend fun doDCR(@Body dcrRequest: SSARegRequest, @Url url: String?): Response<DCResponse>

    @FormUrlEncoded
    @POST
    suspend fun getAuthorizationChallenge(
        @Field("client_id") clientId: String?,
        @Field("username") username: String?,
        @Field("password") password: String?,
        @Field("state") state: String?,
        @Field("nonce") nonce: String?,
        @Field("use_device_session") useDeviceSession: Boolean,
        @Field("acr_values") acrValues: String?,
        @Field("auth_method") authMethod: String?,
        @Field("assertion_result_request") assertionResultRequest: String?,
        @Url url: String?
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST
    suspend fun getUserInfo(
        @Field("access_token") accessToken: String?,
        @Header("Authorization") authHeader: String?,
        @Url url: String?
    ): Response<Any>

    @FormUrlEncoded
    @POST
    suspend fun getToken(
        @Field("client_id") clientId: String?,
        @Field("code") code: String?,
        @Field("grant_type") grantType: String?,
        @Field("redirect_uri") redirectUri: String?,
        @Field("scope") scope: String?,
        @Header("Authorization") authHeader: String?,
        @Header("DPoP") dpopJwt: String?,
        @Url url: String?
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST
    suspend fun logout(
        @Field("token") token: String?,
        @Field("token_type_hint") tokenTypeHint: String?,
        @Header("Authorization") authHeader: String?,
        @Url url: String?
    ): Response<Void>

    @GET
    suspend fun getFidoConfiguration(@Url url: String?): Response<FidoConfigurationResponse>

    @POST
    suspend fun attestationOption(@Body request: AttestationOptionRequest?, @Url url: String?): Response<AttestationOptionResponse>

    @POST
    suspend fun attestationResult(@Body request: AttestationResultRequest?, @Url url: String?): Response<ResponseBody>

    @POST
    suspend fun assertionOption(@Body request: AssertionOptionRequest?, @Url url: String?): Response<AssertionOptionResponse>

    @POST
    suspend fun assertionResult(@Body request: AssertionResultRequest?, @Url url: String?): Response<ResponseBody>

    @GET
    suspend fun verifyIntegrityTokenOnAppServer(@Url url: String?): Response<AppIntegrityResponse>?

}