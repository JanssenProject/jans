package io.jans.chip.repository

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import io.jans.chip.AppDatabase
import io.jans.chip.model.OPConfiguration
import io.jans.chip.model.appIntegrity.AppIntegrityEntity
import io.jans.chip.model.appIntegrity.AppIntegrityResponse
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.utils.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.UUID

class PlayIntegrityRepository(context: Context) {
    val TAG = "PlayIntegrityRepository"
    private var appIntegrityResponse: AppIntegrityResponse? =
        AppIntegrityResponse(null, null, null, null)
    var obtainedContext: Context = context
    private val appDatabase = AppDatabase.getInstance(context);

    suspend fun checkAppIntegrity(): AppIntegrityResponse? {
        // Create an instance of a manager.
        val integrityManager = IntegrityManagerFactory.create(obtainedContext)

        // Request the integrity token by providing a nonce.
        val integrityTokenResponse = integrityManager
            .requestIntegrityToken(
                IntegrityTokenRequest.builder().setNonce(UUID.randomUUID().toString())
                    .setCloudProjectNumber(AppConfig.GOOGLE_CLOUD_PROJECT_ID).build()
            )
        integrityTokenResponse.addOnSuccessListener { integrityTokenResponse1: IntegrityTokenResponse ->
            CoroutineScope(Dispatchers.Main).launch {
                val integrityToken = integrityTokenResponse1.token()
                Log.d("Integrity token Obtained result", "success")
                appIntegrityResponse = getTokenResponse(integrityToken)
            }
        }
        integrityTokenResponse.addOnFailureListener { e: Exception ->
            Log.e("Integrity token Obtained result", "failure")
            appDatabase.appIntegrityDao().insert(
                setErrorInAppIntegrityEntity(
                    "Error in obtaining integrity token :: " + getErrorText(e)
                )
            )
            appIntegrityResponse?.isSuccessful = false
            appIntegrityResponse?.errorMessage =
                "Error in obtaining integrity token :: " + getErrorText(e)

        }
        return appIntegrityResponse
    }

    private suspend fun getTokenResponse(integrityToken: String): AppIntegrityResponse? {
        Log.d(
            "INTEGRITY_APP_SERVER_URL",
            AppConfig.INTEGRITY_APP_SERVER_URL + "/api/check?token=" + integrityToken
        )

        val response: Response<AppIntegrityResponse>? =
            ApiAdapter.getInstance(AppConfig.INTEGRITY_APP_SERVER_URL)
                .verifyIntegrityTokenOnAppServer(AppConfig.INTEGRITY_APP_SERVER_URL + "/api/check?token=" + integrityToken)

        if (response?.code() != 200) {
            appDatabase.appIntegrityDao()
                .insert(setErrorInAppIntegrityEntity("AppIntegrity response is unsuccessful. Error code: ${response?.code()}, Error message: ${response?.message()}"))
            appIntegrityResponse?.isSuccessful = false
            appIntegrityResponse?.errorMessage =
                "AppIntegrity response is unsuccessful. Error code: ${response?.code()}, Error message: ${response?.message()}"
            return appIntegrityResponse
        }
        appIntegrityResponse = response.body()

        if (appIntegrityResponse == null) {
            Log.e("Response from App server :: ", "Response body is empty")
            appDatabase.appIntegrityDao()
                .insert(setErrorInAppIntegrityEntity("Empty response obtained from App Server."))
            appIntegrityResponse?.isSuccessful = false
            appIntegrityResponse?.errorMessage = "Empty response obtained from App Server."
            return appIntegrityResponse
        }

        if (!response.isSuccessful) {
            appDatabase.appIntegrityDao()
                .insert(setErrorInAppIntegrityEntity("AppIntegrity response is unsuccessful."))
            appIntegrityResponse?.isSuccessful = false
            appIntegrityResponse?.errorMessage = "AppIntegrity response is unsuccessful."
            return appIntegrityResponse
        }

        if (appIntegrityResponse?.error != null) {
            Log.e("Response from App server :: ", "Response body has error")
            appDatabase.appIntegrityDao()
                .insert(setErrorInAppIntegrityEntity("Response from App server has error :: " + appIntegrityResponse?.error))
            appIntegrityResponse?.isSuccessful = false
            appIntegrityResponse?.errorMessage =
                "Response from App server has error :: " + appIntegrityResponse?.error
            return appIntegrityResponse
        }
        if (appIntegrityResponse?.appIntegrity == null || appIntegrityResponse?.appIntegrity
                ?.appRecognitionVerdict == null
        ) {
            Log.e(
                "Response from App server :: ",
                "Response body do not have appIntegrity"
            )

            appDatabase.appIntegrityDao()
                .insert(setErrorInAppIntegrityEntity("Response body do not have appIntegrity."))
            appIntegrityResponse?.isSuccessful = false
            appIntegrityResponse?.errorMessage = "Response body do not have appIntegrity."
            return appIntegrityResponse

        }
        Log.d(
            "Inside getTokenResponse :: appIntegrityResponse ::",
            appIntegrityResponse?.appIntegrity?.appRecognitionVerdict.toString()
        )
        val appIntegrityEntity = AppIntegrityEntity(
            AppConfig.DEFAULT_S_NO,
            appIntegrityResponse?.appIntegrity?.appRecognitionVerdict,
            appIntegrityResponse?.deviceIntegrity?.commasSeparatedString(),
            appIntegrityResponse?.accountDetails?.appLicensingVerdict,
            appIntegrityResponse?.requestDetails?.requestPackageName,
            appIntegrityResponse?.requestDetails?.nonce,
            appIntegrityResponse?.error
        )
        appDatabase.appIntegrityDao().insert(appIntegrityEntity)
        appIntegrityResponse?.isSuccessful = true

        return appIntegrityResponse
    }


