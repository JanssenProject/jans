package io.jans.chip.services;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.android.play.core.integrity.model.IntegrityErrorCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import io.jans.chip.SplashScreenActivity;
import io.jans.chip.modal.AppIntegrity;
import io.jans.chip.services.DBHandler;
import io.jans.chip.utils.AppConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

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
            new getTokenResponse(dbH).execute(integrityToken);
        });

        integrityTokenResponse.addOnFailureListener(e -> {
            Log.e("Integrity token Obtained result", "failure");
            //Toast.makeText(context, "Integrity token Obtained result :: failure", Toast.LENGTH_SHORT).show();
            dbH.addAppIntegrity(setErrorInAppIntegrityObj("Error in obtaining integrity token :: " + e.getMessage()));
            throw new RuntimeException(e);
        });
    }

    private class getTokenResponse extends AsyncTask<String, Integer, String[]> {

        private boolean hasError = false;
        private DBHandler dbH;

        public getTokenResponse(DBHandler dbH) {
            this.dbH = dbH;
        }

        @Override
        protected String[] doInBackground(String... token) {

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .get()
                    .url(AppConfig.INTEGRITY_APP_SERVER_URL + "/api/check?token=" + token[0])
                    .build();

            Response response = null;
            try {
                response = client.newCall(request).execute();


                if (!response.isSuccessful()) {
                    hasError = true;
                    Log.e("Response from App server :: Unsuccessful, Response code :: ", String.valueOf(response.code()));
                    //Toast.makeText(context, "Response from App server :: Unsuccessful, Response code :: " + response.code(), Toast.LENGTH_SHORT).show();
                    dbH.addAppIntegrity(setErrorInAppIntegrityObj("Error in obtaining response from aap server :: Response code :: " + response.code()));
                    return new String[]{"Api request error", "Response code: " + response.code()};
                }
                ResponseBody responseBody = response.body();

                if (responseBody == null) {
                    hasError = true;
                    Log.e("Response from App server :: ", "Response body is empty");
                    //Toast.makeText(context, "Api request error :: Empty response", Toast.LENGTH_SHORT).show();
                    dbH.addAppIntegrity(setErrorInAppIntegrityObj("Empty response obtained from App Server."));
                    return new String[]{"Api request error", "Empty response"};
                }

                JSONObject json = null;
                json = new JSONObject(responseBody.string());


                if (json.has("error")) {
                    hasError = true;
                    Log.e("Response from App server :: ", "Response body has error");
                    //Toast.makeText(context, "Api request error :: " + json.getString("error"), Toast.LENGTH_SHORT).show();
                    dbH.addAppIntegrity(setErrorInAppIntegrityObj("Response from App server has error :: " + json.getString("error")));
                    return new String[]{"Api request error", json.getString("error")};
                }

                if (!json.has("appIntegrity")) {
                    hasError = true;
                    Log.e("Response from App server :: ", "Response body do not have appIntegrity");
                    //Toast.makeText(context, "Api request error :: Response does not contain appIntegrity", Toast.LENGTH_SHORT).show();
                    dbH.addAppIntegrity(setErrorInAppIntegrityObj("Response body do not have appIntegrity."));
                    return new String[]{"Api request error", "Response does not contain appIntegrity"};
                }
                return new String[]{json.toString()};
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        protected void onBackgroundError(Exception e) {
            hasError = true;
            onPostExecute(new String[]{"Api request error", e.getMessage()});
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (hasError) {
                Log.d("Response from App server has error :: ", result[0]);
                //Toast.makeText(context, "Api request error :: " + result[0], Toast.LENGTH_SHORT).show();
                dbH.addAppIntegrity(setErrorInAppIntegrityObj("Response from App server has error :: " + result[0]));
                //showErrorDialog(result[0], result[1]);
            } else {
                String json = result[0];
                Log.d("Response from App server is successful :: ", json);
                AppIntegrity appIntegrity = new AppIntegrity();
                appIntegrity.setResponseString(json);
                dbH.addAppIntegrity(appIntegrity);
                Toast.makeText(context, "Api request Success :: " + json, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getErrorText(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return "Unknown Error";
        }

        //Pretty junk way of getting the error code but it works
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

    private AppIntegrity setErrorInAppIntegrityObj(String error) {
        AppIntegrity appIntegrity = new AppIntegrity();
        appIntegrity.setError(error);
        return appIntegrity;

    }
}
