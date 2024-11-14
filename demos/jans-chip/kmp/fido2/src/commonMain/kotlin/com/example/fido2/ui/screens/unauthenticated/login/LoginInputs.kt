package com.example.fido2.ui.screens.unauthenticated.login

import androidx.compose.runtime.Composable
import com.example.fido2.Res
import com.example.fido2.janssen_logo
import com.example.fido2.ui.common.customComposableViews.ElevatedCardExample
import com.example.fido2.ui.screens.unauthenticated.login.state.LoginState

@Composable
fun LoginInputs(
    loginState: LoginState,
    heading: String,
    subheading: String,
    onContinueClick: () -> Unit,
) {

    // Login Inputs Section
    ElevatedCardExample(
        heading = heading,
        subheading = subheading,
        icon = Res.drawable.janssen_logo,
        onButtonClick = onContinueClick
    )
}