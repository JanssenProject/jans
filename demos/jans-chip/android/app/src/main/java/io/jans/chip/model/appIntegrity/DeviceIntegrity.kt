package io.jans.chip.model.appIntegrity

data class DeviceIntegrity (
    var deviceRecognitionVerdict: List<String>? = null
) {
    fun commasSeparatedString(): String? {
        return if (deviceRecognitionVerdict != null) {
            java.lang.String.join(",", deviceRecognitionVerdict)
        } else null
    }
}