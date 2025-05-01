package com.example.androidapp.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.IOException

fun anyToJson(any: Any?): String {
    return Gson().toJson(any)
}

fun jsonToMapWithAnyType(jsonString: String): Map<String, Any> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    return Gson().fromJson(jsonString, type) ?: emptyMap()
}

fun jsonToMapWithStringType(jsonString: String): Map<String, String> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val map: Map<String, Any> = Gson().fromJson(jsonString, type) ?: emptyMap()

    return map.mapValues { it.value.toString() } // Convert all values to String
}

fun readJsonFromAssets(context: Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (ex: IOException) {
        ex.printStackTrace()
        null
    }
}

fun addFieldToJson(jsonString: String, key: String, value: Any): String {
    val jsonObject = JSONObject(jsonString)
    jsonObject.put(key, value)  // Add new field
    return jsonObject.toString()
}