package io.jans.chip.model

import androidx.room.Ignore

class UserInfoResponse (
    var response: Any? = null
) {
    @Ignore
    var isSuccessful: Boolean? = true

    @Ignore
    var errorMessage: String? = null
}