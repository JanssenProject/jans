package io.jans.chip.repository;

import android.content.Context;
import android.util.Log;

import java.util.List;

import io.jans.chip.AppDatabase;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.OperationError;
import io.jans.chip.modal.UserInfoResponse;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.SingleLiveEvent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserInfoResponseRepository {
    public static final String TAG = "UserInfoResponseRepository";
    private final SingleLiveEvent<UserInfoResponse> userInfoResponseLiveData = new SingleLiveEvent<>();
    Context context;
    AppDatabase appDatabase;
    private UserInfoResponseRepository(Context context) {
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
    }

    private static UserInfoResponseRepository userInfoResponseRepository;

    public static UserInfoResponseRepository getInstance(Context context) {
        if (userInfoResponseRepository == null) {
            userInfoResponseRepository = new UserInfoResponseRepository(context);
        }
        return userInfoResponseRepository;
    }

    public SingleLiveEvent<UserInfoResponse> getUserInfo(String accessToken) {
        return getUserInfo(accessToken, false);
    }

    // Overloaded function to get user information with silentOnError flag
    public SingleLiveEvent<UserInfoResponse> getUserInfo(String accessToken, boolean silentOnError) {
        List<OPConfiguration> opConfigurationList = appDatabase.opConfigurationDao().getAll();
        if (opConfigurationList == null || opConfigurationList.isEmpty()) {
            userInfoResponseLiveData.setValue(setErrorInLiveObject("OpenID configuration not found in database."));
            return userInfoResponseLiveData;
        }
        OPConfiguration opConfiguration = opConfigurationList.get(0);
        // Create a call to fetch user information
        Call<Object> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface().getUserInfo(accessToken, "Bearer " + accessToken, opConfiguration.getUserinfoEndpoint());
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                // this method is called when we get response from our api.

                if (response.code() == 200) {
                    Object responseFromAPI = response.body();
                    Log.d("getUserInfo Response :: getUserInfo ::", responseFromAPI.toString());

                    if (responseFromAPI != null) {
                        UserInfoResponse userInfoResponse = new UserInfoResponse();
                        userInfoResponse.setReponse(responseFromAPI);
                        userInfoResponse.setSuccessful(true);
                        userInfoResponseLiveData.setValue(userInfoResponse);
                    } else {
                        Log.e(TAG, "User-Info is null");
                        userInfoResponseLiveData.setValue(setErrorInLiveObject("User-Info is null"));
                    }
                } else {
                    Log.e(TAG, "Error in fetching getUserInfo.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    if (!silentOnError) {
                        userInfoResponseLiveData.setValue(setErrorInLiveObject("Error in fetching getUserInfo.\n Error code: " + response.code() + "\n Error message: " + response.message()));
                    }
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e(TAG, "Error in fetching getUserInfo :: " + t.getMessage());
                if (!silentOnError) {
                    userInfoResponseLiveData.setValue(setErrorInLiveObject("Error in fetching getUserInfo :: " + t.getMessage()));

                }
            }
        });
        return userInfoResponseLiveData;
    }
    private UserInfoResponse setErrorInLiveObject(String errorMessage) {
        OperationError operationError = new OperationError.Builder()
                .title("Error")
                .message(errorMessage)
                .build();
        UserInfoResponse userInfoResponse = new UserInfoResponse(false, operationError);
        return userInfoResponse;
    }
}
