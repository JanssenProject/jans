package com.example.fido2.ui.screens.unauthenticated.registration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.example.fido2.*
import com.example.fido2.ui.common.customComposableViews.EmailTextField
import com.example.fido2.ui.common.customComposableViews.NormalButton
import com.example.fido2.ui.common.customComposableViews.PasswordTextField
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
        EmailTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimens.paddingLarge),
            value = registrationState.username,
            onValueChange = onEmailIdChange,
            label = stringResource(Res.string.username),
            isError = registrationState.errorState.emailIdErrorState.hasError,
            errorText = stringResource(registrationState.errorState.emailIdErrorState.errorMessageStringResource),
            imeAction = ImeAction.Next
        )

        // Password
        PasswordTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimens.paddingLarge),
            value = registrationState.password,
            onValueChange = onPasswordChange,
            label = stringResource(Res.string.registration_password_label),
            isError = registrationState.errorState.passwordErrorState.hasError,
            errorText = stringResource(registrationState.errorState.passwordErrorState.errorMessageStringResource),
            imeAction = ImeAction.Next
        )

        // Registration Submit Button
        NormalButton(
            modifier = Modifier.padding(top = AppTheme.dimens.paddingExtraLarge),
            text = stringResource(Res.string.login_button_text),
            onClick = onSubmit
        )

        // Scam Qr code Button
        NormalButton(
            modifier = Modifier.padding(top = AppTheme.dimens.paddingExtraLarge),
            text = "Scan QR code",
            onClick = onQrCodeSubmit
        )
    }
}