    private fun getErrorText(e: Exception): String {
        val msg = e.message ?: return "Unknown Error"
        val errorCode = msg.replace("\n".toRegex(), "").replace(":(.*)".toRegex(), "").toInt()
        return when (errorCode) {
            IntegrityErrorCode.API_NOT_AVAILABLE -> """
    Integrity API is not available.
    
    The Play Store version might be old, try updating it.
    """.trimIndent()

            IntegrityErrorCode.APP_NOT_INSTALLED -> """
                The calling app is not installed.
                
                This shouldn't happen. If it does please open an issue on Github.
                """.trimIndent()

            IntegrityErrorCode.APP_UID_MISMATCH -> """
                The calling app UID (user id) does not match the one from Package Manager.
                
                This shouldn't happen. If it does please open an issue on Github.
                """.trimIndent()

            IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> """
                Binding to the service in the Play Store has failed.
                
                This can be due to having an old Play Store version installed on the device.
                """.trimIndent()

            IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> "Unknown internal Google server error."
            IntegrityErrorCode.INTERNAL_ERROR -> "Unknown internal error."
            IntegrityErrorCode.NETWORK_ERROR -> """
                No available network is found.
                
                Please check your connection.
                """.trimIndent()

            IntegrityErrorCode.NO_ERROR -> """
                No error has occurred.
                
                If you ever get this, congrats, I have no idea what it means.
                """.trimIndent()

            IntegrityErrorCode.NONCE_IS_NOT_BASE64 -> """
                Nonce is not encoded as a base64 web-safe no-wrap string.
                
                This shouldn't happen. If it does please open an issue on Github.
                """.trimIndent()

            IntegrityErrorCode.NONCE_TOO_LONG -> """
                Nonce length is too long.
                This shouldn't happen. If it does please open an issue on Github.
                """.trimIndent()

            IntegrityErrorCode.NONCE_TOO_SHORT -> """
                Nonce length is too short.
                This shouldn't happen. If it does please open an issue on Github.
                """.trimIndent()

            IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> """
                Play Services is not available or version is too old.
                
                Try updating Google Play Services.
                """.trimIndent()

            IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> """
                No Play Store account is found on device.
                
                Try logging into Play Store.
                """.trimIndent()

            IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> """
                No Play Store app is found on device or not official version is installed.
                
                This app can't work without Play Store.
                """.trimIndent()

            IntegrityErrorCode.TOO_MANY_REQUESTS -> """
                The calling app is making too many requests to the API and hence is throttled.
                
                This shouldn't happen. If it does please open an issue on Github.
                """.trimIndent()

            else -> "Unknown Error"
        }
    }

    private fun setErrorInAppIntegrityEntity(error: String): AppIntegrityEntity? {
        return AppIntegrityEntity(AppConfig.DEFAULT_S_NO, null, null, null, null, null, error)
    }

    suspend fun getAppIntegrityEntityInDatabase(): AppIntegrityEntity? {
        var appIntegrityEntities: List<AppIntegrityEntity>? = appDatabase.appIntegrityDao().getAll()
        var appIntegrityEntity: AppIntegrityEntity? = null
        if(!appIntegrityEntities.isNullOrEmpty()) {
            appIntegrityEntity = appIntegrityEntities[0]
        }
        return appIntegrityEntity
    }

}