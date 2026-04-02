package com.example.androidapp.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uniffi.cedarling_uniffi.Diagnostics
import uniffi.cedarling_uniffi.TokenInput
import java.io.IOException

fun jsonToMapWithStringType(jsonString: String): Map<String, String> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val map: Map<String, Any> = Gson().fromJson(jsonString, type) ?: emptyMap()

    return map.mapValues { it.value.toString() } // Convert all values to String
}

fun jsonToTokenInputList(jsonString: String): List<TokenInput> {
    return try {
        val type = object : TypeToken<List<TokenInput>>() {}.type
        Gson().fromJson<List<TokenInput>>(jsonString, type) ?: emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

fun readJsonFromAssets(context: Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    }
}

fun readZipFromAssets(context: Context, fileName: String): ByteArray? {
    return try {
        context.assets.open(fileName).use { inputStream ->
            inputStream.readBytes()
        }
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    }
}

fun toJsonString(obj: Diagnostics?): String {
    return Gson().toJson(obj)
}