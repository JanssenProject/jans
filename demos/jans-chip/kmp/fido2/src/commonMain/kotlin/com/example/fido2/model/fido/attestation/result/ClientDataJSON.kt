package com.example.fido2.model.fido.attestation.result

import kotlinx.serialization.Serializable

@Serializable
data class ClientDataJSON(
    var type: String,

     val origin: String,

     val challenge: String,
)