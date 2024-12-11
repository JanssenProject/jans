package com.example.fido2.ui.screens.unauthenticated.login

import androidx.compose.runtime.Composable
import com.example.fido2.Res
import com.example.fido2.default_sg_icon
import com.example.fido2.ui.common.customComposableViews.ElevatedCardExample

@Composable
fun LoginInputs(
    heading: String,
    subheading: String,
    onContinueClick: () -> Unit,
) {

    // Login Inputs Section
    ElevatedCardExample(
        heading = heading,
        subheading = subheading,
        icon = Res.drawable.default_sg_icon,
        onButtonClick = onContinueClick
    )
}