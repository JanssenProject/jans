package io.jans.chip;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import java.util.List;

import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.appIntegrity.AppIntegrityEntity;
import io.jans.chip.modelview.DCRViewModel;
import io.jans.chip.modelview.OPConfigurationViewModel;

public class RegisterActivity extends AppCompatActivity {

    EditText issuer;
    EditText scopes;
    Button registerButton;
    ProgressBar registerProgressBar;
    AlertDialog.Builder errorDialog;
    TextView appIntegrityText;
    TextView deviceIntegrityText;
    OPConfigurationViewModel opConfigurationViewModel;
    DCRViewModel dcrViewModel;
    AppDatabase appDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        errorDialog = new AlertDialog.Builder(this);
        appDatabase = AppDatabase.getInstance(this);
        opConfigurationViewModel = new OPConfigurationViewModel(getApplicationContext());
        dcrViewModel = new DCRViewModel(getApplicationContext());

        registerProgressBar = findViewById(R.id.registerProgressBar);
        registerButton = findViewById(R.id.registerButton);

        //Display app integrity
        displayAppIntegrity(appDatabase);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                issuer = findViewById(R.id.issuer);
                scopes = findViewById(R.id.scopes);
                String issuerText = issuer.getText().toString();
                String scopeText = scopes.getText().toString();

                if (validateInputs()) {
                    registerProgressBar.setVisibility(View.VISIBLE);
                    registerButton.setEnabled(false);
                    opConfigurationViewModel.fetchOPConfiguration(issuerText)
                            .observe(RegisterActivity.this, new Observer<OPConfiguration>() {
                                @Override
                                public void onChanged(OPConfiguration opConfiguration) {
                                    if (opConfiguration.isSuccessful()) {
                                        dcrViewModel.doDCR(scopeText).observe(RegisterActivity.this, new Observer<OIDCClient>() {

                                            @Override
                                            public void onChanged(OIDCClient oidcClient) {
                                                if (opConfiguration.isSuccessful()) {
                                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                    startActivity(intent);
                                                } else {
                                                    createErrorDialog(opConfiguration.getOperationError().getMessage());
                                                    errorDialog.show();
                                                    registerProgressBar.setVisibility(View.INVISIBLE);
                                                    registerButton.setEnabled(true);
                                                }
                                            }
                                        });
                                    } else {
                                        createErrorDialog(opConfiguration.getOperationError().getMessage());
                                        errorDialog.show();
                                        registerProgressBar.setVisibility(View.INVISIBLE);
                                        registerButton.setEnabled(true);
                                    }

                                }
                            });
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

    private void displayAppIntegrity(AppDatabase appDatabase) {
        List<AppIntegrityEntity> appIntegrityEntityList = appDatabase.appIntegrityDao().getAll();
        if (appIntegrityEntityList == null || appIntegrityEntityList.isEmpty()) {
            createErrorDialog("App integrity not found in database.");
            errorDialog.show();
            return;
        }
        AppIntegrityEntity appIntegrityEntity = appIntegrityEntityList.get(0);
        appIntegrityText = findViewById(R.id.appIntegrityText);
        deviceIntegrityText = findViewById(R.id.deviceIntegrityText);

        if (appIntegrityEntity == null) {
            appIntegrityText.setText("Unable to fetch App Integrity data frpm App server.");
            registerButton.setEnabled(false);
        }
        if (appIntegrityEntity.getError() != null) {
            appIntegrityText.setText(appIntegrityEntity.getError());
            registerButton.setEnabled(false);
        }
        if (appIntegrityEntity.getAppIntegrity() != null) {
            appIntegrityText.setText(appIntegrityEntity.getAppLicensingVerdict());
        }
        if (appIntegrityEntity.getDeviceIntegrity() != null) {
            deviceIntegrityText.setText(appIntegrityEntity.getDeviceIntegrity());
        }

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