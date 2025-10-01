package io.jans.chip.model.fido.assertion.result

data class ClientDataJSON(
    var type: String,

    val origin: String,

    val challenge: String,
)