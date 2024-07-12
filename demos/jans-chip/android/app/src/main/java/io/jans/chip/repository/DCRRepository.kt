package io.jans.chip.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.jans.chip.model.OPConfiguration
import io.jans.chip.retrofit.ApiAdapter
import io.jans.chip.utils.AppConfig
import io.jans.chip.AppDatabase
import io.jans.chip.factories.DPoPProofFactory
import io.jans.chip.factories.KeyManager
import io.jans.chip.model.DCRequest
import io.jans.chip.model.DCResponse
import io.jans.chip.model.JSONWebKeySet
import io.jans.chip.model.OIDCClient
import io.jans.chip.model.SSARegRequest
import io.jans.chip.utils.AppUtil
import retrofit2.Response
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.util.UUID

class DCRRepository(context: Context) {
    private val TAG = "DCRRepository"
    private val appDatabase = AppDatabase.getInstance(context);
    private var oidcClient: OIDCClient =
        OIDCClient("", null, null, null, null, null, null)
    var obtainedContext: Context = context

    suspend fun doDCRUsingSSA(ssaJwt: String?, scopeText: String?): OIDCClient? {
        val opConfigurationList: List<OPConfiguration> = appDatabase.opConfigurationDao().getAll()
        if (opConfigurationList.isEmpty()) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "OpenID configuration not found in database.."
            return oidcClient
        }
        val opConfiguration: OPConfiguration = opConfigurationList[0]
        val issuer: String? = opConfiguration.issuer
        val registrationUrl: String? = opConfiguration.registrationEndpoint

        val dcrRequest = SSARegRequest(
            AppConfig.APP_NAME + UUID.randomUUID(),
            null,
            null,
            scopeText,
            listOf("code"),
            listOf("authorization_code", "client_credentials"),
            ssaJwt,
            "native",
            listOf(issuer),
        )

