package io.jans.chip.viewmodel

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.jans.chip.model.LoginResponse
import io.jans.chip.model.LogoutResponse
import io.jans.chip.model.OIDCClient
import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.TokenResponse
import io.jans.chip.model.UserInfoResponse
import io.jans.chip.model.appIntegrity.AppIntegrityEntity
import io.jans.chip.model.appIntegrity.AppIntegrityResponse
import io.jans.chip.model.fido.assertion.option.AssertionOptionResponse
import io.jans.chip.model.fido.assertion.result.AssertionResultRequest
import io.jans.chip.model.fido.assertion.result.AssertionResultResponse
import io.jans.chip.model.fido.attestation.option.AttestationOptionResponse
import io.jans.chip.model.fido.attestation.result.AttestationResultRequest
import io.jans.chip.model.fido.attestation.result.AttestationResultResponse
import io.jans.chip.model.fido.config.FidoConfiguration
import io.jans.chip.repository.DCRRepository
import io.jans.chip.repository.FidoAssertionRepository
import io.jans.chip.repository.FidoAttestationRepository
import io.jans.chip.repository.FidoConfigurationRepository
import io.jans.chip.repository.LoginResponseRepository
import io.jans.chip.repository.LogoutRepository
import io.jans.chip.repository.OPConfigurationRepository
import io.jans.chip.repository.PlayIntegrityRepository
import io.jans.chip.repository.TokenResponseRepository
import io.jans.chip.repository.UserInfoResponseRepository
import io.jans.chip.viewmodel.state.MainState

class MainViewModel : ViewModel() {

    private val TAG = "MainViewModel"
    private lateinit var context: Context
    private lateinit var opConfigUrl: String
    private lateinit var fidoConfigUrl: String
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var userInfoResponse: UserInfoResponse

    var mainState by mutableStateOf(MainState())

    private lateinit var opConfigurationRepository: OPConfigurationRepository
    private lateinit var dcrRepository: DCRRepository
    private lateinit var fidoConfigurationRepository: FidoConfigurationRepository
    private lateinit var loginResponseRepository: LoginResponseRepository
    private lateinit var tokenResponseRepository: TokenResponseRepository
    private lateinit var userInfoResponseRepository: UserInfoResponseRepository
    private lateinit var logoutRepository: LogoutRepository
    private lateinit var fidoAttestationRepository: FidoAttestationRepository
    private lateinit var fidoAssertionRepository: FidoAssertionRepository
    private lateinit var playIntegrityRepository: PlayIntegrityRepository

    companion object {

        private lateinit var instance: MainViewModel

        @MainThread
        fun getInstance(activityContext: Context): MainViewModel {
            instance = if (::instance.isInitialized) {
                instance
            } else {
                instance = MainViewModel()
                instance.initModel(activityContext)
                instance
            }
            return instance
        }
    }

    fun initModel(activityContext: Context) {
        context = activityContext
        opConfigurationRepository = OPConfigurationRepository(context)
        dcrRepository = DCRRepository(context)
        fidoConfigurationRepository = FidoConfigurationRepository(context)
        loginResponseRepository = LoginResponseRepository(context)
        tokenResponseRepository = TokenResponseRepository(context)
        userInfoResponseRepository = UserInfoResponseRepository(context)
        logoutRepository = LogoutRepository(context)
        fidoAttestationRepository = FidoAttestationRepository(context)
        fidoAssertionRepository = FidoAssertionRepository(context)
        playIntegrityRepository = PlayIntegrityRepository(context)
    }

    fun setOpConfigUrl(opConfigUrl: String) {
        this.opConfigUrl = opConfigUrl
        Log.d(TAG, opConfigUrl)
    }

    fun getOpConfigUrl(): String {
        return opConfigUrl
    }

    fun getFidoConfigUrl(): String {
        return fidoConfigUrl
    }

    fun setFidoConfigUrl(fidoConfigUrl: String) {
        this.fidoConfigUrl = fidoConfigUrl
        Log.d(TAG, fidoConfigUrl)
    }

    fun setUsername(username: String) {
        this.username = username
        Log.d(TAG, username)
    }

