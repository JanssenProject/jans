package com.example.fido2.ui.screens.unauthenticated.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fido2.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.example.fido2.model.KtPublicKeyCredentialSource
import com.example.fido2.model.LoginResponse
import com.example.fido2.model.TokenResponse
import com.example.fido2.model.UserInfoResponse
import com.example.fido2.model.fido.assertion.option.AssertionOptionResponse
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.ui.common.customComposableViews.CustomAlertDialog
import com.example.fido2.ui.common.customComposableViews.CustomAlertDialogWithTwoButtons
import com.example.fido2.ui.common.customComposableViews.Direction
import com.example.fido2.ui.common.customComposableViews.SmallClickableWithIconAndText
import com.example.fido2.ui.common.customComposableViews.TitleText
import com.example.fido2.ui.screens.unauthenticated.login.state.LoginUiEvent
import com.example.fido2.ui.theme.AppTheme
import com.example.fido2.ui.theme.LightColors
import com.example.fido2.viewmodel.MainViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel { LoginViewModel() },
    onNavigateToRegistration: () -> Unit,
    viewModel: MainViewModel,
    onNavigateToAuthenticatedRoute: () -> Unit
) {
    val isBiometricAvailable = true // remember { BiometricHelper.isBiometricAvailable(context) }
    val shouldShowDialog = remember { mutableStateOf(false) }
    val shouldShowDeleteKeysDialog = remember { mutableStateOf(false) }
    val dialogContent = remember { mutableStateOf("") }
    val creds: List<KtPublicKeyCredentialSource>? = viewModel.getAllCredentials()
    var loginState by remember {
        loginViewModel.loginState
    }

    fun getHorizontalArrangement(): Arrangement.Horizontal {
        if (creds != null && creds.isEmpty()) {
            return Arrangement.End
        }

        return Arrangement.SpaceBetween
    }

    if (loginState.isLoginSuccessful) {
        /**
         * Navigate to Authenticated navigation route
         * once login is successful
         */
        LaunchedEffect(key1 = true) {
            onNavigateToAuthenticatedRoute.invoke()
        }
    } else {
        CustomAlertDialog(
            stringResource(Res.string.warning),
            dialogContent.value,
            stringResource(Res.string.ok),
            shouldShowDialog
        ) {
            // Action
        }
        CustomAlertDialogWithTwoButtons(
            stringResource(Res.string.delete_all_dialog_title),
            "",
            stringResource(Res.string.ok),
            shouldShowDeleteKeysDialog
        ) {
            viewModel.deleteAllKeys()
        }
        if (loginState.isLoading) {
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
                Text(
                    stringResource(Res.string.loading),
                    color = Color.Black
                )
            }
        } else {
            // Full Screen Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Register Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(LightColors.background),
                    horizontalArrangement = getHorizontalArrangement(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (creds?.isEmpty() == false) {
                        SmallClickableWithIconAndText(
                            modifier = Modifier
                                .height(44.dp)
                                .background(LightColors.background),
                            icon = Res.drawable.clear_logs_icon,
                            text = stringResource(Res.string.delete_all),
                            onClick = {
                                shouldShowDeleteKeysDialog.value = true
                            }
                        )
                    }
                    SmallClickableWithIconAndText(
                        modifier = Modifier
                            .height(44.dp)
                            .padding(end = AppTheme.dimens.paddingNormal)
                            .background(LightColors.background),
                        icon = Res.drawable.right,
                        text = stringResource(Res.string.enrol),
                        direction = Direction.RIGHT,
                        onClick = {
                            onNavigateToRegistration.invoke()
                        }
                    )
                }

                // Main card Content for Login
                Card(
                    modifier = Modifier
                        .padding(AppTheme.dimens.paddingNormal)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(AppTheme.dimens.paddingNormal)
                    ) {
                        // Heading Login
                        // Login Inputs Composable
                        if (creds.isNullOrEmpty()) {
                            TitleText(
                                modifier = Modifier.padding(top = AppTheme.dimens.paddingLarge),
                                text = stringResource(Res.string.no_passkey_enrolled)
                            )
                        } else {
                            TitleText(
                                modifier = Modifier.padding(vertical = AppTheme.dimens.paddingSmall),
                                text = stringResource(Res.string.use_saved_passkey)
                            )
                        }
                        Column(
                            modifier = Modifier
                        ) {
                            creds?.forEach { ele ->
                                LoginInputs(
                                    heading = ele.userDisplayName ?: "none",
                                    subheading = ele.rpId ?: "unknown",
                                    onContinueClick = {

                                        if (isBiometricAvailable) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                //show the loading screen
                                                loginState = loginState.copy(isLoading = true)
                                                //fetch FIDO configuration
                                                val fidoConfiguration =
                                                    async { viewModel.getFIDOConfiguration() }.await()
                                                println("fidoConfiguration --> $fidoConfiguration")
                                                if (fidoConfiguration?.isSuccessful == false) {
                                                    shouldShowDialog.value = true
                                                    dialogContent.value =
                                                        fidoConfiguration.errorMessage
                                                    loginState = loginState.copy(isLoading = false)
                                                    return@launch
                                                }
                                                //call /assertion/option
                                                val assertionOptionResponse: AssertionOptionResponse? =
                                                    async {
                                                        viewModel.assertionOption(
                                                            ele.userDisplayName ?: ""
                                                        )
                                                    }.await()
                                                if (assertionOptionResponse?.isSuccessful == false) {
                                                    shouldShowDialog.value = true
                                                    dialogContent.value =
                                                        assertionOptionResponse.errorMessage.toString()
                                                    loginState = loginState.copy(isLoading = false)
                                                    return@launch
                                                }
                                                //show biometric prompt
//                                                BiometricHelper.authenticateUser(context,
//                                                    signature!!,
//                                                    onSuccess = {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    viewModel.setUsername(
                                                        ele.userDisplayName ?: ""
                                                    )
                                                    //call authenticator authenticate method to get authenticatorData and assertion signature
                                                    val assertionResultRequest: AssertionResultRequest? =
                                                        async {
                                                            viewModel.authenticate(
                                                                assertionOptionResponse!!,
                                                                fidoConfiguration?.issuer
                                                            )
                                                        }.await()
                                                    if (assertionResultRequest?.isSuccessful == false) {
                                                        shouldShowDialog.value = true
                                                        dialogContent.value =
                                                            assertionResultRequest.errorMessage.toString()
                                                        loginState =
                                                            loginState.copy(isLoading = false)
                                                        return@launch
                                                    }

                                                    //process authentication to get authorization code
                                                    val loginResponse: LoginResponse? =
                                                        async {
                                                            viewModel.processLogin(
                                                                ele.userDisplayName ?: "",
                                                                null,
                                                                "authenticate",
                                                                assertionResultRequest?.toJson()
                                                            )
                                                        }.await()

                                                    if (loginResponse?.isSuccessful == false) {
                                                        loginState =
                                                            loginState.copy(isLoading = false, isLoginSuccessful = false)
                                                        shouldShowDialog.value = true
                                                        dialogContent.value =
                                                            loginResponse.errorMessage.toString()
                                                        return@launch
                                                    } else {
                                                        loginState =
                                                            loginState.copy(isLoading = false, isLoginSuccessful = true)
                                                    }
                                                    //exchange token for code
                                                    val tokenResponse: TokenResponse? =
                                                        async {
                                                            viewModel.getToken(
                                                                loginResponse?.authorizationCode,
                                                            )
                                                        }.await()
                                                    if (tokenResponse?.isSuccessful == false) {
                                                        shouldShowDialog.value = true
                                                        dialogContent.value =
                                                            tokenResponse.errorMessage.toString()
                                                        loginState =
                                                            loginState.copy(isLoading = false)
                                                        return@launch
                                                    }
                                                    //exchange user-info for token
                                                    val userInfoResponse: UserInfoResponse? =
                                                        async {
                                                            viewModel.getUserInfo(
                                                                tokenResponse?.accessToken
                                                            )
                                                        }.await()
                                                    if (userInfoResponse != null) {
                                                        viewModel.setUserInfoResponse(
                                                            userInfoResponse
                                                        )
                                                    }
                                                    if (userInfoResponse?.isSuccessful == false) {
                                                        shouldShowDialog.value = true
                                                        dialogContent.value =
                                                            userInfoResponse.errorMessage.toString()
                                                        loginState =
                                                            loginState.copy(isLoading = false)
                                                        return@launch
                                                    }
                                                }
                                                viewModel.mainState =
                                                    viewModel.mainState.copy(
                                                        assertionOptionSuccess = true
                                                    )
                                                viewModel.mainState =
                                                    viewModel.mainState.copy(
                                                        assertionResultSuccess = true
                                                    )
                                                loginViewModel.onUiEvent(
                                                    loginUiEvent = LoginUiEvent.Submit
                                                )
                                                loginState = loginState.copy(isLoading = false)
                                                //Toast.makeText(context,"Biometric authentication successful!$plainText", Toast.LENGTH_SHORT).show()
//                                                    })
                                            }
                                            //end
                                        } else {
                                            shouldShowDialog.value = true
                                            dialogContent.value = "Biometric authentication is not available!"
                                            loginState = loginState.copy(isLoading = false)
                                        }
                                    })
                            }
                        }
                    }
                }
            }
        }

    }

}