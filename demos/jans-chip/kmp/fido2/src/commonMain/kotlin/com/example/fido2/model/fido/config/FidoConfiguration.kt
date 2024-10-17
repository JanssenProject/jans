package com.example.fido2.model.fido.config

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "FIDO_CONFIGURATION")
@Serializable
data class FidoConfiguration(
    @PrimaryKey
    @SerialName("SNO")
    var sno: String = "",

    @ColumnInfo(name = "ISSUER")
    @SerialName("issuer")
    var issuer: String? = null,

    @ColumnInfo(name = "ATTESTATION_OPTIONS_ENDPOINT")
    var attestationOptionsEndpoint: String? = null,

    @ColumnInfo(name = "ATTESTATION_RESULT_ENDPOINT")
    var attestationResultEndpoint: String? = null,

    @ColumnInfo(name = "ASSERTION_OPTIONS_ENDPOINT")
    var assertionOptionsEndpoint: String? = null,

    @ColumnInfo(name = "ASSERTION_RESULT_ENDPOINT")
    var assertionResultEndpoint: String? = null,
) {
    @Ignore
    var isSuccessful: Boolean = false

    @Ignore
    var errorMessage: String = ""
}