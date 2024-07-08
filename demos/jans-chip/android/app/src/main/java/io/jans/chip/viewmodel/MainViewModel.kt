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
import io.jans.chip.model.fido.config.FidoConfigurationResponse
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

class MainViewModel : ViewModel() {

    private val TAG = "MainViewModel"
    private lateinit var context: Context
    private lateinit var opConfigUrl: String
    private lateinit var fidoConfigUrl: String
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var userInfoResponse: UserInfoResponse

    var opConfigurationPresent by mutableStateOf(false)
    var fidoConfigurationPresent by mutableStateOf(false)
    var attestationOptionSuccess by mutableStateOf(false)
    var attestationOptionResponse by mutableStateOf(false)
    var clientRegistered by mutableStateOf(false)
    var userIsAuthenticated by mutableStateOf(false)
    var assertionOptionResponse by mutableStateOf(false)
    var errorInLoading by mutableStateOf(false)
    var loadingErrorMessage by mutableStateOf("")


    lateinit var opConfigurationRepository: OPConfigurationRepository
    lateinit var dcrRepository: DCRRepository
    lateinit var fidoConfigurationRepository: FidoConfigurationRepository
    lateinit var loginResponseRepository: LoginResponseRepository
    lateinit var tokenResponseRepository: TokenResponseRepository
    lateinit var userInfoResponseRepository: UserInfoResponseRepository
    lateinit var logoutRepository: LogoutRepository
    lateinit var fidoAttestationRepository: FidoAttestationRepository
    lateinit var fidoAssertionRepository: FidoAssertionRepository
    lateinit var playIntegrityRepository: PlayIntegrityRepository

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

    suspend fun fetchOPConfiguration(): OPConfiguration? {

        val opConfiguration: OPConfiguration? =
            opConfigurationRepository.fetchOPConfiguration(opConfigUrl)
        if (opConfiguration?.isSuccessful == true) {
            opConfigurationPresent = true
        }
        return opConfiguration
    }

    suspend fun doDCR(scopeText: String): OIDCClient? {
        val oidcClient: OIDCClient? = dcrRepository.doDCR(scopeText)
        clientRegistered = true
        return oidcClient
    }

    suspend fun doDCRUsingSSA(ssa: String, scopeText: String): OIDCClient? {
        val oidcClient: OIDCClient? = dcrRepository.doDCRUsingSSA(ssa, scopeText)
        clientRegistered = true
        return oidcClient
    }

    suspend fun fetchFidoConfiguration(): FidoConfigurationResponse? {
        if (!(this::fidoConfigUrl.isInitialized)) {
            val opConfiguration: OPConfiguration? =
                opConfigurationRepository.getOPConfigurationInDatabase()
            opConfiguration?.fidoUrl?.let { setFidoConfigUrl(it) }
        }

        val fidoConfigurationResponse: FidoConfigurationResponse? =
            fidoConfigurationRepository.fetchFidoConfiguration(fidoConfigUrl)
        if (fidoConfigurationResponse?.isSuccessful == true) {
            fidoConfigurationPresent = true
        }
        return fidoConfigurationResponse
    }

    suspend fun getFidoConfigInDatabase(): FidoConfiguration? {
        return fidoConfigurationRepository.getFidoConfigInDatabase()
    }

    suspend fun deleteOPConfigurationInDatabase() {
        opConfigurationRepository.deleteOPConfigurationInDatabase()
        opConfigurationPresent = false
    }

    suspend fun deleteClientInDatabase() {
        dcrRepository.deleteClientInDatabase()
        clientRegistered = false
        userIsAuthenticated = false
    }

    suspend fun deleteFidoConfigurationInDatabase() {
        fidoConfigurationRepository.deleteFidoConfigurationInDatabase()
        fidoConfigurationPresent = false
        attestationOptionSuccess = false
        attestationOptionResponse = false
    }

    suspend fun attestationOption(username: String): AttestationOptionResponse? {
        val attestationOptionResponse: AttestationOptionResponse? =
            fidoAttestationRepository.attestationOption(username)
        if (attestationOptionResponse?.isSuccessful == true) {
            attestationOptionSuccess = true
        }
        return attestationOptionResponse
    }

    suspend fun attestationResult(attestationResultRequest: AttestationResultRequest?): AttestationResultResponse? {
        val attestationResultResponse: AttestationResultResponse? =
            fidoAttestationRepository.attestationResult(attestationResultRequest)
        if (attestationResultResponse?.isSuccessful == true) {
            attestationOptionResponse = true
        }
        return attestationResultResponse
    }

    suspend fun checkAppIntegrity(): AppIntegrityResponse? {
        return playIntegrityRepository.checkAppIntegrity()
    }

    suspend fun checkAppIntegrityFromDatabase(): String? {
        val appIntegrityEntity: AppIntegrityEntity = playIntegrityRepository.getAppIntegrityEntityInDatabase()
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
            assertionOptionResponse = true
        }
        return assertionResultResponse
    }

    suspend fun processlogin(
        usernameText: String,
        passwordText: String?,
        authMethod: String,
        assertionResultRequest: String?
    ): LoginResponse? {
        //userIsAuthenticated = true
        return loginResponseRepository.processlogin(
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

    suspend fun getUserInfo(accessToken: String?): UserInfoResponse {
        val userInfoResponse: UserInfoResponse =
            userInfoResponseRepository.getUserInfo(accessToken)
        if (userInfoResponse?.isSuccessful == true) {
            userIsAuthenticated = true
        }
        return userInfoResponse
    }

    suspend fun logout(): LogoutResponse {
        val logoutResponse: LogoutResponse = logoutRepository.logout()
        if (logoutResponse.isSuccessful == true) {
            userIsAuthenticated = false
        }
        return logoutResponse
    }

    suspend fun isOPConfigurationInDatabase(): Boolean {
        return opConfigurationRepository.isOPConfigurationInDatabase()
    }

    suspend fun getOPConfigurationInDatabase(): OPConfiguration? {
        return opConfigurationRepository.getOPConfigurationInDatabase()
    }

    suspend fun isClientInDatabase(): Boolean {
        return dcrRepository.isClientInDatabase()
    }

    suspend fun getClientInDatabase(): OIDCClient? {
        return dcrRepository.getClientInDatabase()
    }

    suspend fun isAuthenticated(accessToken: String?): Boolean {
        return loginResponseRepository.isAuthenticated(accessToken)
    }

    suspend fun getUserInfoWithAccessToken(accessToken: String?): UserInfoResponse? {
        return userInfoResponseRepository.getUserInfo(accessToken)
    }
}