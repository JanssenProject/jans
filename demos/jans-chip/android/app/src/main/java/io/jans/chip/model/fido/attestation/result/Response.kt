package io.jans.chip.model.fido.attestation.result

data class Response(
    var attestationObject: String,

    val clientDataJSON: String,
)