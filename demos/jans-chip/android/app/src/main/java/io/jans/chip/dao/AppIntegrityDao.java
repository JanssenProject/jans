package io.jans.chip.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.jans.chip.modal.appIntegrity.AppIntegrityEntity;

@Dao
public interface AppIntegrityDao {
    @Insert
    void insert(AppIntegrityEntity appIntegrityEntity);

    @Update
    void update(AppIntegrityEntity appIntegrityEntity);

    @Query("SELECT * FROM APP_INTEGRITY")
    List<AppIntegrityEntity> getAll();

    @Query("DELETE FROM APP_INTEGRITY")
    void deleteAll();
}
