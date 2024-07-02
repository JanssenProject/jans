package io.jans.chip.model

import com.google.gson.annotations.SerializedName

data class SSARegRequest (
    @SerializedName("client_name")
    var clientName: String? = null,

    @SerializedName("evidence")
    var evidence: String? = null,

    @SerializedName("jwks")
    var jwks: String? = null,

    @SerializedName("scope")
    var scope: String? = null,

    @SerializedName("response_types")
    var responseTypes: List<String?>,

    @SerializedName("grant_types")
    var grantTypes: List<String?>,

    @SerializedName("software_statement")
    var ssa: String? = null,

    @SerializedName("application_type")
    var applicationType: String? = null,

    @SerializedName("redirect_uris")
    var redirectUris: List<String?>,
)