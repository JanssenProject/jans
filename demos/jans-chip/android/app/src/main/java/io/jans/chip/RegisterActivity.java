package io.jans.chip;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.google.android.play.core.integrity.model.IntegrityErrorCode;
import com.google.common.collect.Lists;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import io.jans.chip.modal.AppIntegrity;
import io.jans.chip.utils.AppConfig;
import io.jans.chip.utils.ChecksumUtil;
import io.jans.chip.factories.DPoPProofFactory;
import io.jans.chip.factories.KeyManager;
import io.jans.chip.modal.DCRequest;
import io.jans.chip.modal.DCResponse;
import io.jans.chip.modal.JSONWebKeySet;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.DBHandler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText issuer;
    EditText scopes;
    Button registerButton;
    ProgressBar registerProgressBar;
    AlertDialog.Builder errorDialog;
    TextView appIntegrityText;
    TextView deviceIntegrityText;
    public static final String APP_LINK = "https://dpop.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        errorDialog = new AlertDialog.Builder(this);

        DBHandler dbH = new DBHandler(RegisterActivity.this, AppConfig.SQLITE_DB_NAME, null, 1);

        registerProgressBar = findViewById(R.id.registerProgressBar);
        registerButton = findViewById(R.id.registerButton);

        //Display app integrity
        displayAppIntegrity(dbH);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                issuer = findViewById(R.id.issuer);
                scopes = findViewById(R.id.scopes);
                String issuerText = issuer.getText().toString();
                String scopeText = scopes.getText().toString();

                if(validateInputs()) {
                    registerProgressBar.setVisibility(View.VISIBLE);
                    registerButton.setEnabled(false);
                    fetchOPConfiguration(issuerText, scopeText, dbH);
                }
            }
        });
    }

    private boolean validateInputs() {
        if (issuer == null || issuer.length() == 0) {
            createErrorDialog("Configuration endpoint cannot be left empty.");
            errorDialog.show();
            return false;
        }
        return true;
    }

    private void displayAppIntegrity(DBHandler dbH) {
        AppIntegrity appIntegrity = dbH.getAppIntegrity();
        appIntegrityText = findViewById(R.id.appIntegrityText);
        deviceIntegrityText = findViewById(R.id.deviceIntegrityText);

        if (appIntegrity == null) {
            appIntegrityText.setText("Unable to fetch App Integrity data frpm App server.");
            registerButton.setEnabled(false);
        } else if (appIntegrity.getError() != null) {
            appIntegrityText.setText(appIntegrity.getError());
            registerButton.setEnabled(false);
        } else if (appIntegrity.getResponse() != null) {
            try {
                JSONObject jo = new JSONObject(appIntegrity.getResponse().toString());
                if (jo.has("appIntegrity")) {
                    if(jo.getJSONObject("appIntegrity").has("appRecognitionVerdict")) {
                        appIntegrityText.setText(jo.getJSONObject("appIntegrity").getString("appRecognitionVerdict"));
                    }
                }
                if (jo.has("deviceIntegrity")) {
                    if(jo.getJSONObject("deviceIntegrity").has("deviceRecognitionVerdict")) {
                        deviceIntegrityText.setText(jo.getJSONObject("deviceIntegrity").getString("deviceRecognitionVerdict"));
                    }
                }
            } catch (JSONException e) {
                createErrorDialog("Error in  displaying App Integrity " + e.getMessage());
                errorDialog.show();
                registerButton.setEnabled(false);
            }
        }
    }

    private void fetchOPConfiguration(String configurationUrl, String scopeText, DBHandler dbH) {

        String issuer = configurationUrl.replace("/.well-known/openid-configuration", "");
        Log.d("Inside fetchOPConfiguration :: configurationUrl ::", configurationUrl);

        Call<OPConfiguration> call = RetrofitClient.getInstance(issuer).getAPIInterface().getOPConfiguration(configurationUrl);

        call.enqueue(new Callback<OPConfiguration>() {
            @Override
            public void onResponse(Call<OPConfiguration> call, Response<OPConfiguration> response) {
                if (response.code() == 200) {
                    OPConfiguration opConfiguration = response.body();
                    Log.d("Inside fetchOPConfiguration :: opConfiguration ::", opConfiguration.toString());
                    dbH.addOPConfiguration(opConfiguration);
                    doDCR(scopeText, dbH);
                } else {
                    createErrorDialog("Error in  fetching OP Configuration.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    errorDialog.show();
                }
            }

            @Override
            public void onFailure(Call<OPConfiguration> call, Throwable t) {
                Log.e("Inside fetchOPConfiguration :: onFailure :: ", t.getMessage());
                //Toast.makeText(MainActivity.this, "Error in fetching configuration : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                createErrorDialog("Error in  fetching OP Configuration.\n" + t.getMessage());
                errorDialog.show();
                registerProgressBar.setVisibility(View.INVISIBLE);
                registerButton.setEnabled(true);
            }
        });
    }

    private void doDCR(String scopeText, DBHandler dbH) {

        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        String issuer = opConfiguration.getIssuer();
        String registrationUrl = opConfiguration.getRegistrationEndpoint();

        DCRequest dcrRequest = new DCRequest();
        dcrRequest.setIssuer(issuer);
        dcrRequest.setClientName("jans-chip-" + UUID.randomUUID());
        dcrRequest.setApplicationType("web");
        dcrRequest.setGrantTypes(Lists.newArrayList("authorization_code", "client_credentials"));
        dcrRequest.setScope(scopeText);
        dcrRequest.setRedirectUris(Lists.newArrayList(issuer));
        dcrRequest.setResponseTypes(Lists.newArrayList("code"));
        dcrRequest.setTokenEndpointAuthMethod("client_secret_basic");
        dcrRequest.setPostLogoutRedirectUris(Lists.newArrayList(issuer));

        Map<String, Object> claims = new HashMap<>();
        claims.put("appName", "jans-chip");
        claims.put("seq", UUID.randomUUID());
        claims.put("app_id", getApplicationContext().getPackageName());
        String checksum = null;
        try {
            checksum = ChecksumUtil.getChecksum(this);
            claims.put("app_checksum", checksum);
        } catch (IOException | NoSuchAlgorithmException | PackageManager.NameNotFoundException e) {
            createErrorDialog("Error in  generating checksum.\n" + e.getMessage());
            errorDialog.show();
        }
        AppIntegrity appIntegrity = dbH.getAppIntegrity();
        claims.put("app_integrity_result", appIntegrity.getResponse().toString());
        try {
            String evidenceJwt = DPoPProofFactory.getInstance().issueJWTToken(claims);
            dcrRequest.setEvidence(evidenceJwt);
            Log.d("Inside doDCR :: evidence :: ", evidenceJwt);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                 NoSuchProviderException e) {
            createErrorDialog("Error in  DCR.\n" + e.getMessage());
            errorDialog.show();
            registerProgressBar.setVisibility(View.INVISIBLE);
            registerButton.setEnabled(true);
            throw new RuntimeException(e);
        }

        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.addKey(KeyManager.getPublicKeyJWK(KeyManager.getInstance().getPublicKey()).getRequiredParams());

        dcrRequest.setJwks(jwks.toJsonString());
        Log.d("Inside doDCR :: jwks :: ", jwks.toJsonString());

        Call<DCResponse> call = RetrofitClient.getInstance(issuer).getAPIInterface().doDCR(dcrRequest, registrationUrl);
        call.enqueue(new Callback<DCResponse>() {
            @Override
            public void onResponse(Call<DCResponse> call, Response<DCResponse> response) {

                DCResponse responseFromAPI = response.body();
                if (response.code() == 200 || response.code() == 201) {
                    if (responseFromAPI.getClientId() != null && !responseFromAPI.getClientId().isEmpty()) {
                        OIDCClient client = new OIDCClient();
                        client.setClientName(responseFromAPI.getClientName());
                        client.setClientId(responseFromAPI.getClientId());
                        client.setClientSecret(responseFromAPI.getClientSecret());
                        client.setScope(scopeText);
                        dbH.addOIDCClient(client);

                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                } else {
                    createErrorDialog("Error in  DCR.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    errorDialog.show();
                }
                registerProgressBar.setVisibility(View.INVISIBLE);
                registerButton.setEnabled(true);
            }

            @Override
            public void onFailure(Call<DCResponse> call, Throwable t) {
                Log.e("Inside doDCR :: onFailure :: ", t.getMessage());
                //Toast.makeText(MainActivity.this, "Error in DCR : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                createErrorDialog("Error in  DCR.\n" + t.getMessage());
                errorDialog.show();
                registerProgressBar.setVisibility(View.INVISIBLE);
                registerButton.setEnabled(true);
            }
        });
    }

    private void createErrorDialog(String message) {
        errorDialog.setMessage(message)
                .setTitle(R.string.error_title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
    }
}