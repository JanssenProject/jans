package io.jans.chip.ui.screens.unauthenticated.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.google.gson.Gson
import com.spr.jetpack_loading.components.indicators.lineScaleIndicator.LineScaleIndicator
import com.spr.jetpack_loading.enums.PunchType
import io.jans.chip.AppAlertDialog
import io.jans.chip.common.AuthAdaptor
import io.jans.chip.common.LocalCredentialSelector
import io.jans.chip.model.LoginResponse
import io.jans.chip.model.TokenResponse
import io.jans.chip.model.UserInfoResponse
import io.jans.chip.model.fido.assertion.option.AssertionOptionResponse
import io.jans.chip.model.fido.assertion.result.AssertionResultRequest
import io.jans.chip.ui.common.customComposableViews.MediumTitleText
import io.jans.chip.ui.screens.unauthenticated.login.state.LoginUiEvent
import io.jans.chip.ui.theme.AppTheme
import io.jans.chip.ui.theme.Janschip1Theme
import io.jans.chip.utils.biometric.BiometricHelper
import io.jans.chip.viewmodel.MainViewModel
import io.jans.jans_chip.R
import io.jans.webauthn.models.PublicKeyCredentialSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onNavigateToRegistration: () -> Unit,
    onNavigateToAuthenticatedRoute: () -> Unit
) {
    val context = LocalContext.current as FragmentActivity
    val mainViewModel = MainViewModel.getInstance(context)
    val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val authAdaptor = AuthAdaptor(context)
    val shouldShowDialog = remember { mutableStateOf(false) }
    val dialogContent = remember { mutableStateOf("") }
    val creds: List<PublicKeyCredentialSource>? = authAdaptor.getAllCredentials()
    var loginState by remember {
        loginViewModel.loginState
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
        AppAlertDialog(
            shouldShowDialog = shouldShowDialog,
            content = dialogContent
        )
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
                LineScaleIndicator(
                    color = Color(0xFF134520),
                    rectCount = 5,
                    distanceOnXAxis = 30f,
                    lineHeight = 100,
                    animationDuration = 500,
                    minScale = 0.3f,
                    maxScale = 1.5f,
                    punchType = PunchType.RANDOM_PUNCH,
                    penThickness = 15f
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
                    modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Don't have an account?
                    Text(text = stringResource(id = R.string.do_not_have_account))

                    //Register
                    Text(
                        modifier = Modifier
                            .padding(start = AppTheme.dimens.paddingExtraSmall)
                            .clickable {
                                onNavigateToRegistration.invoke()
                            },
                        fontWeight = FontWeight.Bold,
                        text = stringResource(id = R.string.enrol),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Main card Content for Login
                ElevatedCard(
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
                            text = stringResource(id = R.string.janssen),
                            textAlign = TextAlign.Center
                        )

                        // Login Logo
                        AsyncImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(128.dp)
                                .padding(top = AppTheme.dimens.paddingSmall),
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(data = R.drawable.janssen_logo)
                                .crossfade(enable = true)
                                .scale(Scale.FILL)
                                .build(),
                            contentDescription = stringResource(id = R.string.use_saved_passkey)
                        )
                        // Heading Login
                        // Login Inputs Composable
                        if (creds.isNullOrEmpty()) {
                            MediumTitleText(
                                modifier = Modifier.padding(top = AppTheme.dimens.paddingLarge),
                                text = stringResource(id = R.string.no_passkey_enrolled)
                            )
                        } else {
                            MediumTitleText(
                                modifier = Modifier.padding(top = AppTheme.dimens.paddingLarge),
                                text = stringResource(id = R.string.use_saved_passkey)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(unbounded = true)

                        ) {
                            creds?.forEach { ele ->
                                LoginInputs(
                                    loginState = loginState,
                                    heading = ele.userDisplayName,
                                    subheading = ele.rpId,
                                    icon = R.drawable.passkey_icon,
                                    onContinueClick = {

                                        if (isBiometricAvailable) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                //show the loading screen
                                                loginState = loginState.copy(isLoading = true)
                                                //fetch FIDO configuration
                                                val fidoConfiguration =
                                                    async { mainViewModel.getFIDOConfiguration() }.await()
                                                if (fidoConfiguration?.isSuccessful == false) {
                                                    shouldShowDialog.value = true
                                                    dialogContent.value = fidoConfiguration.errorMessage.toString()
                                                    loginState = loginState.copy(isLoading = false)
                                                    return@launch
                                                }
                                                //call /assertion/option
                                                val assertionOptionResponse: AssertionOptionResponse? =
                                                    async { mainViewModel.assertionOption(ele.userDisplayName) }.await()
                                                if (assertionOptionResponse?.isSuccessful == false) {
                                                    shouldShowDialog.value = true
                                                    dialogContent.value = assertionOptionResponse.errorMessage.toString()
                                                    loginState = loginState.copy(isLoading = false)
                                                    return@launch
                                                }
                                                //get authenticator wrapper instance
                                                val authAdaptor = AuthAdaptor(context)
                                                //select public key credential
                                                val selectedPublicKeyCredentialSource = async {
                                                    authAdaptor.selectPublicKeyCredentialSource(
                                                        LocalCredentialSelector(),
                                                        assertionOptionResponse,
                                                        fidoConfiguration?.issuer,
                                                    )
                                                }.await()
                                                //Generate a signature object
                                                val signature = async {
                                                        authAdaptor.generateSignature(
                                                            selectedPublicKeyCredentialSource
                                                        )
                                                    }.await()
                                                //show biometric prompt
                                                BiometricHelper.authenticateUser(context,
                                                    signature!!,
                                                    onSuccess = { plainText ->
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            mainViewModel.setUsername(ele.userDisplayName)
                                                            //call authenticator authenticate method to get authenticatorData and assertion signature
                                                            val assertionResultRequest: AssertionResultRequest =
                                                                async {
                                                                    authAdaptor.authenticate(
                                                                        assertionOptionResponse!!,
                                                                        fidoConfiguration?.issuer,
                                                                        selectedPublicKeyCredentialSource
                                                                    )
                                                                }.await()
                                                            if (assertionResultRequest.isSuccessful == false) {
                                                                shouldShowDialog.value = true
                                                                dialogContent.value = assertionResultRequest.errorMessage.toString()
                                                                loginState = loginState.copy(isLoading = false)
                                                                return@launch
                                                            }

                                                            //process authentication to get authorization code
                                                            val loginResponse: LoginResponse? =
                                                                async {
                                                                    mainViewModel.processLogin(
                                                                        ele.userDisplayName,
                                                                        null,
                                                                        "authenticate",
                                                                        Gson().toJson(
                                                                            assertionResultRequest
                                                                        )
                                                                    )
                                                                }.await()

                                                            if (loginResponse?.isSuccessful == false) {
                                                                shouldShowDialog.value = true
                                                                dialogContent.value = loginResponse.errorMessage.toString()
                                                                loginState = loginState.copy(isLoading = false)
                                                                return@launch
                                                            }
                                                            //exchange token for code
                                                            val tokenResponse: TokenResponse? =
                                                                async {
                                                                    mainViewModel.getToken(
                                                                        loginResponse?.authorizationCode,
                                                                    )
                                                                }.await()
                                                            if (tokenResponse?.isSuccessful == false) {
                                                                shouldShowDialog.value = true
                                                                dialogContent.value = tokenResponse.errorMessage.toString()
                                                                loginState = loginState.copy(isLoading = false)
                                                                return@launch
                                                            }
                                                            //exchange user-info for token
                                                            val userInfoResponse: UserInfoResponse? =
                                                                async {
                                                                    mainViewModel.getUserInfo(
                                                                        tokenResponse?.accessToken
                                                                    )
                                                                }.await()
                                                            if (userInfoResponse != null) {
                                                                mainViewModel.setUserInfoResponse(
                                                                    userInfoResponse
                                                                )
                                                            }
                                                            if (userInfoResponse?.isSuccessful == false) {
                                                                shouldShowDialog.value = true
                                                                dialogContent.value = userInfoResponse.errorMessage.toString()
                                                                loginState = loginState.copy(isLoading = false)
                                                                return@launch
                                                            }
                                                        }
                                                        mainViewModel.mainState =
                                                            mainViewModel.mainState.copy(
                                                                assertionOptionSuccess = true
                                                            )
                                                        mainViewModel.mainState =
                                                            mainViewModel.mainState.copy(
                                                                assertionResultSuccess = true
                                                            )
                                                        loginViewModel.onUiEvent(
                                                            loginUiEvent = LoginUiEvent.Submit
                                                        )
                                                        loginState = loginState.copy(isLoading = false)
                                                        //Toast.makeText(context,"Biometric authentication successful!$plainText", Toast.LENGTH_SHORT).show()
                                                    })
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

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    Janschip1Theme {
        LoginScreen(
            onNavigateToRegistration = {},
            onNavigateToAuthenticatedRoute = {}
        )
    }
}