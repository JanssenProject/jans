package io.jans.chip.model.fido.assertion.result

import androidx.room.Ignore

class AssertionResultRequest {
     var id: String? = null

     var type: String? = null

     var rawId: String? = null

     var response: Response? = null

     @Ignore
     var isSuccessful: Boolean? = true

     @Ignore
     var errorMessage: String? = null
}