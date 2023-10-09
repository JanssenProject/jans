package io.jans.chip;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import java.util.List;

import io.jans.chip.modal.LoginResponse;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.TokenResponse;
import io.jans.chip.modal.UserInfoResponse;
import io.jans.chip.modelview.LoginViewModel;
import io.jans.chip.modelview.TokenViewModel;
import io.jans.chip.modelview.UserInfoViewModel;

public class LoginActivity extends AppCompatActivity {

    EditText username;
    EditText password;
    Button loginButton;
    ProgressBar loginProgressBar;
    AlertDialog.Builder errorDialog;
    public static final String USER_INFO = "io.jans.DPoP.LoginActivity.USER_INFO";
    AppDatabase appDatabase;
    LoginViewModel loginViewModel;
    TokenViewModel tokenViewModel;
    UserInfoViewModel userInfoViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        errorDialog = new AlertDialog.Builder(this);
        appDatabase = AppDatabase.getInstance(this);
        loginViewModel = new LoginViewModel(getApplicationContext());
        tokenViewModel = new TokenViewModel(getApplicationContext());
        userInfoViewModel = new UserInfoViewModel(getApplicationContext());

        loginProgressBar = findViewById(R.id.loginProgressBar);
        loginButton = findViewById(R.id.loginButton);
        // Check if OIDCClient is available, if not, navigate to MainActivity
        List<OIDCClient> oidcClientList = appDatabase.oidcClientDao().getAll();
        if (oidcClientList == null || oidcClientList.isEmpty()) {
            createErrorDialog("OpenID client not found in database.");
            errorDialog.show();
        }
        OIDCClient oidcClient = oidcClientList.get(0);
        if (oidcClient == null || oidcClient.getClientId() == null) {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        }
        // If a recent access token is available, automatically try to fetch user info
        if (oidcClient.getRecentGeneratedAccessToken() != null) {
            loginProgressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            userInfoViewModel.getUserInfo(oidcClient.getRecentGeneratedAccessToken(), true)
                    .observe(this, new Observer<UserInfoResponse>() {
                        @Override
                        public void onChanged(UserInfoResponse userInfoResponse) {
                            if (userInfoResponse.isSuccessful()) {
                                Intent intent = new Intent(LoginActivity.this, AfterLoginActivity.class);
                                intent.putExtra(USER_INFO, userInfoResponse.getReponse().toString());
                                startActivity(intent);
                            } else {
                                createErrorDialog(userInfoResponse.getOperationError().getMessage());
                                errorDialog.show();
                                loginButton.setEnabled(true);
                                loginProgressBar.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
        }
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                username = findViewById(R.id.username);
                password = findViewById(R.id.password);
                String usernameText = username.getText().toString();
                String passwordText = password.getText().toString();

                if (validateInputs()) {
                    loginProgressBar.setVisibility(View.VISIBLE);
                    loginButton.setEnabled(false);

                    loginViewModel.processlogin(usernameText, passwordText).observe(LoginActivity.this, new Observer<LoginResponse>() {

                        @Override
                        public void onChanged(LoginResponse loginResponse) {
                            if (loginResponse.isSuccessful()) {
                                tokenViewModel.getToken(loginResponse.getAuthorizationCode(), usernameText, passwordText)
                                        .observe(LoginActivity.this, new Observer<TokenResponse>() {

                                            @Override
                                            public void onChanged(TokenResponse tokenResponse) {
                                                if (tokenResponse.isSuccessful()) {
                                                    userInfoViewModel.getUserInfo(tokenResponse.getAccessToken(), false)
                                                            .observe(LoginActivity.this, new Observer<UserInfoResponse>() {
                                                                @Override
                                                                public void onChanged(UserInfoResponse userInfoResponse) {
                                                                    if (userInfoResponse.isSuccessful()) {
                                                                        Intent intent = new Intent(LoginActivity.this, AfterLoginActivity.class);
                                                                        intent.putExtra(USER_INFO, userInfoResponse.getReponse().toString());
                                                                        startActivity(intent);
                                                                    } else {
                                                                        createErrorDialog(userInfoResponse.getOperationError().getMessage());
                                                                        errorDialog.show();
                                                                        loginButton.setEnabled(true);
                                                                        loginProgressBar.setVisibility(View.INVISIBLE);
                                                                    }
                                                                }
                                                            });
                                                } else {
                                                    createErrorDialog(tokenResponse.getOperationError().getMessage());
                                                    errorDialog.show();
                                                }

                                            }
                                        });
                            } else {
                                createErrorDialog(loginResponse.getOperationError().getMessage());
                                errorDialog.show();
                            }
                        }
                    });
                }
            }
        });
    }

    private boolean validateInputs() {
        if (username == null || username.length() == 0) {
            createErrorDialog("Username cannot be left empty.");
            errorDialog.show();
            return false;
        }
        if (password == null || password.length() == 0) {
            createErrorDialog("Password cannot be left empty.");
            errorDialog.show();
            return false;
        }
        return true;
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