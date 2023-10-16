package io.jans.chip.repository;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.util.List;

import io.jans.chip.AppDatabase;
import io.jans.chip.modal.LogoutResponse;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.OperationError;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.SingleLiveEvent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogoutRepository {
    public static final String TAG = "LogoutRepository";
    private final SingleLiveEvent<LogoutResponse> logoutResponseLiveData = new SingleLiveEvent<>();
    Context context;
    AppDatabase appDatabase;
    private LogoutRepository(Context context) {
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
    }

    private static LogoutRepository logoutRepository;

    public static LogoutRepository getInstance(Context context) {
        if (logoutRepository == null) {
            logoutRepository = new LogoutRepository(context);
        }
        return logoutRepository;
    }
    public SingleLiveEvent<LogoutResponse> logout() {
        List<OPConfiguration> opConfigurationList = appDatabase.opConfigurationDao().getAll();
        if (opConfigurationList == null || opConfigurationList.isEmpty()) {
            logoutResponseLiveData.setValue(setErrorInLiveObject("OpenID configuration not found in database."));
            return logoutResponseLiveData;
        }
        List<OIDCClient> oidcClientList = appDatabase.oidcClientDao().getAll();
        if (oidcClientList == null || oidcClientList.isEmpty()) {
            logoutResponseLiveData.setValue(setErrorInLiveObject("OpenID client not found in database."));
            return logoutResponseLiveData;
        }
        OPConfiguration opConfiguration = opConfigurationList.get(0);
        OIDCClient oidcClient = oidcClientList.get(0);
        Log.d(TAG,"Logout access token :: "+ oidcClient.getRecentGeneratedAccessToken());
        // Create a call to perform the logout
        Call<Void> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface().logout(oidcClient.getRecentGeneratedAccessToken(),
                "access_token",
                "Basic " + Base64.encodeToString((oidcClient.getClientId() + ":" + oidcClient.getClientSecret()).getBytes(), Base64.NO_WRAP),
                opConfiguration.getRevocationEndpoint());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("Logout Response code :: ", String.valueOf(response.code()));
                if (response.code() == 200) {
                    oidcClient.setRecentGeneratedAccessToken(null);
                    oidcClient.setRecentGeneratedAccessToken(null);
                    appDatabase.oidcClientDao().update(oidcClient);
                    LogoutResponse logoutResponse = new LogoutResponse();
                    logoutResponse.setSuccessful(true);
                    logoutResponseLiveData.setValue(logoutResponse);
                } else {
                    Log.e(TAG, "Error in logout.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    logoutResponseLiveData.setValue(setErrorInLiveObject("Error in logout.\n Error code: " + response.code() + "\n Error message: " + response.message()));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG,"Error in logout. " + t.getMessage());
                logoutResponseLiveData.setValue(setErrorInLiveObject("Error in logout. " + t.getMessage()));
            }
        });
        return logoutResponseLiveData;
    }
    private LogoutResponse setErrorInLiveObject(String errorMessage) {
        OperationError operationError = new OperationError.Builder()
                .title("Error")
                .message(errorMessage)
                .build();
        LogoutResponse logoutResponse = new LogoutResponse(false, operationError);
        return logoutResponse;
    }
}
