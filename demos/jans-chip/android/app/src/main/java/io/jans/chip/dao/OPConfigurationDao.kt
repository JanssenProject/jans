package io.jans.chip.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.jans.chip.model.OPConfiguration

@Dao
interface OPConfigurationDao {
    @Insert
    fun insert(opConfiguration: OPConfiguration?)

    @Update
    fun update(opConfiguration: OPConfiguration?)

    @Query("SELECT * FROM OP_CONFIGURATION")
    fun getAll(): List<OPConfiguration>

    @Query("DELETE FROM OP_CONFIGURATION")
    fun deleteAll()
}
