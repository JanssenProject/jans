package com.example.fido2.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.fido2.authAdaptor.AuthenticationProvider
import com.example.fido2.model.KtPublicKeyCredentialSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.example.fido2.model.LoginResponse
import com.example.fido2.model.LogoutResponse
import com.example.fido2.model.OIDCClient
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.TokenResponse
import com.example.fido2.model.UserInfoResponse
import com.example.fido2.model.appIntegrity.AppIntegrityEntity
import com.example.fido2.model.appIntegrity.AppIntegrityResponse
import com.example.fido2.model.fido.assertion.option.AssertionOptionResponse
import com.example.fido2.model.fido.assertion.result.AssertionResultRequest
import com.example.fido2.model.fido.assertion.result.AssertionResultResponse
import com.example.fido2.model.fido.attestation.option.AttestationOptionResponse
import com.example.fido2.model.fido.attestation.result.AttestationResultRequest
import com.example.fido2.model.fido.attestation.result.AttestationResultResponse
import com.example.fido2.model.fido.config.FidoConfiguration
import com.example.fido2.repository.PlayIntegrityRepository
import com.example.fido2.usecase.DCRClientUseCase
import com.example.fido2.usecase.FidoAssertionUseCase
import com.example.fido2.usecase.FidoAttestationUseCase
import com.example.fido2.usecase.GetFidoConfigurationUseCase
import com.example.fido2.usecase.GetOPConfigurationUseCase
import com.example.fido2.usecase.GetTokenUseCase
import com.example.fido2.usecase.GetUserInfoUseCase
import com.example.fido2.usecase.LoginUseCase
import com.example.fido2.usecase.LogoutUseCase
import com.example.fido2.viewmodel.state.MainState
import kotlinx.coroutines.IO

