package io.jans.chip.model.fido.config

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

data class Attestation(
    @ColumnInfo(name = "BASE_PATH")
    @SerializedName("base_path")
    var basePath: String?,

    @ColumnInfo(name = "OPTIONS_ENDPOINT")
    @SerializedName("options_endpoint")
    var optionsEndpoint: String?,

    @ColumnInfo(name = "RESULT_ENDPOINT")
    @SerializedName("result_endpoint")
    var resultEndpoint: String?,
)