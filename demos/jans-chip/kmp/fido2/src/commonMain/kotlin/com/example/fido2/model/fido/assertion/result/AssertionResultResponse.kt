package com.example.fido2.model.fido.assertion.result

import androidx.room.Ignore

class AssertionResultResponse {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}