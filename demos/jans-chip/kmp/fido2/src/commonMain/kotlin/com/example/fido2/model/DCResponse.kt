package com.example.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DCResponse (
    @SerialName("client_id")
    val clientId: String? = null,

    @SerialName("client_secret")
    val clientSecret: String? = null,

    @SerialName("client_name")
    val clientName: String? = null,

    @SerialName("authorization_challenge_endpoint")
    val authorizationChallengeEndpoint: String? = null,

    @SerialName("end_session_endpoint")
    val endSessionEndpoint: String? = null
)