package com.example.fido2.ui.screens.unauthenticated.login.state

import com.example.fido2.ui.common.state.ErrorState
import com.example.fido2.*
import com.example.fido2.login_error_msg_empty_email_mobile

val emailOrMobileEmptyErrorState = ErrorState(
    hasError = true,
    errorMessageStringResource = Res.string.login_error_msg_empty_email_mobile
)

val passwordEmptyErrorState = ErrorState(
    hasError = true,
    errorMessageStringResource = Res.string.login_error_msg_empty_password
)