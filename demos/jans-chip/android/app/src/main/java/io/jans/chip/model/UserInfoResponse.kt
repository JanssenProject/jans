package io.jans.chip.model

import androidx.room.Ignore

class UserInfoResponse (
    var response: Any? = null
) {
    @Ignore
    var isSuccessful: Boolean? = false

    @Ignore
    var errorMessage: String? = null
}