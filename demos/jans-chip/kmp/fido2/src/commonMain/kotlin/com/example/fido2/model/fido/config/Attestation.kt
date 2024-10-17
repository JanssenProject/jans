package com.example.fido2.model.fido.config

import androidx.room.ColumnInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attestation(
    @ColumnInfo(name = "BASE_PATH")
    @SerialName("base_path")
    var basePath: String?,

    @ColumnInfo(name = "OPTIONS_ENDPOINT")
    @SerialName("options_endpoint")
    var optionsEndpoint: String?,

    @ColumnInfo(name = "RESULT_ENDPOINT")
    @SerialName("result_endpoint")
    var resultEndpoint: String?,
)