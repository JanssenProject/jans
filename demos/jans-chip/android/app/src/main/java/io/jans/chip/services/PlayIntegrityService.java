package io.jans.chip.services;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.android.play.core.integrity.model.IntegrityErrorCode;
import java.util.UUID;
import io.jans.chip.modal.appIntegrity.AppIntegrityResponse;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.utils.AppConfig;
import retrofit2.Call;
import retrofit2.Callback;

public class PlayIntegrityService {
    Context context;

    public PlayIntegrityService(Context context) {
        this.context = context;
    }

    public void checkAppIntegrity() {
        DBHandler dbH = new DBHandler(context, AppConfig.SQLITE_DB_NAME, null, 1);
        // Create an instance of a manager.
        IntegrityManager integrityManager = IntegrityManagerFactory.create(this.context);

        // Request the integrity token by providing a nonce.
        Task<IntegrityTokenResponse> integrityTokenResponse = integrityManager
                .requestIntegrityToken(IntegrityTokenRequest.builder().setNonce(UUID.randomUUID().toString()).setCloudProjectNumber(AppConfig.GOOGLE_CLOUD_PROJECT_ID).build());
        integrityTokenResponse.addOnSuccessListener(integrityTokenResponse1 -> {
            String integrityToken = integrityTokenResponse1.token();
            Log.d("Integrity token Obtained result", "success");
            getTokenResponse(dbH, integrityToken);
        });

        integrityTokenResponse.addOnFailureListener(e -> {
            Log.e("Integrity token Obtained result", "failure");
            dbH.addAppIntegrity(setErrorInAppIntegrityObj("Error in obtaining integrity token :: " + getErrorText(e)));
            throw new RuntimeException(e);
        });
    }

    private void getTokenResponse(DBHandler dbH, String integrityToken) {
        Log.d("INTEGRITY_APP_SERVER_URL", AppConfig.INTEGRITY_APP_SERVER_URL + "/api/check?token=" + integrityToken);
        Call<AppIntegrityResponse> call = RetrofitClient.getInstance(AppConfig.INTEGRITY_APP_SERVER_URL).getAPIInterface().verifyIntegrityTokenOnAppServer(AppConfig.INTEGRITY_APP_SERVER_URL + "/api/check?token=" + integrityToken);

        call.enqueue(new Callback<AppIntegrityResponse>() {
            @Override
            public void onResponse(Call<AppIntegrityResponse> call, retrofit2.Response<AppIntegrityResponse> response) {
                if (response.code() == 200) {
                    AppIntegrityResponse appIntegrityResponse = response.body();

                    if (appIntegrityResponse == null) {
                        Log.e("Response from App server :: ", "Response body is empty");
                        dbH.addAppIntegrity(setErrorInAppIntegrityObj("Empty response obtained from App Server."));
                    }
                    if (appIntegrityResponse.getError() != null) {
                        Log.e("Response from App server :: ", "Response body has error");
                        dbH.addAppIntegrity(setErrorInAppIntegrityObj("Response from App server has error :: " + appIntegrityResponse.getError()));
                    }
                    if (appIntegrityResponse.getAppIntegrity() == null || appIntegrityResponse.getAppIntegrity().getAppRecognitionVerdict() == null) {
                        Log.e("Response from App server :: ", "Response body do not have appIntegrity");
                        dbH.addAppIntegrity(setErrorInAppIntegrityObj("Response body do not have appIntegrity."));
                    }

                    Log.d("Inside getTokenResponse :: appIntegrityResponse ::", appIntegrityResponse.getAppIntegrity().getAppRecognitionVerdict());
                    dbH.addAppIntegrity(appIntegrityResponse);
                } else {
                    Log.e("Response from App server :: Unsuccessful, Response code :: ", String.valueOf(response.code()));
                    dbH.addAppIntegrity(setErrorInAppIntegrityObj("Error in obtaining response from aap server :: Response code :: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<AppIntegrityResponse> call, Throwable t) {
                Log.e("Error in fetching AppIntegrity :: ", t.getMessage());
                dbH.addAppIntegrity(setErrorInAppIntegrityObj("Error in fetching AppIntegrity :: " + t.getMessage()));

            }
        });
    }

    private String getErrorText(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return "Unknown Error";
        }

        int errorCode = Integer.parseInt(msg.replaceAll("\n", "").replaceAll(":(.*)", ""));
        switch (errorCode) {
            case IntegrityErrorCode.API_NOT_AVAILABLE:
                return "Integrity API is not available.\n\n" +
                        "The Play Store version might be old, try updating it.";
            case IntegrityErrorCode.APP_NOT_INSTALLED:
                return "The calling app is not installed.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.APP_UID_MISMATCH:
                return "The calling app UID (user id) does not match the one from Package Manager.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.CANNOT_BIND_TO_SERVICE:
                return "Binding to the service in the Play Store has failed.\n\n" +
                        "This can be due to having an old Play Store version installed on the device.";
            case IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE:
                return "Unknown internal Google server error.";
            case IntegrityErrorCode.INTERNAL_ERROR:
                return "Unknown internal error.";
            case IntegrityErrorCode.NETWORK_ERROR:
                return "No available network is found.\n\n" +
                        "Please check your connection.";
            case IntegrityErrorCode.NO_ERROR:
                return "No error has occurred.\n\n" +
                        "If you ever get this, congrats, I have no idea what it means.";
            case IntegrityErrorCode.NONCE_IS_NOT_BASE64:
                return "Nonce is not encoded as a base64 web-safe no-wrap string.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.NONCE_TOO_LONG:
                return "Nonce length is too long.\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.NONCE_TOO_SHORT:
                return "Nonce length is too short.\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            case IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND:
                return "Play Services is not available or version is too old.\n\n" +
                        "Try updating Google Play Services.";
            case IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND:
                return "No Play Store account is found on device.\n\n" +
                        "Try logging into Play Store.";
            case IntegrityErrorCode.PLAY_STORE_NOT_FOUND:
                return "No Play Store app is found on device or not official version is installed.\n\n" +
                        "This app can't work without Play Store.";
            case IntegrityErrorCode.TOO_MANY_REQUESTS:
                return "The calling app is making too many requests to the API and hence is throttled.\n\n" +
                        "This shouldn't happen. If it does please open an issue on Github.";
            default:
                return "Unknown Error";
        }
    }

    private AppIntegrityResponse setErrorInAppIntegrityObj(String error) {
        AppIntegrityResponse appIntegrity = new AppIntegrityResponse();
        appIntegrity.setError(error);
        return appIntegrity;

    }
}
