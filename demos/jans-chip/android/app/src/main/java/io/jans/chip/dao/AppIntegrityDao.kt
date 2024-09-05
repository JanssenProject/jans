package io.jans.chip.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.jans.chip.model.appIntegrity.AppIntegrityEntity

@Dao
interface AppIntegrityDao {
    @Insert
    fun insert(appIntegrityEntity: AppIntegrityEntity?)

    @Update
    fun update(appIntegrityEntity: AppIntegrityEntity?)

    @Query("SELECT * FROM APP_INTEGRITY")
    fun getAll(): List<AppIntegrityEntity>

    @Query("DELETE FROM APP_INTEGRITY")
    fun deleteAll()
}