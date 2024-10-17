package com.example.fido2.ui.screens.unauthenticated.registration.state

import com.example.fido2.*
import com.example.fido2.ui.common.state.ErrorState

val emailEmptyErrorState = ErrorState(
    hasError = true,
    errorMessageStringResource = Res.string.registration_error_msg_empty_username
)

val passwordEmptyErrorState = ErrorState(
    hasError = true,
    errorMessageStringResource = Res.string.registration_error_msg_empty_password
)

val passwordMismatchErrorState = ErrorState(
    hasError = true,
    errorMessageStringResource = Res.string.registration_error_msg_password_mismatch
)
