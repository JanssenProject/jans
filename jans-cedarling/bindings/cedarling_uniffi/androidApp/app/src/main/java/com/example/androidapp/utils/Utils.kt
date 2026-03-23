package com.example.androidapp.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uniffi.cedarling_uniffi.Diagnostics
import uniffi.cedarling_uniffi.TokenInput
import java.io.IOException

/**
 * Deserialize a JSON object string into a map with every value converted to a string.
 *
 * @param jsonString The JSON object as a string.
 * @return `Map<String, String>` mapping keys to their stringified values; returns an empty map if deserialization produces `null` or the input does not represent a JSON object.
 */
fun jsonToMapWithStringType(jsonString: String): Map<String, String> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val map: Map<String, Any> = Gson().fromJson(jsonString, type) ?: emptyMap()

    return map.mapValues { it.value.toString() } // Convert all values to String
}

/**
 * Parses a JSON string into a list of TokenInput objects.
 *
 * @param jsonString A JSON-formatted string representing an array of TokenInput objects.
 * @return The parsed `List<TokenInput>`, or an empty list if parsing fails or the JSON does not represent a list.
 */
fun jsonToTokenInputList(jsonString: String): List<TokenInput> {
    return try {
        val type = object : TypeToken<List<TokenInput>>() {}.type
        Gson().fromJson<List<TokenInput>>(jsonString, type) ?: emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

/**
 * Reads a text file from the application's assets and returns its contents.
 *
 * @param context The Android Context used to access the assets.
 * @param fileName The name of the asset file to read.
 * @return The file contents as a `String`, or `null` if the asset cannot be read. 
 */
fun readJsonFromAssets(context: Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    }
}

/**
 * Loads an asset file by name and returns its raw bytes.
 *
 * @param fileName The asset file name or path within the app's assets directory.
 * @return A `ByteArray` containing the file contents, or `null` if the asset cannot be read. 
 */
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

/**
 * Serialize a `Diagnostics` object to its JSON string representation.
 *
 * @param obj The `Diagnostics` instance to serialize, or `null`.
 * @return A JSON string representing `obj`; the string `null` if `obj` is `null`.
 */
fun toJsonString(obj: Diagnostics?): String {
    return Gson().toJson(obj)
}