package io.jans.chip;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import io.jans.chip.modal.LogoutResponse;
import io.jans.chip.modelview.LogoutViewModel;

public class AfterLoginActivity extends AppCompatActivity {
    TextView message;
    Button logoutButton;
    AlertDialog.Builder errorDialog;
    LogoutViewModel logoutViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_login);
        logoutViewModel = new LogoutViewModel(getApplicationContext());

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
                logoutViewModel.logout().observe(AfterLoginActivity.this, new Observer<LogoutResponse>() {

                    @Override
                    public void onChanged(LogoutResponse logoutResponse) {
                        if (logoutResponse.isSuccessful()) {
                            Intent intent = new Intent(AfterLoginActivity.this, LoginActivity.class);
                            startActivity(intent);
                        } else {
                            createErrorDialog(logoutResponse.getOperationError().getMessage());
                            errorDialog.show();
                        }

                    }
                });
                //logout(dbH);
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