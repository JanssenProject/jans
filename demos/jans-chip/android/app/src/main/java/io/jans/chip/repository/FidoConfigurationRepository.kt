package io.jans.chip.repository

import android.content.Context
import android.util.Log
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.utils.AppConfig
import io.jans.chip.AppDatabase
import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.fido.config.FidoConfiguration
import io.jans.chip.model.fido.config.FidoConfigurationResponse
import retrofit2.Response

class FidoConfigurationRepository(context: Context) {
    private val TAG = "FidoConfigurationRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    private var fidoConfigurationResponse: FidoConfigurationResponse? =
        FidoConfigurationResponse(null, null, null)
    var obtainedContext: Context = context

    suspend fun fetchFidoConfiguration(configurationUrl: String): FidoConfigurationResponse? {
        val issuer: String = configurationUrl.replace(AppConfig.FIDO_CONFIG_URL, "")
        Log.d(TAG, "Inside fetchFIDOConfiguration :: configurationUrl ::$configurationUrl")
        try {
            val opConfigurationList: List<OPConfiguration> = appDatabase.opConfigurationDao().getAll()
            if (opConfigurationList.isEmpty()) {
                fidoConfigurationResponse?.isSuccessful = false
                fidoConfigurationResponse?.errorMessage = "OpenID configuration not found in database."
                return fidoConfigurationResponse
            }
            val opConfiguration: OPConfiguration = opConfigurationList[0]

            val response: Response<FidoConfigurationResponse> =
                ApiAdapter.getInstance(issuer).getFidoConfiguration(configurationUrl)

            if (response.code() != 200) {
                fidoConfigurationResponse?.isSuccessful = false
                fidoConfigurationResponse?.errorMessage =
                    "Error in fetching FIDO Configuration. Error message: ${response.message()}"
                return fidoConfigurationResponse
            }
            fidoConfigurationResponse = response.body()
            if (!response.isSuccessful || fidoConfigurationResponse == null) {
                fidoConfigurationResponse?.isSuccessful = false
                fidoConfigurationResponse?.errorMessage =
                    "Error in fetching FIDO Configuration. Error message: ${response.message()}"
                return fidoConfigurationResponse
            }
            fidoConfigurationResponse?.isSuccessful = true
            val fidoConfigDB = FidoConfiguration(
                AppConfig.DEFAULT_S_NO,
                fidoConfigurationResponse?.issuer,
                fidoConfigurationResponse?.attestation?.optionsEndpoint,
                fidoConfigurationResponse?.attestation?.resultEndpoint,
                fidoConfigurationResponse?.assertion?.optionsEndpoint,
                fidoConfigurationResponse?.assertion?.resultEndpoint
            )

            Log.d(
                TAG,
                "Inside fetchOPConfiguration :: opConfiguration :: ${fidoConfigurationResponse?.issuer}"
            )
            appDatabase.fidoConfigurationDao().deleteAll()
            appDatabase.fidoConfigurationDao().insert(fidoConfigDB)

            opConfiguration.fidoUrl = configurationUrl
            appDatabase.opConfigurationDao().update(opConfiguration)

            return fidoConfigurationResponse
        } catch (e: Exception) {
            Log.e(TAG, "Error in  fetching OP Configuration. ${e.message}".trimIndent())
            fidoConfigurationResponse?.isSuccessful = false
            fidoConfigurationResponse?.errorMessage =
                "Error in fetching FIDO Configuration. Error message: ${e.message}"
            return fidoConfigurationResponse
        }
    }

    suspend fun getFidoConfigInDatabase(): FidoConfiguration? {
        val fidoConfigurationList: List<FidoConfiguration> = appDatabase.fidoConfigurationDao()
            .getAll()
        var fidoConfiguration: FidoConfiguration? = null
        if(fidoConfigurationList.isNotEmpty()) {
            fidoConfiguration = fidoConfigurationList.let { it -> it[0] }
        }
        return fidoConfiguration
    }

    suspend fun deleteFidoConfigurationInDatabase() {
        appDatabase.fidoConfigurationDao().deleteAll()
    }
}