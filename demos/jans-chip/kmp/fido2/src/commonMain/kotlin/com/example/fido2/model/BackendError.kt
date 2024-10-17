package com.example.fido2.model

import kotlinx.serialization.Serializable

@Serializable
data class BackendError (
    var reason: String? = null,
    var error_description: String? = null,
    var error: String? = null
)