package io.jans.chip.model.appIntegrity

data class RequestDetails(
    var requestPackageName: String? = null,
    var timestampMillis: String? = null,
    var nonce: String? = null,
)