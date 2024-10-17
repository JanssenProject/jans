package com.example.fido2.model

import androidx.room.Ignore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse (

    @SerialName("access_token")
    var accessToken: String? = null,
    @SerialName("id_token")
    var idToken: String? = null,
    @SerialName("token_type")
    var tokenType: String? = null
){
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}