package com.example.fido2.model.appIntegrity

data class RequestDetails(
    var requestPackageName: String? = null,
    var timestampMillis: String? = null,
    var nonce: String? = null,
)