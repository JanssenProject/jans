package com.example.fido2.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.fido2.model.OIDCClient

@Dao
interface OidcClientDao {

    @Insert
    suspend fun insert(oidcClient: OIDCClient)

    @Update
    suspend fun update(oidcClient: OIDCClient)

    @Query("SELECT * FROM OIDC_CLIENT")
    suspend fun getAll(): List<OIDCClient>

    @Query("DELETE FROM OIDC_CLIENT")
    suspend fun deleteAll()
}