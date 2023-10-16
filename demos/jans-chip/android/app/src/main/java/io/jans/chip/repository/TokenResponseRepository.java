package io.jans.chip.repository;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

import io.jans.chip.AppDatabase;
import io.jans.chip.factories.DPoPProofFactory;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.OperationError;
import io.jans.chip.modal.TokenResponse;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.SingleLiveEvent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TokenResponseRepository {
    public static final String TAG = "TokenResponseRepository";
    private final SingleLiveEvent<TokenResponse> tokenResponseLiveData = new SingleLiveEvent<>();
    Context context;
    AppDatabase appDatabase;
    private TokenResponseRepository(Context context) {
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
    }

    private static TokenResponseRepository tokenResponseRepository;

    public static TokenResponseRepository getInstance(Context context) {
        if (tokenResponseRepository == null) {
            tokenResponseRepository = new TokenResponseRepository(context);
        }
        return tokenResponseRepository;
    }

    public SingleLiveEvent<TokenResponse> getToken(String authorizationCode, String usernameText, String passwordText) {
        // Get OPConfiguration and OIDCClient
        List<OPConfiguration> opConfigurationList = appDatabase.opConfigurationDao().getAll();
        if (opConfigurationList == null || opConfigurationList.isEmpty()) {
            tokenResponseLiveData.setValue(setErrorInLiveObject("OpenID configuration not found in database."));
            return tokenResponseLiveData;
        }
        List<OIDCClient> oidcClientList = appDatabase.oidcClientDao().getAll();
        if (oidcClientList == null || oidcClientList.isEmpty()) {
            tokenResponseLiveData.setValue(setErrorInLiveObject("OpenID client not found in database."));
            return tokenResponseLiveData;
        }
        OPConfiguration opConfiguration = opConfigurationList.get(0);
        OIDCClient oidcClient = oidcClientList.get(0);

        try {
            Log.d(TAG, "dpop token" + DPoPProofFactory.getInstance().issueDPoPJWTToken("POST", opConfiguration.getIssuer()));
            // Create a call to request a token
            Call<TokenResponse> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface()
                    .getToken(oidcClient.getClientId(),
                            authorizationCode,
                            "authorization_code",
                            opConfiguration.getIssuer(),
                            "openid",
                            "Basic " + Base64.encodeToString((oidcClient.getClientId() + ":" + oidcClient.getClientSecret()).getBytes(), Base64.NO_WRAP),
                            DPoPProofFactory.getInstance().issueDPoPJWTToken("POST", opConfiguration.getIssuer()),
                            opConfiguration.getTokenEndpoint());
            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    // this method is called when we get response from our api.
                    if (response.code() == 200) {
                        TokenResponse responseFromAPI = response.body();

                        if (responseFromAPI.getAccessToken() != null && !responseFromAPI.getAccessToken().isEmpty()) {
                            Log.d(TAG, "getToken Response :: getIdToken ::" + responseFromAPI.getIdToken());
                            Log.d(TAG, "getToken Response :: getTokenType ::" + responseFromAPI.getTokenType());
                            oidcClient.setRecentGeneratedIdToken(responseFromAPI.getIdToken());
                            oidcClient.setRecentGeneratedAccessToken(responseFromAPI.getAccessToken());
                            appDatabase.oidcClientDao().update(oidcClient);
                            responseFromAPI.setSuccessful(true);
                            tokenResponseLiveData.setValue(responseFromAPI);
                        }
                    } else {
                        Log.e(TAG, "Error in Token generation.\n Error code: " + response.code() + "\n Error message: " + response.message());
                        tokenResponseLiveData.setValue(setErrorInLiveObject("Error in Token generation.\n Error code: " + response.code() + "\n Error message: " + response.message()));
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Log.e(TAG,"Inside getToken :: onFailure :: "+ t.getMessage());
                    tokenResponseLiveData.setValue(setErrorInLiveObject("Error in Token generation.\n" + t.getMessage()));
                }
            });
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            Log.e(TAG, "Error in Token generation.\n" + e.getMessage());
            tokenResponseLiveData.setValue(setErrorInLiveObject("Error in Token generation.\n" + e.getMessage()));
        }
        return tokenResponseLiveData;
    }
    private TokenResponse setErrorInLiveObject(String errorMessage) {
        OperationError operationError = new OperationError.Builder()
                .title("Error")
                .message(errorMessage)
                .build();
        TokenResponse tokenResponse = new TokenResponse(false, operationError);
        return tokenResponse;
    }
}
