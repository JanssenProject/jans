package com.example.androidapp.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.IOException

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

fun toJsonString(obj: Any): String {
    return Gson().toJson(obj)
}