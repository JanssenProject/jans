package io.jans.chip.repository;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import io.jans.chip.AppDatabase;
import io.jans.chip.modal.LoginResponse;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.OperationError;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.modal.SingleLiveEvent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginResponseRepository {
    public static final String TAG = "LoginRepository";
    private final SingleLiveEvent<LoginResponse> loginResponseLiveData = new SingleLiveEvent<>();
    Context context;
    AppDatabase appDatabase;

    private LoginResponseRepository(Context context) {
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
    }

    private static LoginResponseRepository dcrRepository;

    public static LoginResponseRepository getInstance(Context context) {
        if (dcrRepository == null) {
            dcrRepository = new LoginResponseRepository(context);
        }
        return dcrRepository;
    }

    public SingleLiveEvent<LoginResponse> processlogin(String usernameText, String passwordText) {
        // Get OPConfiguration and OIDCClient
        List<OPConfiguration> opConfigurationList = appDatabase.opConfigurationDao().getAll();
        if (opConfigurationList == null || opConfigurationList.isEmpty()) {
            loginResponseLiveData.setValue(setErrorInLiveObject("OpenID configuration not found in database."));
            return loginResponseLiveData;
        }
        List<OIDCClient> oidcClientList = appDatabase.oidcClientDao().getAll();
        if (oidcClientList == null || oidcClientList.isEmpty()) {
            loginResponseLiveData.setValue(setErrorInLiveObject("OpenID client not found in database."));
            return loginResponseLiveData;
        }
        OPConfiguration opConfiguration = opConfigurationList.get(0);
        OIDCClient oidcClient = oidcClientList.get(0);
        Log.d(TAG,"Authorization Challenge Endpoint :: "+ opConfiguration.getAuthorizationChallengeEndpoint());
        // Create a call to request an authorization challenge
        Call<LoginResponse> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface().
                getAuthorizationChallenge(oidcClient.getClientId(),
                        usernameText,
                        passwordText,
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        true,
                        opConfiguration.getAuthorizationChallengeEndpoint());
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.code() == 200) {
                    LoginResponse responseFromAPI = response.body();
                    Log.d(TAG, "processlogin Response :: getAuthorizationCode ::" + responseFromAPI.getAuthorizationCode());

                    if (responseFromAPI.getAuthorizationCode() != null && !responseFromAPI.getAuthorizationCode().isEmpty()) {
                        responseFromAPI.setSuccessful(true);
                        loginResponseLiveData.setValue(responseFromAPI);
                    } else {
                        loginResponseLiveData.setValue(setErrorInLiveObject("Error in fetching OP Configuration. Authorization code is empty"));
                    }
                } else {
                    loginResponseLiveData.setValue(setErrorInLiveObject("Error in  generating authorization code.\n Error code: " + response.code() + "\n Error message: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e(TAG, "Inside processlogin :: onFailure :: " + t.getMessage());
                loginResponseLiveData.setValue(setErrorInLiveObject("Inside processlogin :: onFailure :: " + t.getMessage()));
            }
        });
        return loginResponseLiveData;
    }

    private LoginResponse setErrorInLiveObject(String errorMessage) {
        OperationError operationError = new OperationError.Builder()
                .title("Error")
                .message(errorMessage)
                .build();
        LoginResponse loginResponse = new LoginResponse(false, operationError);
        return loginResponse;
    }
}
