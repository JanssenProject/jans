package com.example.fido2.model.fido.attestation.option

import kotlinx.serialization.Serializable

@Serializable
data class RP (
     var id: String,

     var name: String,
)