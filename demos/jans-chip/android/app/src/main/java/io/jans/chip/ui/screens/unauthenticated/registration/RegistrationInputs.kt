package io.jans.chip.ui.screens.unauthenticated.registration

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import io.jans.chip.ui.common.customComposableViews.EmailTextField
import io.jans.chip.ui.common.customComposableViews.NormalButton
import io.jans.chip.ui.common.customComposableViews.PasswordTextField
import io.jans.chip.ui.screens.unauthenticated.registration.state.RegistrationState
import io.jans.chip.ui.theme.AppTheme
import io.jans.jans_chip.R

@Composable
fun RegistrationInputs(
    registrationState: RegistrationState,
    onEmailIdChange: (String) -> Unit,
    //onMobileNumberChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    //onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
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
            label = stringResource(id = R.string.username),
            isError = registrationState.errorState.emailIdErrorState.hasError,
            errorText = stringResource(id = registrationState.errorState.emailIdErrorState.errorMessageStringResource),
            imeAction = ImeAction.Next
        )

        // Password
        PasswordTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimens.paddingLarge),
            value = registrationState.password,
            onValueChange = onPasswordChange,
            label = stringResource(id = R.string.registration_password_label),
            isError = registrationState.errorState.passwordErrorState.hasError,
            errorText = stringResource(id = registrationState.errorState.passwordErrorState.errorMessageStringResource),
            imeAction = ImeAction.Next
        )

        // Registration Submit Button
        NormalButton(
            modifier = Modifier.padding(top = AppTheme.dimens.paddingExtraLarge),
            text = stringResource(id = R.string.login_button_text),
            onClick = onSubmit
        )


    }
}