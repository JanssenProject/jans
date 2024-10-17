package com.example.fido2.model.fido.assertion.result

import androidx.room.Ignore
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
class AssertionResultRequest {
     var id: String? = null

     var type: String? = null

     var rawId: String? = null

     @Contextual
     var response: Response? = null

     @Ignore
     var isSuccessful: Boolean? = true

     @Ignore
     var errorMessage: String? = null

     fun toJson() = Json.encodeToString(serializer(),this)
}