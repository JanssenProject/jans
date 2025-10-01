package io.jans.chip.model.fido.attestation.option

data class AuthenticatorSelection(
    var authenticatorAttachment: String,

    var requireResidentKey: Boolean,

    var userVerification: String,

    var residentKey: String,
)