    fun getUsername(): String {
        if (!(this::username.isInitialized)) {
            return ""
        }
        return username
    }

    fun setPassword(password: String) {
        this.password = password
    }

    fun getPassword(): String {
        if (!(this::password.isInitialized)) {
            return ""
        }
        return password
    }

    fun setUserInfoResponse(userInfoResponse: UserInfoResponse) {
        this.userInfoResponse = userInfoResponse
    }

    fun getUserInfoResponse(): UserInfoResponse {
        return userInfoResponse
    }

    suspend fun attestationOption(username: String): AttestationOptionResponse? {
        val attestationOptionResponse: AttestationOptionResponse? =
            fidoAttestationRepository.attestationOption(username)
        if (attestationOptionResponse?.isSuccessful == true) {
            mainState = mainState.copy(attestationOptionSuccess = true)
        }
        return attestationOptionResponse
    }

    suspend fun attestationResult(attestationResultRequest: AttestationResultRequest?): AttestationResultResponse? {
        val attestationResultResponse: AttestationResultResponse? =
            fidoAttestationRepository.attestationResult(attestationResultRequest)
        if (attestationResultResponse?.isSuccessful == true) {
            mainState = mainState.copy(attestationResultSuccess = true)
        }
        return attestationResultResponse
    }

    suspend fun checkAppIntegrity(): AppIntegrityResponse? {
        return playIntegrityRepository.checkAppIntegrity()
    }

    suspend fun checkAppIntegrityFromDatabase(): String? {
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
        return fidoAssertionRepository.assertionOption(username)
    }

    suspend fun assertionResult(assertionResultRequest: AssertionResultRequest?): AssertionResultResponse {
        val assertionResultResponse: AssertionResultResponse =
            fidoAssertionRepository.assertionResult(assertionResultRequest)
        if (assertionResultResponse.isSuccessful == true) {
            mainState = mainState.copy(attestationOptionSuccess = true)
            mainState = mainState.copy(attestationResultSuccess = true)
        }
        return assertionResultResponse
    }

    suspend fun processLogin(
        usernameText: String,
        passwordText: String?,
        authMethod: String,
        assertionResultRequest: String?
    ): LoginResponse? {
        //userIsAuthenticated = true
        return loginResponseRepository.processLogin(
            usernameText,
            passwordText,
            authMethod,
            assertionResultRequest
        )
    }

    suspend fun getToken(
        authorizationCode: String?,
    ): TokenResponse? {
        return tokenResponseRepository.getToken(authorizationCode)
    }

    suspend fun logout(): LogoutResponse {
        val logoutResponse: LogoutResponse = logoutRepository.logout()
        if (logoutResponse.isSuccessful == true) {
            mainState = mainState.copy(isUserIsAuthenticated = false)
        }
        return logoutResponse
    }

    suspend fun getOIDCClient(): OIDCClient? {
        val oidcClient: OIDCClient? = dcrRepository.getOIDCClient()
        if (oidcClient?.isSuccessful == true) {
            mainState = mainState.copy(isClientRegistered = true)
        }
        return oidcClient
    }

    suspend fun getOPConfiguration(): OPConfiguration? {
        val opConfiguration: OPConfiguration? = opConfigurationRepository.getOPConfiguration()
        if (opConfiguration?.isSuccessful == true) {
            mainState = mainState.copy(opConfigurationPresent = true)
        }
        return opConfiguration
    }

    suspend fun getFIDOConfiguration(): FidoConfiguration? {
        val fidoConfiguration: FidoConfiguration? = fidoConfigurationRepository.getFidoConfig()
        if (fidoConfiguration?.isSuccessful == true) {
            mainState = mainState.copy(fidoConfigurationPresent = true)
        }
        return fidoConfiguration
    }

    suspend fun getUserInfo(accessToken: String?): UserInfoResponse? {
        val userInfoResponse: UserInfoResponse? =  userInfoResponseRepository.getUserInfo(accessToken)
        if (userInfoResponse?.isSuccessful == true) {
            mainState = mainState.copy(isUserIsAuthenticated = true)
        }
        return userInfoResponse
    }
}