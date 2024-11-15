package com.example.fido2.ui.screens.unauthenticated.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fido2.*
import com.example.fido2.modules.Dispatcher
import com.example.fido2.ui.common.customComposableViews.MediumTitleText
import com.example.fido2.ui.common.customComposableViews.SmallClickableWithIconAndText
import com.example.fido2.ui.common.customComposableViews.TitleText
import com.example.fido2.ui.screens.unauthenticated.registration.state.RegistrationUiEvent
import com.example.fido2.ui.theme.AppTheme
import com.example.fido2.ui.common.QrScannerView
import com.example.fido2.ui.common.customComposableViews.AppAlertDialog
import com.example.fido2.ui.screens.unauthenticated.registration.state.RegistrationState
import com.example.fido2.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegistrationScreen(
    registrationViewModel: RegistrationViewModel = viewModel { RegistrationViewModel() },
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel,
    onNavigateToAuthenticatedRoute: () -> Unit,
    onQRCodeScanClose: () -> Unit
) {
    val shouldShowDialog = remember { viewModel.shouldShowDialog }
    val shouldQRCodeScanning = remember { viewModel.shouldQRCodeScanning }
    val isRegistrationSuccessful = remember { viewModel.isRegistrationSuccessful }
    val dialogContent = remember { viewModel.dialogContent }
    var registrationState by remember {
        registrationViewModel.registrationState
    }

    if (isRegistrationSuccessful.value) {
        LaunchedEffect(key1 = true) {
            onNavigateToAuthenticatedRoute.invoke()
        }
    } else {
        // Full Screen Content
        AppAlertDialog(
            shouldShowDialog = shouldShowDialog,
            content = dialogContent
        )
        if (shouldQRCodeScanning.value) {
            QrScannerView(onQRCodeScanClose)
            return
        }
        if (registrationState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(Res.string.loading), color = Color.Black)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {

                // Back Button Icon
                SmallClickableWithIconAndText(
                    modifier = Modifier
                        .padding(horizontal = AppTheme.dimens.paddingLarge)
                        .padding(top = AppTheme.dimens.paddingLarge),
                    iconContentDescription = stringResource(Res.string.navigate_back),
                    iconVector = Icons.Outlined.ArrowDropDown,
                    text = stringResource(Res.string.back_to_login),
                    onClick = onNavigateBack
                )

                // Main card Content for Registration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppTheme.dimens.paddingLarge)
                ) {

                    Column(
                        modifier = Modifier
                            .padding(horizontal = AppTheme.dimens.paddingLarge)
                            .padding(bottom = AppTheme.dimens.paddingExtraLarge)
                    ) {
                        // Heading Jetpack Compose
                        MediumTitleText(
                            modifier = Modifier
                                .padding(top = AppTheme.dimens.paddingLarge)
                                .fillMaxWidth(),
                            text = stringResource(Res.string.janssen),
                            textAlign = TextAlign.Center
                        )

                        // Heading Registration
                        TitleText(
                            modifier = Modifier.padding(top = AppTheme.dimens.paddingLarge),
                            text = stringResource(Res.string.enrol_account)
                        )

                        /**
                         * Registration Inputs Composable
                         */
                        RegistrationInputs(
                            registrationState = registrationState,

                            onEmailIdChange = { inputString ->
                                val result = registrationViewModel.onUiEvent(
                                    registrationUiEvent = RegistrationUiEvent.UsernameChanged(
                                        inputValue = inputString
                                    )
                                )
                                if (result) {
                                    viewModel.setUsername(inputString)
                                }
                            },

                            onPasswordChange = { inputString ->
                                val result = registrationViewModel.onUiEvent(
                                    registrationUiEvent = RegistrationUiEvent.PasswordChanged(
                                        inputValue = inputString
                                    )
                                )
                                if (result) {
                                    viewModel.setPassword(inputString)
                                }
                            },

                            onSubmit = {
                                registrationViewModel.onUiEvent(
                                    registrationUiEvent = RegistrationUiEvent.ValidateInputs
                                )
                                if (registrationState.isValidationSuccessful) {
                                    onSubmitAction(viewModel = viewModel, registrationViewModel = registrationViewModel, shouldShowDialog = shouldShowDialog, dialogContent = dialogContent)
                                }
                            },

                            onQrCodeSubmit = {
                                shouldQRCodeScanning.value = true
                            }
                        )
                    }
                }
            }
        }
    }
}

fun onSubmitAction(viewModel: MainViewModel, registrationViewModel: RegistrationViewModel, shouldShowDialog: MutableState<Boolean>, dialogContent: MutableState<String>) {
//    CoroutineScope(Dispatchers.Main).launch {
        viewModel.loadAppTasks(shouldShowDialog, dialogContent) { success ->
            if (success) {
                viewModel.proceedRegistration(
                    registrationViewModel,
                    shouldShowDialog,
                    dialogContent
                )
            }
        }
//    }
}