package io.jans.chip.ui.screens.unauthenticated.login

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import io.jans.chip.ElevatedCardExample
import io.jans.chip.ui.screens.unauthenticated.login.state.LoginState

@Composable
fun LoginInputs(
    loginState: LoginState,
    heading: String,
    subheading: String,
    @DrawableRes icon: Int,
    onContinueClick: () -> Unit,
) {

    // Login Inputs Section
    ElevatedCardExample(
        heading = heading,
        subheading = subheading,
        icon = icon,
        onButtonClick = onContinueClick
    )
}