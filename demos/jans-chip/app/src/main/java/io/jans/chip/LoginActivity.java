package io.jans.chip;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.UUID;

import io.jans.chip.keyGen.DPoPProofFactory;
import io.jans.chip.modal.LoginResponse;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.TokenResponse;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.DBHandler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText username;
    EditText password;
    Button loginButton;
    ProgressBar loginProgressBar;
    AlertDialog.Builder errorDialog;
    public static final String USER_INFO = "io.jans.DPoP.LoginActivity.USER_INFO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        errorDialog = new AlertDialog.Builder(this);

        loginProgressBar = findViewById(R.id.loginProgressBar);
        loginButton = findViewById(R.id.loginButton);
        // Initialize the database handler
        DBHandler dbH = new DBHandler(LoginActivity.this, "chipDB", null, 1);
        // Check if OIDCClient is available, if not, navigate to MainActivity
        OIDCClient oidcClient = dbH.getOIDCClient(1);
        if (oidcClient == null || oidcClient.getClientId() == null) {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        }
        // If a recent access token is available, automatically try to fetch user info
        if (oidcClient.getRecentGeneratedAccessToken() != null) {
            loginProgressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);

            getUserInfo(oidcClient.getRecentGeneratedAccessToken(), dbH, true);
        }
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginProgressBar.setVisibility(View.VISIBLE);
                loginButton.setEnabled(false);

                username = findViewById(R.id.username);
                password = findViewById(R.id.password);
                String usernameText = username.getText().toString();
                String passwordText = password.getText().toString();

                processlogin(usernameText, passwordText, dbH);
            }
        });
    }

    // Function to handle the login process
    private void processlogin(String usernameText, String passwordText, DBHandler dbH) {
        // Get OPConfiguration and OIDCClient
        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        OIDCClient oidcClient = dbH.getOIDCClient(1);

        // Create a call to request an authorization challenge
        Call<LoginResponse> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface().
                getAuthorizationChallenge(oidcClient.getClientId(),
                        usernameText,
                        passwordText,
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        opConfiguration.getAuthorizationChallengeEndpoint());
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.code() == 200) {
                    LoginResponse responseFromAPI = response.body();
                    Log.d("processlogin Response :: getAuthorizationCode ::", responseFromAPI.getAuthorizationCode());

                    if (responseFromAPI.getAuthorizationCode() != null && !responseFromAPI.getAuthorizationCode().isEmpty()) {
                        getToken(responseFromAPI.getAuthorizationCode(), usernameText, passwordText, dbH);
                    }
                } else {
                    createErrorDialog("Error in  generating authorization code.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    errorDialog.show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("Inside processlogin :: onFailure :: ", t.getMessage());
                createErrorDialog("Error in  generating authorization code.\n" + t.getMessage());
                errorDialog.show();
            }
        });
    }

    // Function to request a token
    private void getToken(String authorizationCode, String usernameText, String passwordText, DBHandler dbH) {
        // Get OPConfiguration and OIDCClient
        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        OIDCClient oidcClient = dbH.getOIDCClient(1);

        try {
            Log.d("dpop token", DPoPProofFactory.issueDPoPJWTToken("POST", opConfiguration.getIssuer()));
            // Create a call to request a token
            Call<TokenResponse> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface()
                    .getToken(oidcClient.getClientId(),
                            authorizationCode,
                            "authorization_code",
                            opConfiguration.getIssuer(),
                            "openid",
                            "Basic " + Base64.encodeToString((oidcClient.getClientId() + ":" + oidcClient.getClientSecret()).getBytes(), Base64.NO_WRAP),
                            DPoPProofFactory.issueDPoPJWTToken("POST", opConfiguration.getIssuer()),
                            opConfiguration.getTokenEndpoint());
            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    // this method is called when we get response from our api.
                    if (response.code() == 200) {
                        TokenResponse responseFromAPI = response.body();

                        if (responseFromAPI.getAccessToken() != null && !responseFromAPI.getAccessToken().isEmpty()) {
                            Log.d("getToken Response :: getIdToken ::", responseFromAPI.getIdToken());
                            Log.d("getToken Response :: getTokenType ::", responseFromAPI.getTokenType());
                            oidcClient.setRecentGeneratedIdToken(responseFromAPI.getIdToken());
                            oidcClient.setRecentGeneratedAccessToken(responseFromAPI.getAccessToken());
                            dbH.updateOIDCClient(oidcClient);
                            getUserInfo(responseFromAPI.getAccessToken(), dbH);
                        }
                    } else {
                        createErrorDialog("Error in Token generation.\n Error code: " + response.code() + "\n Error message: " + response.message());
                        errorDialog.show();
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Log.e("Inside getToken :: onFailure :: ", t.getMessage());
                    //Toast.makeText(LoginActivity.this, "Error in Token generation : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    createErrorDialog("Error in fetching configuration.\n" + t.getMessage());
                    errorDialog.show();
                }
            });
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    // Function to get user information
    private void getUserInfo(String accessToken, DBHandler dbH) {
        getUserInfo(accessToken, dbH, false);
    }

    // Overloaded function to get user information with silentOnError flag
    private void getUserInfo(String accessToken, DBHandler dbH, boolean silentOnError) {
        OPConfiguration opConfiguration = dbH.getOPConfiguration();
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
                        Intent intent = new Intent(LoginActivity.this, AfterLoginActivity.class);
                        intent.putExtra(USER_INFO, responseFromAPI.toString());
                        startActivity(intent);
                    }
                } else {
                    if (!silentOnError) {
                        createErrorDialog("Error in fetching getUserInfo.\n Error code: " + response.code() + "\n Error message: " + response.message());
                        errorDialog.show();
                    }
                }
                loginButton.setEnabled(true);
                loginProgressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e("Error in fetching getUserInfo :: ", t.getMessage());
                //Toast.makeText(LoginActivity.this, "Error found is : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (!silentOnError) {
                    createErrorDialog("Error in fetching getUserInfo :: " + t.getMessage());
                    errorDialog.show();
                }
                loginButton.setEnabled(true);
                loginProgressBar.setVisibility(View.INVISIBLE);
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