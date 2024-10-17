package com.example.fido2.ui.common.state

import com.example.fido2.*
import org.jetbrains.compose.resources.StringResource

/**
 * Error state holding values for error ui
 */
data class ErrorState(
    val hasError: Boolean = false,
    val errorMessageStringResource: StringResource = Res.string.empty_string
)