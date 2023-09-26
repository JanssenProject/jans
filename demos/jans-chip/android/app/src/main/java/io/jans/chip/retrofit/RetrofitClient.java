package io.jans.chip.retrofit;

import io.jans.chip.services.APIInterface;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance = null;
    private APIInterface apiInterface;

    private RetrofitClient(String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiInterface = retrofit.create(APIInterface.class);
    }

    public static synchronized RetrofitClient getInstance(String baseUrl) {
        if (instance == null) {
            instance = new RetrofitClient(baseUrl);
        }
        return instance;
    }

    public APIInterface getAPIInterface() {
        return apiInterface;
    }
}
