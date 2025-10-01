package io.jans.chip.model.fido.attestation.result

data class ClientDataJSON(
    var type: String,

     val origin: String,

     val challenge: String,
)