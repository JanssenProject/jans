package com.example.fido2.model.appIntegrity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName

@Entity(tableName = "APP_INTEGRITY")
data class AppIntegrityEntity(

    @PrimaryKey
    @SerialName("SNO")
    var sno: String,

    @ColumnInfo(name = "APP_INTEGRITY")
    var appIntegrity: String? = null,

    @ColumnInfo(name = "DEVICE_INTEGRITY")
    var deviceIntegrity: String? = null,

    @ColumnInfo(name = "APP_LICENSING_VERDICT")
    var appLicensingVerdict: String? = null,

    @ColumnInfo(name = "REQUEST_PACKAGE_NAME")
    var requestPackageName: String? = null,

    @ColumnInfo(name = "NONCE")
    var nonce: String? = null,

    @ColumnInfo(name = "ERROR")
    var error: String? = null,
)