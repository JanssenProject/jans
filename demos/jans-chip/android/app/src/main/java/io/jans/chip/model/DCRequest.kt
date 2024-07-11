package io.jans.chip.model

import com.google.gson.annotations.SerializedName

data class DCRequest (
    @SerializedName("issuer")
    var issuer: String? = null,

    @SerializedName("redirect_uris")
    var redirectUris: List<String?>,

    @SerializedName("scope")
    var scope: String? = null,

    @SerializedName("response_types")
    var responseTypes: List<String?>,

    @SerializedName("post_logout_redirect_uris")
    var postLogoutRedirectUris: List<String?>,

    @SerializedName("grant_types")
    var grantTypes: List<String?>,

    @SerializedName("application_type")
    var applicationType: String? = null,

    @SerializedName("client_name")
    var clientName: String? = null,

    @SerializedName("token_endpoint_auth_method")
    var tokenEndpointAuthMethod: String? = null,

    @SerializedName("evidence")
    var evidence: String? = null,

    @SerializedName("jwks")
    var jwks: String? = null,
)