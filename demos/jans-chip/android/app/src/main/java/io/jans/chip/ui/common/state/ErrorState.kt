package io.jans.chip.ui.common.state

import androidx.annotation.StringRes
import io.jans.jans_chip.R

/**
 * Error state holding values for error ui
 */
data class ErrorState(
    val hasError: Boolean = false,
    @StringRes val errorMessageStringResource: Int = R.string.empty_string
)