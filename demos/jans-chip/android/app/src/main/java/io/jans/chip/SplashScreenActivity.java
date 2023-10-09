package io.jans.chip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import java.util.List;

import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.appIntegrity.AppIntegrityEntity;
import io.jans.chip.modal.appIntegrity.AppIntegrityResponse;
import io.jans.chip.modelview.PlayIntegrityViewModel;

public class SplashScreenActivity extends AppCompatActivity {
    Handler handler;
    int handlerDelay = 1000;
    PlayIntegrityViewModel playIntegrityViewModel;
    AppDatabase appDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        // Initialize a Handler to manage the delay
        handler = new Handler();
        playIntegrityViewModel = new PlayIntegrityViewModel(getApplicationContext());
        appDatabase = AppDatabase.getInstance(this);
        //check app integrity
        checkAppIntegrity(appDatabase);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // Create a database handler to interact with your database
                List<OIDCClient> oidcClientList = appDatabase.oidcClientDao().getAll();
                OIDCClient client = null;
                if (oidcClientList != null && !oidcClientList.isEmpty()) {
                    client = oidcClientList.get(0);
                }

                if (client == null || client.getClientId() == null) {
                    // If the client is not found or its client ID is null, navigate to the MainActivity
                    Intent intent = new Intent(SplashScreenActivity.this, RegisterActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // If the client is found, log its details and navigate to the LoginActivity
                    Log.d("client", client.toString());
                    Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }

            }
        }, handlerDelay); // Delay execution for 1000 milliseconds (1 second)
    }

    private void checkAppIntegrity(AppDatabase appDatabase) {
        List<AppIntegrityEntity> appIntegrityEntityList = appDatabase.appIntegrityDao().getAll();
        AppIntegrityEntity appIntegrityEntity = null;
        if(appIntegrityEntityList != null && !appIntegrityEntityList.isEmpty()){
            appIntegrityEntity = appIntegrityEntityList.get(0);
        }
        if(appIntegrityEntity == null || appIntegrityEntity.getError() != null) {
            appDatabase.appIntegrityDao().deleteAll();
            playIntegrityViewModel.checkAppIntegrity()
                    .observe(SplashScreenActivity.this, new Observer<AppIntegrityResponse>() {

                        @Override
                        public void onChanged(AppIntegrityResponse appIntegrityResponse) {
                            if(!appIntegrityResponse.isSuccessful()) {
                                Toast.makeText(SplashScreenActivity.this, appIntegrityResponse.getOperationError().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            handlerDelay = 20000;
        }
    }
}