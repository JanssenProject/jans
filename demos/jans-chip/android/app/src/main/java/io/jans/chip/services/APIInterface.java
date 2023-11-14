package io.jans.chip.services;

import io.jans.chip.modal.appIntegrity.AppIntegrityResponse;
import io.jans.chip.modal.DCRequest;
import io.jans.chip.modal.DCResponse;
import io.jans.chip.modal.LoginResponse;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.TokenResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface APIInterface {
    @GET
    Call<OPConfiguration> getOPConfiguration(@Url String url);

    @POST
    Call<DCResponse> doDCR(@Body DCRequest dcrRequest, @Url String url);

    @FormUrlEncoded
    @POST
    Call<LoginResponse> getAuthorizationChallenge (@Field("client_id") String clientId,
                                                   @Field("username") String username,
                                                   @Field("password") String password,
                                                   @Field("state") String state,
                                                   @Field("nonce") String nonce,
                                                   @Field("use_device_session") boolean useDeviceSession,
                                                   @Url String url);
    @FormUrlEncoded
    @POST
    Call<TokenResponse> getToken (@Field("client_id") String clientId,
                                  @Field("code") String code,
                                  @Field("grant_type") String grantType,
                                  @Field("redirect_uri") String redirectUri,
                                  @Field("scope") String scope,
                                  @Header("Authorization") String authHeader,
                                  @Header("DPoP") String dpopJwt,
                                  @Url String url);
    @FormUrlEncoded
    @POST
    Call<Object> getUserInfo (@Field("access_token") String accessToken, @Header("Authorization") String authHeader, @Url String url);

    @FormUrlEncoded
    @POST
    Call<Void> logout (@Field("token") String token, @Field("token_type_hint") String tokenTypeHint, @Header("Authorization") String authHeader, @Url String url);

    // This API for verifying integrity token
    @GET
    Call<AppIntegrityResponse> verifyIntegrityTokenOnAppServer(@Url String url);
}
