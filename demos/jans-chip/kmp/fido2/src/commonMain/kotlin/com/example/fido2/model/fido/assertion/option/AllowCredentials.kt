package com.example.fido2.model.fido.assertion.option

import kotlinx.serialization.Serializable

@Serializable
data class AllowCredentials (
     val id: String,
     val type: String,
     val transports: List<String>,
)