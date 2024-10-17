package com.example.fido2.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "OIDC_CLIENT")
@Serializable
data class OIDCClient(
    @PrimaryKey(autoGenerate = false)
    @SerialName("SNO")
    var sno: String = "",

    @ColumnInfo(name = "CLIENT_NAME")
    @SerialName("CLIENT_NAME")
    var clientName: String? = null,

    @ColumnInfo(name = "CLIENT_ID")
    @SerialName("CLIENT_ID")
    var clientId: String? = null,

    @ColumnInfo(name = "CLIENT_SECRET")
    @SerialName("CLIENT_SECRET")
    var clientSecret: String? = null,

    @ColumnInfo(name = "RECENT_GENERATED_ID_TOKEN")
    @SerialName("RECENT_GENERATED_ID_TOKEN")
    var recentGeneratedIdToken: String? = null,

    @ColumnInfo(name = "RECENT_GENERATED_ACCESS_TOKEN")
    @SerialName("RECENT_GENERATED_ACCESS_TOKEN")
    var recentGeneratedAccessToken: String? = null,

    @SerialName("scope")
    var scope: String? = null
) {
    @Ignore
    var isSuccessful: Boolean = false

    @Ignore
    var errorMessage: String = ""
}