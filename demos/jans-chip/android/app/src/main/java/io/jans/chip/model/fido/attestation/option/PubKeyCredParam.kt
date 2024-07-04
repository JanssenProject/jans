package io.jans.chip.model.fido.attestation.option

data class PubKeyCredParam(
    var type: String,

    val alg: Long = 0
)