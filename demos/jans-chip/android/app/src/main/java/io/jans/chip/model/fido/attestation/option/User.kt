package io.jans.chip.model.fido.attestation.option

data class User(
    var id: String,

    val name: String,

    val displayName: String,
)