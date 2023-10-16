package io.jans.chip.repository;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.jans.chip.AppDatabase;
import io.jans.chip.factories.DPoPProofFactory;
import io.jans.chip.factories.KeyManager;
import io.jans.chip.modal.DCRequest;
import io.jans.chip.modal.DCResponse;
import io.jans.chip.modal.JSONWebKeySet;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.OperationError;
import io.jans.chip.modal.appIntegrity.AppIntegrityEntity;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.SingleLiveEvent;
import io.jans.chip.utils.AppConfig;
import io.jans.chip.utils.AppUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DCRRepository {
    public static final String TAG = "DCRRepository";
    private final SingleLiveEvent<OIDCClient> oidcClientLiveData = new SingleLiveEvent<>();
    Context context;

    private static DCRRepository dcrRepository;

    AppDatabase appDatabase;

    public static DCRRepository getInstance(Context context) {
        if (dcrRepository == null) {
            dcrRepository = new DCRRepository(context);
        }
        return dcrRepository;
    }

    private DCRRepository(Context context) {
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
    }

    public SingleLiveEvent<OIDCClient> doDCR(String scopeText) {

        List<OPConfiguration> opConfigurationList = appDatabase.opConfigurationDao().getAll();
        if (opConfigurationList == null || opConfigurationList.isEmpty()) {
            oidcClientLiveData.setValue(setErrorInLiveObject("OpenID configuration not found in database."));
            return oidcClientLiveData;
        }
        OPConfiguration opConfiguration = opConfigurationList.get(0);
        String issuer = opConfiguration.getIssuer();
        String registrationUrl = opConfiguration.getRegistrationEndpoint();

        DCRequest dcrRequest = new DCRequest();
        dcrRequest.setIssuer(issuer);
        dcrRequest.setClientName(AppConfig.APP_NAME + UUID.randomUUID());
        dcrRequest.setApplicationType("web");
        dcrRequest.setGrantTypes(Lists.newArrayList("authorization_code", "client_credentials"));
        dcrRequest.setScope(scopeText);
        dcrRequest.setRedirectUris(Lists.newArrayList(issuer));
        dcrRequest.setResponseTypes(Lists.newArrayList("code"));
        dcrRequest.setTokenEndpointAuthMethod("client_secret_basic");
        dcrRequest.setPostLogoutRedirectUris(Lists.newArrayList(issuer));

        Map<String, Object> claims = new HashMap<>();
        claims.put("appName", AppConfig.APP_NAME);
        claims.put("seq", UUID.randomUUID());
        claims.put("app_id", context.getPackageName());
        String checksum = null;
        try {
            checksum = AppUtil.getChecksum(context);
            claims.put("app_checksum", checksum);
        } catch (IOException | NoSuchAlgorithmException | PackageManager.NameNotFoundException e) {
            oidcClientLiveData.setValue(setErrorInLiveObject("Error in generating app checksum.\n" + e.getMessage()));
            return oidcClientLiveData;
        }
        List<AppIntegrityEntity> appIntegrityList = appDatabase.appIntegrityDao().getAll();
        if (appIntegrityList == null || appIntegrityList.isEmpty()) {
            oidcClientLiveData.setValue(setErrorInLiveObject("App Integrity not found in database."));
            return oidcClientLiveData;
        }
        claims.put("app_integrity_result", appIntegrityList.get(0));
        try {
            String evidenceJwt = DPoPProofFactory.getInstance().issueJWTToken(claims);
            dcrRequest.setEvidence(evidenceJwt);
            Log.d(TAG, "Inside doDCR :: evidence :: " + evidenceJwt);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                 NoSuchProviderException e) {
            oidcClientLiveData.setValue(setErrorInLiveObject("Error in  generating DPoP jwt.\n" + e.getMessage()));
            return oidcClientLiveData;
        }

        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.addKey(KeyManager.getPublicKeyJWK(KeyManager.getInstance().getPublicKey()).getRequiredParams());

        dcrRequest.setJwks(jwks.toJsonString());
        Log.d(TAG, "Inside doDCR :: jwks :: " + jwks.toJsonString());

        Call<DCResponse> call = RetrofitClient.getInstance(issuer).getAPIInterface().doDCR(dcrRequest, registrationUrl);
        call.enqueue(new Callback<DCResponse>() {
            @Override
            public void onResponse(Call<DCResponse> call, Response<DCResponse> response) {

                DCResponse responseFromAPI = response.body();
                if (response.code() == 200 || response.code() == 201) {
                    if (responseFromAPI.getClientId() != null && !responseFromAPI.getClientId().isEmpty()) {
                        OIDCClient client = new OIDCClient();
                        client.setSno(AppConfig.DEFAULT_S_NO);
                        client.setClientName(responseFromAPI.getClientName());
                        client.setClientId(responseFromAPI.getClientId());
                        client.setClientSecret(responseFromAPI.getClientSecret());
                        client.setScope(scopeText);
                        appDatabase.oidcClientDao().insert(client);
                        client.setSuccessful(true);
                        oidcClientLiveData.setValue(client);
                    }
                } else {
                    Log.e(TAG, "Error in  DCR.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    oidcClientLiveData.setValue(setErrorInLiveObject("Error in  DCR.\n Error code: " + response.code() + "\n Error message: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<DCResponse> call, Throwable t) {
                Log.e(TAG, "Inside doDCR :: onFailure :: " + t.getMessage());
                oidcClientLiveData.setValue(setErrorInLiveObject("Error in  DCR.\n" + t.getMessage()));

            }
        });
        return oidcClientLiveData;
    }

    private OIDCClient setErrorInLiveObject(String errorMessage) {
        OperationError operationError = new OperationError.Builder()
                .title("Error")
                .message(errorMessage)
                .build();
        OIDCClient oidcClient = new OIDCClient(false, operationError);
        return oidcClient;
    }
}