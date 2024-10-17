package com.example.fido2.model.appIntegrity

data class DeviceIntegrity (
    var deviceRecognitionVerdict: List<String>? = null
) {
    fun commasSeparatedString(): String? {
        return deviceRecognitionVerdict?.joinToString { ", " }
    }
}