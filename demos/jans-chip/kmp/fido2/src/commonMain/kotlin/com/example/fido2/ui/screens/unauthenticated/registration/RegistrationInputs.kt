package com.example.fido2.ui.screens.unauthenticated.registration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.fido2.*
import com.example.fido2.ui.common.customComposableViews.CustomTextField
import com.example.fido2.ui.common.customComposableViews.LoginButton
import com.example.fido2.ui.common.customComposableViews.NormalButton
import com.example.fido2.ui.screens.unauthenticated.registration.state.RegistrationState
import com.example.fido2.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegistrationInputs(
    registrationState: RegistrationState,
    onEmailIdChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onQrCodeSubmit: () -> Unit,
) {
    // Login Inputs Section
    Column(modifier = Modifier.fillMaxWidth()) {
        // Email ID
        CustomTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = registrationState.username,
            onValueChange = onEmailIdChange,
            label = stringResource(Res.string.username),
            isError = registrationState.errorState.emailIdErrorState.hasError,
            errorText = stringResource(registrationState.errorState.emailIdErrorState.errorMessageStringResource),
            imeAction = ImeAction.Next
        )
        Spacer(modifier = Modifier.height(20.dp))
        // Password
        CustomTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = registrationState.password,
            onValueChange = onPasswordChange,
            label = stringResource(Res.string.registration_password_label),
            isError = registrationState.errorState.passwordErrorState.hasError,
            errorText = stringResource(registrationState.errorState.passwordErrorState.errorMessageStringResource),
            imeAction = ImeAction.Next
        )

        // Registration Submit Button
        LoginButton(
            modifier = Modifier
                .padding(top = AppTheme.dimens.paddingExtraLarge)
                .fillMaxWidth(),
            text = stringResource(Res.string.login_button_text),
            onClick = onSubmit
        )

        // Scam Qr code Button
        NormalButton(
            modifier = Modifier
                .padding(top = AppTheme.dimens.paddingExtraLarge)
                .fillMaxWidth(),
            text = "Scan QR code",
            onClick = onQrCodeSubmit
        )
    }
}