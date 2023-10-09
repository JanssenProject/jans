package io.jans.chip.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.jans.chip.modal.OPConfiguration;
@Dao
public interface OPConfigurationDao {
    @Insert
    void insert(OPConfiguration opConfiguration);

    @Update
    void update(OPConfiguration opConfiguration);

    @Query("SELECT * FROM OP_CONFIGURATION")
    List<OPConfiguration> getAll();

    @Query("DELETE FROM OP_CONFIGURATION")
    void deleteAll();
}