class MainViewModel(
    private val getOPConfigurationUseCase: GetOPConfigurationUseCase,
    private val getFidoConfigurationUseCase: GetFidoConfigurationUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val tokenUseCase: GetTokenUseCase,
    private val getUserInfoCase: GetUserInfoUseCase,
    private val fidoAssertionUseCase: FidoAssertionUseCase,
    private val fidoAttestationUseCase: FidoAttestationUseCase,
    private val dcrClientUseCase: DCRClientUseCase,
    private val authenticator: AuthenticationProvider
) {

    private val TAG = "MainViewModel"
    private var username: String = ""
    private var password: String = ""

    private var userInfoResponse: UserInfoResponse? = null
    private lateinit var playIntegrityRepository: PlayIntegrityRepository

    var mainState by mutableStateOf(MainState())

    val isBiometricAvailable = true // remember { BiometricHelper.isBiometricAvailable(context) }

    suspend fun getOIDCClient(): OIDCClient? {
        return dcrClientUseCase.invoke().getOrNull()
    }

    fun getAllCredentials(): List<KtPublicKeyCredentialSource>? {
        return authenticator.getAllCredentials()
    }

    suspend fun authenticate(
        assertionOptionResponse: AssertionOptionResponse,
        origin: String?
    ): AssertionResultRequest? {
        return authenticator.authenticate(assertionOptionResponse, origin)
    }

    fun setUsername(username: String) {
        this.username = username
        println(username)
    }

    fun getUsername(): String {
        return username
    }

    fun setPassword(password: String) {
        this.password = password
    }

    fun getPassword(): String {
        return password
    }

    fun setUserInfoResponse(userInfoResponse: UserInfoResponse) {
        this.userInfoResponse = userInfoResponse
        mainState = mainState.copy(isUserIsAuthenticated = true)
    }

    fun getUserInfoResponse(): UserInfoResponse? {
        return userInfoResponse
    }

    fun loadAppTasks(shouldShowDialog: MutableState<Boolean>, dialogContent: MutableState<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            //get openid configuration
            try {
                mainState = mainState.copy(isLoading = true)
                val opConfiguration: OPConfiguration? = async { getOPConfiguration() }.await()
                println("opConfiguration --> $opConfiguration")
                if (opConfiguration?.isSuccessful == false) {
                    mainState = mainState.copy(errorInLoading = true)
                    mainState = mainState.copy(loadingErrorMessage = "Error in fetching OP Configuration")
                }

                //get FIDO configuration
                val fidoConfiguration: FidoConfiguration? = async { getFIDOConfiguration() }.await()
                if (fidoConfiguration?.isSuccessful == false) {
                    mainState = mainState.copy(errorInLoading = true)
                    mainState = mainState.copy(loadingErrorMessage = "Error in fetching FIDO Configuration")
                }

                //check OIDC client
                val oidcClient: OIDCClient? = async { getOIDCClient() }.await()
                if (oidcClient?.isSuccessful == false) {
                    mainState = mainState.copy(errorInLoading = true)
                    mainState = mainState.copy(loadingErrorMessage = "Error in registering OIDC Client")
                } else {
                    mainState = mainState.copy(isClientRegistered = true)
                }
                //setting user-info
                val userInfoResponse: UserInfoResponse? = getUserInfo(oidcClient?.recentGeneratedAccessToken)
                if (userInfoResponse?.isSuccessful == true) {
                    setUserInfoResponse(userInfoResponse)
                }
                //checking app integrity
                val appIntegrityEntity: String? = async { checkAppIntegrityFromDatabase() }.await()
                if (appIntegrityEntity == null) {
                    val appIntegrityResponse: AppIntegrityResponse? = async { checkAppIntegrity() }.await()
                    if (appIntegrityResponse != null) {
                        mainState = mainState.copy(errorInLoading = true)
                        mainState = mainState.copy(
                            loadingErrorMessage = appIntegrityResponse.appIntegrity?.appRecognitionVerdict
                                ?: "Unable to fetch App Integrity from Google Play Integrity"
                        )
                    }
                } else {
                    mainState = mainState.copy(errorInLoading = true)
                    mainState = mainState.copy(loadingErrorMessage = "App Integrity: $appIntegrityEntity")
                }
                shouldShowDialog.value = mainState.errorInLoading
                dialogContent.value = mainState.loadingErrorMessage

                mainState = mainState.copy(isLoading = false)
            } catch (e: Exception) {
                //catching exception
                mainState = mainState.copy(isLoading = false)
                mainState = mainState.copy(errorInLoading = true)
                mainState = mainState.copy(loadingErrorMessage = "Error in loading app: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun proceedRegistration(callback: (shouldShowDialogValue: Boolean, dialogContentValue: String?, success: Boolean) -> Unit) {
        if (getUsername().isEmpty()) {
            callback.invoke(true, "Username cannot be empty", false)
            return
        }
        if (getPassword().isEmpty()) {
            callback.invoke(true, "Password cannot be empty", false)
            return
        }
        //create authenticator instance
        val authAdaptor = authenticator
        //check if selected  enrolled credential present in database
        if(authAdaptor?.isCredentialsPresent(getUsername()) == true) {
            callback.invoke(true, "Username already enrolled", false)
            return
        }
        if (isBiometricAvailable) {
            CoroutineScope(Dispatchers.Main).launch {
                //authenticate to get authorization code
                val loginResponse: LoginResponse? = async {
                    processLogin(
                        getUsername(),
                        getPassword(),
                        "enroll",
                        null
                    )
                }.await()
                println("loginResponse ---> $loginResponse")
                if (loginResponse?.isSuccessful == false) {
                    callback.invoke(true, loginResponse.errorMessage.toString(), false)
                    return@launch
                }
                //exchange token for code
                val tokenResponse: TokenResponse? = async {
                    getToken(
                        loginResponse?.authorizationCode,
                    )
                }.await()
                println("tokenResponse --> $tokenResponse")
                if (tokenResponse?.isSuccessful == false) {
                    callback.invoke(true, tokenResponse.errorMessage.toString(), false)
                    return@launch
                }
                //exchange user-info for token
                val userInfoResponse: UserInfoResponse? =
                    async { getUserInfo(tokenResponse?.accessToken) }.await()
                if (userInfoResponse != null) {
                    setUserInfoResponse(
                        userInfoResponse
                    )
                }
                if (userInfoResponse?.isSuccessful == false) {
                    callback.invoke(true, userInfoResponse.errorMessage.toString(), false)
                    return@launch
                }
                //get fido configuration
                val fidoConfiguration: FidoConfiguration? = async { getFIDOConfiguration() }.await()
                if (fidoConfiguration?.isSuccessful == false) {
                    callback.invoke(true, fidoConfiguration?.errorMessage.toString(), false)
                    return@launch
                }
                //call /attestation/option
                val attestationOptionResponse: AttestationOptionResponse? =
                    async {
                        attestationOption(getUsername())
                    }.await()
                println("attestationOptionResponse --> $attestationOptionResponse")
                if (attestationOptionResponse?.isSuccessful == false) {
                    callback.invoke(true, attestationOptionResponse?.errorMessage.toString(), false)
                    return@launch
                }
                //show biometric prompt
//                                        BiometricHelper.registerUserBiometrics(context,
//                                            signature!!,
//                                            onSuccess = { plainText ->
                CoroutineScope(Dispatchers.Main).launch {
                    //call authenticator register method to get attestationObject
                    val attestationResultRequest: AttestationResultRequest? =
                        async {
                            authAdaptor?.register(
                                attestationOptionResponse,
                                fidoConfiguration?.issuer
                            )
                        }.await()
                    if (attestationResultRequest?.isSuccessful == false) {
                        callback.invoke(true, attestationResultRequest.errorMessage.toString(), false)
                        return@launch
                    }
                    //call /attestation/result
                    attestationResultRequest?.let { attestationResultRequestObject ->
                        val attestationResultResponse = async {
                            attestationResult(
                                attestationResultRequestObject
                            )
                        }.await()
                        if (attestationResultResponse?.isSuccessful == false) {
                            callback.invoke(true, attestationResultRequestObject.errorMessage.toString(), false)
                            return@launch
                        }
                        mainState = mainState.copy(attestationOptionSuccess = true)
                        mainState = mainState.copy(attestationResultSuccess = true)

                        callback.invoke(false, null, true)
                    }
                }
//              })
            }
        } else {
            callback.invoke(true, "Biometric authentication is not available!", false)
        }
    }

    suspend fun attestationOption(username: String): AttestationOptionResponse? {
        var result: AttestationOptionResponse? = null
        fidoAttestationUseCase.invoke(username).onSuccess {
            result = it
        }.onFailure {
            it.printStackTrace()
        }
        return result
    }

    suspend fun attestationResult(attestationResultRequest: AttestationResultRequest): AttestationResultResponse? {
        return fidoAttestationUseCase.invoke(attestationResultRequest).getOrNull()
    }

    suspend fun checkAppIntegrity(): AppIntegrityResponse? {
        return null // playIntegrityRepository.checkAppIntegrity()
    }

    suspend fun checkAppIntegrityFromDatabase(): String? {
//        if (playIntegrityRepository == null) {
            return "no data found"
//        }
        val appIntegrityEntity: AppIntegrityEntity =
            playIntegrityRepository.getAppIntegrityEntityInDatabase()
                ?: return null

        if (appIntegrityEntity.error != null) {
            return appIntegrityEntity.error
        }
        if (appIntegrityEntity.appIntegrity != null) {
            return appIntegrityEntity.appLicensingVerdict
        }
        if (appIntegrityEntity.deviceIntegrity != null) {
            return appIntegrityEntity.deviceIntegrity
        }
        return null
    }

    suspend fun assertionOption(username: String): AssertionOptionResponse? {
        return fidoAssertionUseCase.invoke(username).getOrNull()
    }

    suspend fun assertionResult(assertionResultRequest: AssertionResultRequest): AssertionResultResponse? {
        return fidoAssertionUseCase.invoke(assertionResultRequest).getOrNull()
    }

    suspend fun processLogin(
        usernameText: String,
        passwordText: String?,
        authMethod: String,
        assertionResultRequest: String?
    ): LoginResponse? {
        return loginUseCase.invoke(
                    usernameText,
                    passwordText,
                    authMethod,
                    assertionResultRequest
                ).getOrNull()
    }

    suspend fun getToken(authorizationCode: String?): TokenResponse? {
        return tokenUseCase.invoke(authorizationCode).getOrNull()
    }

    suspend fun logout(): LogoutResponse? {
        mainState = mainState.copy(isUserIsAuthenticated = false)
        return logoutUseCase.invoke().getOrNull()
    }

    suspend fun getOPConfiguration(): OPConfiguration? {
        return getOPConfigurationUseCase.invoke().getOrNull()
    }

    suspend fun getFIDOConfiguration(): FidoConfiguration? {
        return getFidoConfigurationUseCase.invoke().getOrNull()
    }

    suspend fun getUserInfo(accessToken: String?): UserInfoResponse? {
        val oidcClient = getOIDCClient()
        return getUserInfoCase.invoke(accessToken, oidcClient?.clientId ?: "", oidcClient?.clientSecret ?: "").getOrNull()
    }
}