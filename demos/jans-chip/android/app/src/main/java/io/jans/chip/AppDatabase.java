package io.jans.chip;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import io.jans.chip.dao.AppIntegrityDao;
import io.jans.chip.dao.OPConfigurationDao;
import io.jans.chip.dao.OidcClientDao;
import io.jans.chip.modal.OIDCClient;
import io.jans.chip.modal.OPConfiguration;
import io.jans.chip.modal.appIntegrity.AppIntegrityEntity;
import io.jans.chip.utils.AppConfig;

@Database(
        entities = {
                OIDCClient.class,
                OPConfiguration.class,
                AppIntegrityEntity.class
        },
        version = 1
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String LOG_TAG = AppDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = AppConfig.SQLITE_DB_NAME;
    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                Log.d(LOG_TAG, "Creating new database instance");
                instance = Room.databaseBuilder(context.getApplicationContext(),
                                AppDatabase.class, AppDatabase.DATABASE_NAME)
                        .allowMainThreadQueries()
                        .build();
            }
        }
        Log.d(LOG_TAG, "Getting the database instance");
        return instance;
    }

    public abstract OidcClientDao oidcClientDao();
    public abstract OPConfigurationDao opConfigurationDao();
    public abstract AppIntegrityDao appIntegrityDao();
}
