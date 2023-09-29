package io.jans.chip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import io.jans.chip.modal.AppIntegrity;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.services.DBHandler;
import io.jans.chip.services.PlayIntegrityService;
import io.jans.chip.utils.AppConfig;

public class SplashScreenActivity extends Activity {
    Handler handler;
    int handlerDelay = 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        // Initialize a Handler to manage the delay
        handler = new Handler();

        DBHandler dbH = new DBHandler(SplashScreenActivity.this, AppConfig.SQLITE_DB_NAME, null, 1);

        //check app integrity
        checkAppIntegrity(dbH);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                // Create a database handler to interact with your database
                OIDCClient client = dbH.getOIDCClient(1);

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

    private void checkAppIntegrity(DBHandler dbH) {
        AppIntegrity appIntegrity = dbH.getAppIntegrity();
        if(appIntegrity == null) {
            PlayIntegrityService PlayIntegrityService = new PlayIntegrityService(getApplicationContext());
            PlayIntegrityService.checkAppIntegrity();
            handlerDelay = 20000;
        }
    }
}