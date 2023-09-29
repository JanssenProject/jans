package io.jans.chip.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import io.jans.chip.modal.AppIntegrity;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;

public class DBHandler extends SQLiteOpenHelper {
    public DBHandler(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS OIDC_CLIENT (SNO INTEGER PRIMARY KEY, CLIENT_NAME TEXT, CLIENT_ID TEXT, CLIENT_SECRET TEXT, PUBLIC_KEY TEXT, RECENT_GENERATED_ID_TOKEN TEXT, RECENT_GENERATED_ACCESS_TOKEN TEXT)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS OP_CONFIGURATION (REGISTRATION_ENDPOINT TEXT, TOKEN_ENDPOINT TEXT, USERINFO_ENDPOINT TEXT, AUTHORIZATION_CHALLENGE_ENDPOINT TEXT, ISSUER, REVOCATION_ENDPOINT TEXT)");
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS APP_INTEGRITY (RESPONSE TEXT, ERROR TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String drop = "DROP TABLE IF EXISTS";
        sqLiteDatabase.execSQL(drop);
        onCreate(sqLiteDatabase);
    }
    public void addAppIntegrity(AppIntegrity appIntegrity) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        if(appIntegrity.getResponse() != null) {
            values.put("RESPONSE", appIntegrity.getResponse().toString());
        }
        values.put("ERROR", appIntegrity.getError());
        long id = db.insert("APP_INTEGRITY", null, values);
        Log.d("DBHandler :: addAppIntegrity ::", Long.toString(id));
        db.close();
    }
    public AppIntegrity getAppIntegrity() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query("APP_INTEGRITY", new String[]{"RESPONSE", "ERROR"},
                null, null, null, null, null);
        if (result != null && result.moveToFirst()) {

            AppIntegrity appIntegrity = new AppIntegrity();
            appIntegrity.setResponseString(result.getString(0));
            appIntegrity.setError(result.getString(1));

            return appIntegrity;

        } else {
            Log.e("DBHandler :: getAppIntegrity :: ", "Some error occured!");
            return null;
        }
    }
    public void addOIDCClient(OIDCClient client) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("SNO", "1");
        values.put("CLIENT_NAME", client.getClientName());
        values.put("CLIENT_ID", client.getClientId());
        values.put("CLIENT_SECRET", client.getClientSecret());
        values.put("RECENT_GENERATED_ID_TOKEN", client.getRecentGeneratedIdToken());
        values.put("RECENT_GENERATED_ACCESS_TOKEN", client.getRecentGeneratedAccessToken());
        long id = db.insert("OIDC_CLIENT", null, values);
        Log.d("DBHandler :: addOIDCClient ::", Long.toString(id));
        db.close();
    }

    public void updateOIDCClient(OIDCClient client) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("CLIENT_NAME", client.getClientName());
        values.put("CLIENT_ID", client.getClientId());
        values.put("CLIENT_SECRET", client.getClientSecret());
        values.put("RECENT_GENERATED_ID_TOKEN", client.getRecentGeneratedIdToken());
        values.put("RECENT_GENERATED_ACCESS_TOKEN", client.getRecentGeneratedAccessToken());
        long id = db.update("OIDC_CLIENT", values, "SNO=?", new String[]{client.getSno()});
        Log.d("DBHandler :: updateOIDCClient ::", Long.toString(id));
        db.close();
    }

    public void addOPConfiguration(OPConfiguration configuration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ISSUER", configuration.getIssuer());
        values.put("REGISTRATION_ENDPOINT", configuration.getRegistrationEndpoint());
        values.put("TOKEN_ENDPOINT", configuration.getTokenEndpoint());
        values.put("USERINFO_ENDPOINT", configuration.getUserinfoEndpoint());
        values.put("AUTHORIZATION_CHALLENGE_ENDPOINT", configuration.getAuthorizationChallengeEndpoint());
        values.put("REVOCATION_ENDPOINT", configuration.getRevocationEndpoint());

        long id = db.insert("OP_CONFIGURATION", null, values);
        Log.d("DBHandler :: addOPConfiguration ::", Long.toString(id));
        db.close();
    }

    public OPConfiguration getOPConfiguration() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query("OP_CONFIGURATION", new String[]{"REGISTRATION_ENDPOINT", "TOKEN_ENDPOINT", "USERINFO_ENDPOINT", "AUTHORIZATION_CHALLENGE_ENDPOINT", "ISSUER", "REVOCATION_ENDPOINT"},
                null, null, null, null, null);
        if (result != null && result.moveToFirst()) {
            Log.d("getConfiguration 1", result.getString(0));
            Log.d("getConfiguration 2", result.getString(1));
            OPConfiguration configuration = new OPConfiguration();
            configuration.setRegistrationEndpoint(result.getString(0));
            configuration.setTokenEndpoint(result.getString(1));
            configuration.setUserinfoEndpoint(result.getString(2));
            configuration.setAuthorizationChallengeEndpoint(result.getString(3));
            configuration.setIssuer(result.getString(4));
            configuration.setRevocationEndpoint(result.getString(5));

            return configuration;

        } else {
            Log.e("DBHandler :: getOPConfiguration :: ", "Some error occured!");
            return null;
        }
    }

    public OIDCClient getOIDCClient(int sno) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor result = db.query("OIDC_CLIENT", new String[]{"SNO", "CLIENT_NAME", "CLIENT_ID", "CLIENT_SECRET", "RECENT_GENERATED_ID_TOKEN", "RECENT_GENERATED_ACCESS_TOKEN"},
                "SNO=?", new String[]{String.valueOf(sno)}, null, null, null);
        if (result != null && result.moveToFirst()) {
            Log.d("getConfiguration 1", result.getString(1));
            Log.d("getConfiguration 2", result.getString(2));
            OIDCClient client = new OIDCClient();
            client.setSno(result.getString(0));
            client.setClientName(result.getString(1));
            client.setClientId(result.getString(2));
            client.setClientSecret(result.getString(3));
            client.setRecentGeneratedIdToken(result.getString(4));
            client.setRecentGeneratedAccessToken(result.getString(5));
            return client;

        } else {
            Log.e("DBHandler :: getOIDCClient ::", "Some error occured!");
            return null;
        }
    }
}
