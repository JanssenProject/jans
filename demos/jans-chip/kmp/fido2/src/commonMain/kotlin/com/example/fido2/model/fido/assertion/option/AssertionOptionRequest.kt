package com.example.fido2.model.fido.assertion.option

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Serializable
data class AssertionOptionRequest(
    var username: String?,
    var userVerification: String?,
    var documentDomain: String?,
    var extensions: String?,
    var session_id: String?,
    var description: String?
) {
    fun toJson() = Json.encodeToString(serializer(), this)
}