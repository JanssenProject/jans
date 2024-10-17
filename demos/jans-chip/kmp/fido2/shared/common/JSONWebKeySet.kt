package com.example.fido2.shared.common

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper

class JSONWebKeySet {
    private var keys: MutableList<Map<String?, *>?> = mutableListOf()
    fun addKey(key: Map<String?, *>?) {
        keys.add(key)
    }
    fun toJsonString(): String? {
        val mapper = ObjectMapper()
        return try {
            mapper.writeValueAsString(this)
        } catch (e: JsonProcessingException) {
            println(e.localizedMessage)
            return null
        }
    }
}