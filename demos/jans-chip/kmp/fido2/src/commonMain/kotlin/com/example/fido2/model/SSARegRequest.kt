package com.example.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class SSARegRequest (
    @SerialName("client_name")
    var clientName: String? = null,

    @SerialName("evidence")
    var evidence: String? = null,

    @SerialName("jwks")
    var jwks: String? = null,

    @SerialName("scope")
    var scope: String? = null,

    @SerialName("response_types")
    var responseTypes: List<String?>,

    @SerialName("grant_types")
    var grantTypes: List<String?>,

    @SerialName("software_statement")
    var ssa: String? = null,

    @SerialName("application_type")
    var applicationType: String? = null,

    @SerialName("redirect_uris")
    var redirectUris: List<String?>,
) {
    fun toJson() = Json.encodeToString(serializer(),this)
}