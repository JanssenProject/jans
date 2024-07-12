package io.jans.chip.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "OP_CONFIGURATION")
data class OPConfiguration(
    @NonNull
    @PrimaryKey(autoGenerate = false)
    @SerializedName("SNO")
    var sno: String,
    @ColumnInfo(name = "ISSUER")
    @SerializedName("issuer")
    var issuer: String?,
    @ColumnInfo(name = "REGISTRATION_ENDPOINT")
    @SerializedName("registration_endpoint")
    var registrationEndpoint: String?,
    @ColumnInfo(name = "TOKEN_ENDPOINT")
    @SerializedName("token_endpoint")
    var tokenEndpoint: String?,
    @ColumnInfo(name = "USERINFO_ENDPOINT")
    @SerializedName("userinfo_endpoint")
    var userinfoEndpoint: String?,
    @ColumnInfo(name = "AUTHORIZATION_CHALLENGE_ENDPOINT")
    @SerializedName("authorization_challenge_endpoint")
    var authorizationChallengeEndpoint: String?,
    @ColumnInfo(name = "REVOCATION_ENDPOINT")
    @SerializedName("revocation_endpoint")
    var revocationEndpoint: String?,
    @ColumnInfo(name = "BIOMETRIC_ENROLLED")
    @SerializedName("biometric_enrolled")
    var biometricEnrolled: Boolean = false,

) {
    @Ignore
    var isSuccessful: Boolean = false

    @Ignore
    var errorMessage: String = ""

    @ColumnInfo(name = "FIDO_URL")
    var fidoUrl: String? = null
}