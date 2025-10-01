package io.jans.chip.model

import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

data class LoginResponse (
    @SerializedName("authorization_code")
    var authorizationCode: String?
) {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}