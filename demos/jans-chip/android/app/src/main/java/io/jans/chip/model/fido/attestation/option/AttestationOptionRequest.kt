package io.jans.chip.model.fido.attestation.option

data class AttestationOptionRequest(
    var username: String,

    var displayName: String,

    var attestation: String,
)