package io.jans.chip.repository

import android.content.Context
import android.util.Log
import com.nimbusds.jwt.JWTClaimsSet
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.utils.AppConfig
import io.jans.chip.AppDatabase
import io.jans.chip.factories.DPoPProofFactory
import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.fido.config.FidoConfiguration
import io.jans.chip.model.fido.config.FidoConfigurationResponse
import retrofit2.Response

class FidoConfigurationRepository(context: Context) {
    private val TAG = "FidoConfigurationRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    private var fidoConfiguration: FidoConfiguration = FidoConfiguration("", null, null, null, null, null)
    var obtainedContext: Context = context

    private suspend fun fetchFidoConfiguration(configurationUrl: String): FidoConfiguration? {
        val issuer: String = configurationUrl.replace(AppConfig.FIDO_CONFIG_URL, "")
        Log.d(TAG, "Inside fetchFIDOConfiguration :: configurationUrl ::$configurationUrl")
        try {
            val opConfigurationList: List<OPConfiguration> = appDatabase.opConfigurationDao().getAll()
            if (opConfigurationList.isEmpty()) {
                fidoConfiguration.isSuccessful = false
                fidoConfiguration.errorMessage = "OpenID configuration not found in database."
                return fidoConfiguration
            }
            val opConfiguration: OPConfiguration = opConfigurationList[0]

            val response: Response<FidoConfigurationResponse> =
                ApiAdapter.getInstance(issuer).getFidoConfiguration(configurationUrl)

            if (response.code() != 200) {
                fidoConfiguration.isSuccessful = false
                fidoConfiguration.errorMessage =
                    "Error in fetching FIDO Configuration. Error message: ${response.message()}"
                return fidoConfiguration
            }
            val fidoConfigurationResponse: FidoConfigurationResponse? = response.body()
            if (!response.isSuccessful || fidoConfigurationResponse == null) {
                fidoConfiguration.isSuccessful = false
                fidoConfiguration.errorMessage =
                    "Error in fetching FIDO Configuration. Error message: ${response.message()}"
                return fidoConfiguration
            }
            fidoConfiguration = FidoConfiguration(
                AppConfig.DEFAULT_S_NO,
                fidoConfigurationResponse.issuer,
                fidoConfigurationResponse.attestation?.optionsEndpoint,
                fidoConfigurationResponse.attestation?.resultEndpoint,
                fidoConfigurationResponse.assertion?.optionsEndpoint,
                fidoConfigurationResponse.assertion?.resultEndpoint
            )
            fidoConfiguration.isSuccessful = true
            Log.d(
                TAG,
                "Inside fetchOPConfiguration :: ${fidoConfigurationResponse.issuer}"
            )
            appDatabase.fidoConfigurationDao().deleteAll()
            appDatabase.fidoConfigurationDao().insert(fidoConfiguration)

            opConfiguration.fidoUrl = configurationUrl
            appDatabase.opConfigurationDao().update(opConfiguration)

            return fidoConfiguration
        } catch (e: Exception) {
            Log.e(TAG, "Error in  fetching OP Configuration. ${e.message}".trimIndent())
            fidoConfiguration.isSuccessful = false
            fidoConfiguration.errorMessage =
                "Error in fetching FIDO Configuration. Error message: ${e.message}"
            return fidoConfiguration
        }
    }

    suspend fun getFidoConfig(): FidoConfiguration? {
        val fidoConfigurationList: List<FidoConfiguration> = appDatabase.fidoConfigurationDao().getAll()
        var fidoConfiguration: FidoConfiguration? = null
        if(!fidoConfigurationList.isNullOrEmpty()) {
            fidoConfiguration = fidoConfigurationList[0]
            fidoConfiguration.isSuccessful = true
            return fidoConfiguration
        }
        val jwtClaimsSet: JWTClaimsSet = DPoPProofFactory.getClaimsFromSSA()
        val issuer: String = jwtClaimsSet.getClaim("iss").toString()
        fidoConfiguration = fetchFidoConfiguration(issuer + AppConfig.FIDO_CONFIG_URL)
        return fidoConfiguration
    }

    suspend fun deleteFidoConfigurationInDatabase() {
        appDatabase.fidoConfigurationDao().deleteAll()
    }
}