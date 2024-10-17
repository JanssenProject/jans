package com.example.fido2.model.fido.attestation.option

import kotlinx.serialization.Serializable

@Serializable
data class PubKeyCredParam(
    var type: String,

    val alg: Long = 0
)