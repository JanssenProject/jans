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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.appIntegrity.AppIntegrityEntity;
import io.jans.chip.modelview.DCRViewModel;
import io.jans.chip.modelview.OPConfigurationViewModel;
import io.jans.chip.utils.AppConfig;

public class RegisterActivity extends AppCompatActivity {

    EditText issuer;
    TextView scopes;
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
        issuer = findViewById(R.id.issuer);
        scopes = findViewById(R.id.scopes);
        showItemSelectionPopup(scopes, AppConfig.scopeArray);

        //Display app integrity
        displayAppIntegrity(appDatabase);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectScopesByDefault(scopes, AppConfig.defaultScopeArray);
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
                                                    showErrorDialog(opConfiguration.getOperationError().getMessage());
                                                }
                                            }
                                        });
                                    } else {
                                        showErrorDialog(opConfiguration.getOperationError().getMessage());
                                    }

                                }
                            });
                }
            }
        });
    }

    private void showErrorDialog(String message) {
        createErrorDialog(message);
        errorDialog.show();
        registerProgressBar.setVisibility(View.INVISIBLE);
        registerButton.setEnabled(true);
    }

    private void selectScopesByDefault(TextView scopesEditText, String[] scopeArray) {
        // Initialize string builder
        StringBuilder stringBuilder = new StringBuilder();
        Set<String> scopeSet = new HashSet<>();
        scopeSet.addAll(Arrays.asList(scopeArray));

        String scopesString = scopesEditText.getText().toString();
        scopeSet.addAll(Arrays.asList(scopesString.split(" ")));

        scopeSet.stream().forEach(scope -> {
            stringBuilder.append(scope);
            stringBuilder.append(" ");
        });

        // set text on textView
        scopesEditText.setText(stringBuilder.toString());
    }

    private void showItemSelectionPopup(TextView scopesEditText, String[] scopeArray) {

        boolean[] selectedScope = new boolean[scopeArray.length];
        List<Integer> scopeList = new ArrayList<>();
        scopesEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Initialize alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                // set title
                builder.setTitle("Select Scope");
                // set dialog non cancelable
                builder.setCancelable(false);
                builder.setMultiChoiceItems(scopeArray, selectedScope, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        // check condition
                        if (b) {
                            // when checkbox selected
                            // Add position  in scope list
                            scopeList.add(i);
                            // Sort array list
                            Collections.sort(scopeList);
                        } else {
                            // when checkbox unselected
                            // Remove position from scopeList
                            scopeList.remove(Integer.valueOf(i));
                        }
                    }
                });

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Initialize string builder
                        StringBuilder stringBuilder = new StringBuilder();
                        // use for loop
                        for (int j = 0; j < scopeList.size(); j++) {
                            // concat array value
                            stringBuilder.append(scopeArray[scopeList.get(j)]);
                            // check condition
                            if (j != scopeList.size() - 1) {
                                // When j value  not equal
                                // to scope list size - 1
                                // add comma
                                stringBuilder.append(" ");
                            }
                        }
                        // set text on textView
                        scopesEditText.setText(stringBuilder.toString());
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // dismiss dialog
                        dialogInterface.dismiss();
                    }
                });
                builder.setNeutralButton("Clear All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // use for loop
                        for (int j = 0; j < selectedScope.length; j++) {
                            // remove all selection
                            selectedScope[j] = false;
                            // clear scope list
                            scopeList.clear();
                            // clear text view value
                            scopesEditText.setText("");
                        }
                    }
                });
                // show dialog
                builder.show();
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