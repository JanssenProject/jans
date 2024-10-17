package com.example.fido2.model.fido.attestation.option

import kotlinx.serialization.Serializable

@Serializable
data class User(
    var id: String,

    val name: String,

    val displayName: String,
)