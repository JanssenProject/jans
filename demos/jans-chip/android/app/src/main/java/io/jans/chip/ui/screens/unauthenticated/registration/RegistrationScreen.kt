package io.jans.chip.ui.screens.unauthenticated.registration

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import com.spr.jetpack_loading.components.indicators.lineScaleIndicator.LineScaleIndicator
import com.spr.jetpack_loading.enums.PunchType
import io.jans.chip.AppAlertDialog
import io.jans.chip.common.AuthAdaptor
import io.jans.chip.model.LoginResponse
import io.jans.chip.model.TokenResponse
import io.jans.chip.model.UserInfoResponse
import io.jans.chip.model.fido.attestation.option.AttestationOptionResponse
import io.jans.chip.model.fido.attestation.result.AttestationResultRequest
import io.jans.chip.model.fido.config.FidoConfiguration
import io.jans.chip.model.fido.config.FidoConfigurationResponse
import io.jans.chip.ui.common.customComposableViews.MediumTitleText
import io.jans.chip.ui.common.customComposableViews.SmallClickableWithIconAndText
import io.jans.chip.ui.common.customComposableViews.TitleText
import io.jans.chip.ui.screens.unauthenticated.registration.state.RegistrationUiEvent
import io.jans.chip.ui.theme.AppTheme
import io.jans.chip.ui.theme.Janschip1Theme
import io.jans.chip.utils.biometric.BiometricHelper
import io.jans.chip.viewmodel.MainViewModel
import io.jans.jans_chip.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(
    registrationViewModel: RegistrationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAuthenticatedRoute: () -> Unit
) {
    val context = LocalContext.current as FragmentActivity
    val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val shouldShowDialog = remember { mutableStateOf(false) }
    val dialogContent = remember { mutableStateOf("") }

    val mainViewModel = MainViewModel.getInstance(context)
    var registrationState by remember {
        registrationViewModel.registrationState
    }

    if (registrationState.isRegistrationSuccessful) {
        LaunchedEffect(key1 = true) {
            onNavigateToAuthenticatedRoute.invoke()
        }
    } else {
        // Full Screen Content
        AppAlertDialog(
            shouldShowDialog = shouldShowDialog,
            content = dialogContent
        )
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
                    iconContentDescription = stringResource(id = R.string.navigate_back),
                    iconVector = Icons.Outlined.ArrowBack,
                    text = stringResource(id = R.string.back_to_login),
                    onClick = onNavigateBack
                )

                // Main card Content for Registration
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

                        // Logo
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
                            contentDescription = stringResource(id = R.string.enrol_account)
                        )
                        // Heading Registration
                        TitleText(
                            modifier = Modifier.padding(top = AppTheme.dimens.paddingLarge),
                            text = stringResource(id = R.string.enrol_account)
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
                                    mainViewModel.setUsername(inputString)
                                }
                            },

                            onPasswordChange = { inputString ->
                                val result = registrationViewModel.onUiEvent(
                                    registrationUiEvent = RegistrationUiEvent.PasswordChanged(
                                        inputValue = inputString
                                    )
                                )
                                if (result) {
                                    mainViewModel.setPassword(inputString)
                                }
                            },

                            onSubmit = {
                                //create authenticator instance
                                val authAdaptor = AuthAdaptor(context)
                                //check if selected  enrolled credential present in database
                                if(authAdaptor.isCredentialsPresent(mainViewModel.getUsername())) {
                                    shouldShowDialog.value = true
                                    dialogContent.value = "Username already enrolled!"
                                    return@RegistrationInputs
                                }
                                if (isBiometricAvailable) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        registrationState = registrationState.copy(isLoading = true)
                                        //authenticate to get authorization code
                                        val loginResponse: LoginResponse? = async {
                                            mainViewModel.processLogin(
                                                mainViewModel.getUsername(),
                                                mainViewModel.getPassword(),
                                                "enroll",
                                                null
                                            )
                                        }.await()

                                        if (loginResponse?.isSuccessful == false) {
                                            shouldShowDialog.value = true
                                            dialogContent.value = loginResponse.errorMessage.toString()
                                            registrationState = registrationState.copy(isLoading = false)
                                            return@launch
                                        }
                                        //exchange token for code
                                        val tokenResponse: TokenResponse? = async {
                                            mainViewModel.getToken(
                                                loginResponse?.authorizationCode,
                                            )
                                        }.await()
                                        if (tokenResponse?.isSuccessful == false) {
                                            shouldShowDialog.value = true
                                            dialogContent.value = tokenResponse.errorMessage.toString()

                                            registrationState = registrationState.copy(isLoading = false)
                                            return@launch
                                        }
                                        //exchange user-info for token
                                        val userInfoResponse: UserInfoResponse? =
                                            async { mainViewModel.getUserInfo(tokenResponse?.accessToken) }.await()
                                        if (userInfoResponse != null) {
                                            mainViewModel.setUserInfoResponse(
                                                userInfoResponse
                                            )
                                        }
                                        if (userInfoResponse?.isSuccessful == false) {
                                            shouldShowDialog.value = true
                                            dialogContent.value = userInfoResponse.errorMessage.toString()

                                            registrationState = registrationState.copy(isLoading = false)
                                            return@launch
                                        }
                                        //get fido configuration
                                        val fidoConfiguration: FidoConfiguration? =
                                            async { mainViewModel.getFIDOConfiguration() }.await()
                                        if (fidoConfiguration?.isSuccessful == false) {
                                            shouldShowDialog.value = true
                                            dialogContent.value = fidoConfiguration?.errorMessage.toString()

                                            registrationState = registrationState.copy(isLoading = false)
                                            return@launch
                                        }
                                        //call /attestation/option
                                        val attestationOptionResponse: AttestationOptionResponse? =
                                            async {
                                                mainViewModel.attestationOption(
                                                    mainViewModel.getUsername()
                                                )
                                            }.await()
                                        if (attestationOptionResponse?.isSuccessful == false) {
                                            shouldShowDialog.value = true
                                            dialogContent.value = attestationOptionResponse?.errorMessage.toString()

                                            registrationState = registrationState.copy(isLoading = false)
                                            return@launch
                                        }
                                        // Generate a new credential
                                        val publicKeyCredentialSource = async {
                                            authAdaptor.getPublicKeyCredentialSource(
                                                attestationOptionResponse,
                                                fidoConfiguration?.issuer
                                            )
                                        }.await()
                                        //Generate a signature object
                                        val signature =
                                            async {
                                                authAdaptor.generateSignature(
                                                    publicKeyCredentialSource
                                                )
                                            }.await()
                                        //show biometric prompt
                                        BiometricHelper.registerUserBiometrics(context,
                                            signature!!,
                                            onSuccess = { plainText ->
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    //call authenticator register method to get attestationObject
                                                    val attestationResultRequest: AttestationResultRequest? =
                                                        async {
                                                            authAdaptor.register(
                                                                attestationOptionResponse,
                                                                fidoConfiguration?.issuer,
                                                                publicKeyCredentialSource
                                                            )
                                                        }.await()
                                                    if (attestationResultRequest?.isSuccessful == false) {
                                                        shouldShowDialog.value = true
                                                        dialogContent.value = attestationResultRequest.errorMessage.toString()

                                                        registrationState = registrationState.copy(isLoading = false)
                                                        return@launch
                                                    }
                                                    //call /attestation/result
                                                    val attestationResultResponse = async {
                                                        mainViewModel.attestationResult(
                                                            attestationResultRequest
                                                        )
                                                    }.await()
                                                    if (attestationResultResponse?.isSuccessful == false) {
                                                        shouldShowDialog.value = true
                                                        dialogContent.value = attestationResultResponse.errorMessage.toString()

                                                        registrationState = registrationState.copy(isLoading = false)
                                                        return@launch
                                                    }
                                                    mainViewModel.mainState = mainViewModel.mainState.copy(attestationOptionSuccess = true)
                                                    mainViewModel.mainState = mainViewModel.mainState.copy(attestationResultSuccess = true)

                                                    registrationViewModel.onUiEvent(registrationUiEvent = RegistrationUiEvent.Submit)

                                                    registrationState = registrationState.copy(isLoading = false)
                                                }
                                            })
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Biometric authentication is not available!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    registrationState = registrationState.copy(isLoading = false)
                                }
                            }
                        )
                        Row(
                            modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Don't have an account?
                            Text(text = stringResource(id = R.string.already_have_login))

                            //Register
                            Text(
                                modifier = Modifier
                                    .padding(start = AppTheme.dimens.paddingExtraSmall)
                                    .clickable { onNavigateBack.invoke() },
                                fontWeight = FontWeight.Bold,
                                text = stringResource(id = R.string.login),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun PreviewRegistrationScreen() {
    Janschip1Theme {
        RegistrationScreen(onNavigateBack = {}, onNavigateToAuthenticatedRoute = {})
    }
}