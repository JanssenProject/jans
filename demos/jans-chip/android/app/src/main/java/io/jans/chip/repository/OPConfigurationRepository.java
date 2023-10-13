package io.jans.chip.repository;

import android.content.Context;
import android.util.Log;

import io.jans.chip.AppDatabase;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.OperationError;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.SingleLiveEvent;
import io.jans.chip.utils.AppConfig;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OPConfigurationRepository {
    public static final String TAG = "OPConfigurationRepository";
    private final SingleLiveEvent<OPConfiguration> opConfigurationLiveData = new SingleLiveEvent<>();
    Context context;
    AppDatabase appDatabase;
    private OPConfigurationRepository(Context context) {
        this.context = context;
        appDatabase = AppDatabase.getInstance(context);
    }

    private static OPConfigurationRepository opConfigurationRepository;

    public static OPConfigurationRepository getInstance(Context context) {
        if (opConfigurationRepository == null) {
            opConfigurationRepository = new OPConfigurationRepository(context);
        }
        return opConfigurationRepository;
    }

    public SingleLiveEvent<OPConfiguration> fetchOPConfiguration(String configurationUrl) {

        String issuer = configurationUrl.replace("/.well-known/openid-configuration", "");
        Log.d(TAG, "Inside fetchOPConfiguration :: configurationUrl ::" + configurationUrl);
        try {
            Call<OPConfiguration> call = RetrofitClient.getInstance(issuer).getAPIInterface().getOPConfiguration(configurationUrl);

            call.enqueue(new Callback<OPConfiguration>() {
                @Override
                public void onResponse(Call<OPConfiguration> call, Response<OPConfiguration> response) {
                    if (response.code() == 200) {
                        OPConfiguration opConfiguration = response.body();
                        opConfiguration.setSuccessful(true);
                        opConfiguration.setOperationError(null);
                        opConfiguration.setSno(AppConfig.DEFAULT_S_NO);
                        Log.d(TAG, "Inside fetchOPConfiguration :: opConfiguration :: " + opConfiguration.toString());
                        appDatabase.opConfigurationDao().deleteAll();
                        appDatabase.opConfigurationDao().insert(opConfiguration);
                        opConfigurationLiveData.setValue(opConfiguration);
                    } else {
                        opConfigurationLiveData.setValue(setErrorInLiveObject("Error in fetching OP Configuration.\n Error code: " + response.code() + "\n Error message: " + response.message()));

                    }
                }

                @Override
                public void onFailure(Call<OPConfiguration> call, Throwable t) {
                    Log.e(TAG, "Inside fetchOPConfiguration :: onFailure :: " + t.getMessage());
                    opConfigurationLiveData.setValue(setErrorInLiveObject("Error in  fetching OP Configuration.\n" + t.getMessage()));

                }
            });
        } catch (Exception e) {
            opConfigurationLiveData.setValue(setErrorInLiveObject("Error in  fetching OP Configuration.\n" + e.getMessage()));
        }
        return opConfigurationLiveData;

    }
    private OPConfiguration setErrorInLiveObject(String errorMessage) {
        OperationError operationError = new OperationError.Builder()
                .title("Error")
                .message(errorMessage)
                .build();
        OPConfiguration opConfiguration = new OPConfiguration(false, operationError);
        return opConfiguration;
    }
}