        val claims: MutableMap<String?, Any?> = HashMap()
        claims["appName"] = AppConfig.APP_NAME
        claims["seq"] = UUID.randomUUID()
        claims["app_id"] = obtainedContext.getPackageName()
        var checksum: String? = null
        try {
            checksum = AppUtil.getChecksum(obtainedContext)
            claims["app_checksum"] = checksum
        } catch (e: IOException) {
            Log.d(TAG, "Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum.${e.message}".trimIndent()
            return oidcClient
        } catch (e: NoSuchAlgorithmException) {
            Log.d(TAG, "Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum. ${e.message}".trimIndent()
            return oidcClient
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum. ${e.message}".trimIndent()
            return oidcClient
        }

        try {
            val evidenceJwt: String? = DPoPProofFactory.issueJWTToken(claims)
            dcrRequest.evidence = evidenceJwt
            Log.d(TAG, "Inside doDCR :: evidence :: $evidenceJwt")
        } catch (e: InvalidAlgorithmParameterException) {
            Log.e(TAG, "Error in  generating DPoP jwt. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in  generating DPoP jwt. ${e.message}".trimIndent()

            return oidcClient
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Error in  generating DPoP jwt. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in  generating DPoP jwt. ${e.message}".trimIndent()

            return oidcClient
        } catch (e: NoSuchProviderException) {
            Log.e(TAG, "Error in  generating DPoP jwt. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in  generating DPoP jwt. ${e.message}".trimIndent()

            return oidcClient
        }
        val jwks = JSONWebKeySet()
        jwks.addKey(KeyManager.getPublicKeyJWK(KeyManager.getPublicKey())?.getRequiredParams())
        dcrRequest.jwks = jwks.toJsonString()
        Log.d(TAG, "Inside doDCR :: jwks :: " + jwks.toJsonString())

        val response: Response<DCResponse> =
            ApiAdapter.getInstance(issuer).doDCR(dcrRequest, registrationUrl)

        if (response.code() != 200 && response.code() != 201) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            Log.e(
                TAG,
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            )
            return oidcClient
        }
        val dcrResponse: DCResponse? = response.body()
        if (!response.isSuccessful || dcrResponse == null) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            Log.e(
                TAG,
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            )
            return oidcClient
        }

        oidcClient.sno = AppConfig.DEFAULT_S_NO
        oidcClient.clientName = dcrResponse.clientName
        oidcClient.clientId = dcrResponse.clientId
        oidcClient.clientSecret = dcrResponse.clientSecret
        oidcClient.scope = scopeText
        oidcClient.clientName = dcrResponse.clientName
        oidcClient.isSuccessful = true

        appDatabase.oidcClientDao().deleteAll()
        appDatabase.oidcClientDao().insert(oidcClient)
        Log.d(TAG, "DCR is successful")

        return oidcClient

    }
    suspend fun doDCR(scopeText: String?): OIDCClient? {
        val opConfigurationList: List<OPConfiguration> = appDatabase.opConfigurationDao().getAll()
        if (opConfigurationList == null || opConfigurationList.isEmpty()) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "OpenID configuration not found in database.."
            return oidcClient
        }
        val opConfiguration: OPConfiguration = opConfigurationList[0]
        val issuer: String? = opConfiguration.issuer
        val registrationUrl: String? = opConfiguration.registrationEndpoint

        val dcrRequest = DCRequest(
            issuer,
            listOf(issuer),
            scopeText,
            listOf("code"),
            listOf(issuer),
            listOf("authorization_code", "client_credentials"),
            "web",
            AppConfig.APP_NAME + UUID.randomUUID(),
            "client_secret_basic",
            null,
            null,
        )

        val claims: MutableMap<String?, Any?> = HashMap()
        claims["appName"] = AppConfig.APP_NAME
        claims["seq"] = UUID.randomUUID()
        claims["app_id"] = obtainedContext.getPackageName()
        var checksum: String? = null
        try {
            checksum = AppUtil.getChecksum(obtainedContext)
            claims["app_checksum"] = checksum
        } catch (e: IOException) {
            Log.d(TAG, "Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum.${e.message}".trimIndent()
            return oidcClient
        } catch (e: NoSuchAlgorithmException) {
            Log.d(TAG, "Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum. ${e.message}".trimIndent()
            return oidcClient
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum. ${e.message}".trimIndent()
            return oidcClient
        }

        /*val appIntegrityList: List<AppIntegrityEntity> = appDatabase.appIntegrityDao().getAll()
        if (appIntegrityList == null || appIntegrityList.isEmpty()) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "App Integrity not found in database"
            return oidcClient
        }
        claims["app_integrity_result"] = appIntegrityList[0]

         */
        try {
            val evidenceJwt: String? = DPoPProofFactory.issueJWTToken(claims)
            dcrRequest.evidence = evidenceJwt
            Log.d(TAG, "Inside doDCR :: evidence :: $evidenceJwt")
        } catch (e: InvalidAlgorithmParameterException) {
            Log.e(TAG, "Error in  generating DPoP jwt. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in  generating DPoP jwt. ${e.message}".trimIndent()

            return oidcClient
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Error in  generating DPoP jwt. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in  generating DPoP jwt. ${e.message}".trimIndent()

            return oidcClient
        } catch (e: NoSuchProviderException) {
            Log.e(TAG, "Error in  generating DPoP jwt. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in  generating DPoP jwt. ${e.message}".trimIndent()

            return oidcClient
        }
        val jwks = JSONWebKeySet()
        jwks.addKey(KeyManager.getPublicKeyJWK(KeyManager.getPublicKey())?.getRequiredParams())
        dcrRequest.jwks = jwks.toJsonString()
        Log.d(TAG, "Inside doDCR :: jwks :: " + jwks.toJsonString())

        var response: Response<DCResponse> =
            ApiAdapter.getInstance(issuer).doDCR(dcrRequest, registrationUrl)

        if (response.code() != 200 && response.code() != 201) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            Log.e(
                TAG,
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            )
            return oidcClient
        }
        val dcrResponse: DCResponse? = response.body()
        if (!response.isSuccessful || dcrResponse == null) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            Log.e(
                TAG,
                "Error in  DCR. Error code: ${response.code()} Error message: ${response.message()}"
            )
            return oidcClient
        }

        oidcClient.sno = AppConfig.DEFAULT_S_NO
        oidcClient.clientName = dcrResponse.clientName
        oidcClient.clientId = dcrResponse.clientId
        oidcClient.clientSecret = dcrResponse.clientSecret
        oidcClient.scope = scopeText
        oidcClient.clientName = dcrResponse.clientName
        oidcClient.isSuccessful = true

        appDatabase.oidcClientDao().deleteAll()
        appDatabase.oidcClientDao().insert(oidcClient)
        Log.d(TAG, "DCR is successful")

        return oidcClient

    }

    suspend fun getOIDCClient(): OIDCClient? {
        val oidcClients: List<OIDCClient> = appDatabase.oidcClientDao().getAll()
        var oidcClient: OIDCClient? = null
        if(!oidcClients.isNullOrEmpty()) {
            oidcClient = oidcClients[0]
            oidcClient.isSuccessful = true
            return oidcClient
        }
        oidcClient = doDCRUsingSSA(AppConfig.SSA, AppConfig.ALLOWED_REGISTRATION_SCOPES)
        return oidcClient
    }

    suspend fun deleteClientInDatabase() {
        appDatabase.oidcClientDao().deleteAll()
    }

}