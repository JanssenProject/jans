package io.jans.chip.model

import androidx.room.Ignore

class LogoutResponse() {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}