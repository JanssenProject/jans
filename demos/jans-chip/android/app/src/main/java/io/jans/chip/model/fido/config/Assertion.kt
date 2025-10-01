package io.jans.chip.model.fido.config

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

data class Assertion(
    @ColumnInfo(name = "BASE_PATH")
    @SerializedName("base_path")
    var basePath: String?,

    @ColumnInfo(name = "OPTIONS_ENDPOINT")
    @SerializedName("options_endpoint")
    var optionsEndpoint: String?,

    @ColumnInfo(name = "OPTIONS_GENERATE_ENDPOINT")
    @SerializedName("options_generate_endpoint")
    var optionsGenerateEndpoint: String?,

    @ColumnInfo(name = "RESULT_ENDPOINT")
    @SerializedName("result_endpoint")
    var resultEndpoint: String?,
)