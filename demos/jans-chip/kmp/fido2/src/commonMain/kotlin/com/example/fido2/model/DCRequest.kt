package com.example.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DCRequest (
    @SerialName("issuer")
    var issuer: String? = null,

    @SerialName("redirect_uris")
    var redirectUris: List<String?>,

    @SerialName("scope")
    var scope: String? = null,

    @SerialName("response_types")
    var responseTypes: List<String?>,

    @SerialName("post_logout_redirect_uris")
    var postLogoutRedirectUris: List<String?>,

    @SerialName("grant_types")
    var grantTypes: List<String?>,

    @SerialName("application_type")
    var applicationType: String? = null,

    @SerialName("client_name")
    var clientName: String? = null,

    @SerialName("token_endpoint_auth_method")
    var tokenEndpointAuthMethod: String? = null,

    @SerialName("evidence")
    var evidence: String? = null,

    @SerialName("jwks")
    var jwks: String? = null,
)