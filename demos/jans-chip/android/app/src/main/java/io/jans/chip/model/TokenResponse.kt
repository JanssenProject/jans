package io.jans.chip.model

import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

data class TokenResponse (

    @SerializedName("access_token")
    var accessToken: String? = null,
    @SerializedName("id_token")
    var idToken: String? = null,
    @SerializedName("token_type")
    var tokenType: String? = null
){
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}