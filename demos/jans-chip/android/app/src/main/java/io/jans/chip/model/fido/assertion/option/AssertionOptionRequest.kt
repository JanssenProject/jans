package io.jans.chip.model.fido.assertion.option

data class AssertionOptionRequest(
    var username: String?,
    var userVerification: String?,
    var documentDomain: String?,
    var extensions: String?,
    var session_id: String?,
    var description: String?,
)