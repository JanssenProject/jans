package com.example.fido2.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "OP_CONFIGURATION")
@Serializable
data class OPConfiguration(

    @PrimaryKey(autoGenerate = false)
    @SerialName("SNO")
    var sno: String = "",

    @ColumnInfo(name = "ISSUER")
    @SerialName("issuer")
    var issuer: String? = null,

    @ColumnInfo(name = "REGISTRATION_ENDPOINT")
    @SerialName("registration_endpoint")
    var registrationEndpoint: String? = null,

    @ColumnInfo(name = "TOKEN_ENDPOINT")
    @SerialName("token_endpoint")
    var tokenEndpoint: String?,

    @ColumnInfo(name = "USERINFO_ENDPOINT")
    @SerialName("userinfo_endpoint")
    var userinfoEndpoint: String? = null,

    @ColumnInfo(name = "AUTHORIZATION_CHALLENGE_ENDPOINT")
    @SerialName("authorization_challenge_endpoint")
    var authorizationChallengeEndpoint: String? = null,

    @ColumnInfo(name = "REVOCATION_ENDPOINT")
    @SerialName("revocation_endpoint")
    var revocationEndpoint: String? = null,

    @ColumnInfo(name = "BIOMETRIC_ENROLLED")
    @SerialName("biometric_enrolled")
    var biometricEnrolled: Boolean = false,

) {
    @Ignore
    var isSuccessful: Boolean = false

    @Ignore
    var errorMessage: String = ""

    @ColumnInfo(name = "FIDO_URL")
    var fidoUrl: String? = null
}