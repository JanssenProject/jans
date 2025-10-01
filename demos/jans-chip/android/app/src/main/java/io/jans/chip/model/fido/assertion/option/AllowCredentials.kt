package io.jans.chip.model.fido.assertion.option

data class AllowCredentials (
     val id: String,
     val type: String,
     val transports: List<String>,
)