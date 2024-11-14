package com.example.fido2.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "APP_SETTINGS")
@Serializable
data class AppSettings(
    @PrimaryKey(autoGenerate = false)
    @SerialName("ServerUrl")
    var serverUrl: String = "",
)