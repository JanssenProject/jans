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
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.retrofit.RetrofitClient;
import io.jans.chip.services.DBHandler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AfterLoginActivity extends AppCompatActivity {
    TextView message;
    Button logoutButton;
    AlertDialog.Builder errorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_login);
        errorDialog = new AlertDialog.Builder(this);
        message = findViewById(R.id.userInfo);
        // Retrieve user info from the intent passed from LoginActivity
        Intent intent = getIntent();
        String userInfo = intent.getStringExtra(LoginActivity.USER_INFO);
        message.setText("User Info is: " + userInfo);

        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DBHandler dbH = new DBHandler(AfterLoginActivity.this, "chipDB", null, 1);
                logout(dbH);
            }
        });
    }

    private void logout(DBHandler dbH) {
        Toast.makeText(AfterLoginActivity.this, "Processing Logout.", Toast.LENGTH_SHORT).show();
        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        OIDCClient oidcClient = dbH.getOIDCClient(1);
        Log.d("Logout access token :: ", oidcClient.getRecentGeneratedAccessToken());
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
                    dbH.updateOIDCClient(oidcClient);

                    Intent intent = new Intent(AfterLoginActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    createErrorDialog("Error in fetching User-Info.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    errorDialog.show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Inside getUserInfo :: onFailure :: ", t.getMessage());
                //Toast.makeText(AfterLoginActivity.this, "Error in Logout. : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                createErrorDialog("Error in fetching User-Info :: " + t.getMessage());
                errorDialog.show();
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