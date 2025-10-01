package io.jans.chip.model

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "OIDC_CLIENT")
data class OIDCClient(
    @NonNull
    @PrimaryKey(autoGenerate = false)
    @SerializedName("SNO")
    var sno: String,

    @ColumnInfo(name = "CLIENT_NAME")
    @SerializedName("CLIENT_NAME")
    var clientName: String?,

    @ColumnInfo(name = "CLIENT_ID")
    @SerializedName("CLIENT_ID")
    var clientId: String?,

    @ColumnInfo(name = "CLIENT_SECRET")
    @SerializedName("CLIENT_SECRET")
    var clientSecret: String?,

    @ColumnInfo(name = "RECENT_GENERATED_ID_TOKEN")
    @SerializedName("RECENT_GENERATED_ID_TOKEN")
    var recentGeneratedIdToken: String?,

    @ColumnInfo(name = "RECENT_GENERATED_ACCESS_TOKEN")
    @SerializedName("RECENT_GENERATED_ACCESS_TOKEN")
    var recentGeneratedAccessToken: String?,

    @SerializedName("scope")
    var scope: String?,


) {
    @Ignore
    var isSuccessful: Boolean = false

    @Ignore
    var errorMessage: String = ""
}