package com.example.fido2.model.fido.attestation.option

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticatorSelection(
    var authenticatorAttachment: String,

    var requireResidentKey: Boolean,

    var userVerification: String

//    var residentKey: String,
)