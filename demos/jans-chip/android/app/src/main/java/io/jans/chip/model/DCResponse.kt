package io.jans.chip.model

import com.google.gson.annotations.SerializedName

data class DCResponse (
    @SerializedName("client_id")
    val clientId: String? = null,

    @SerializedName("client_secret")
    val clientSecret: String? = null,

    @SerializedName("client_name")
    val clientName: String? = null,

    @SerializedName("authorization_challenge_endpoint")
    val authorizationChallengeEndpoint: String? = null,

    @SerializedName("end_session_endpoint")
    val endSessionEndpoint: String? = null,

)