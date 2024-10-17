package com.example.fido2.repository.dcr

import com.example.fido2.authAdaptor.DPoPProofFactoryProvider
import com.example.fido2.database.local.LocalDataSource
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import com.example.fido2.model.BackendError
import com.example.fido2.utils.AppConfig
import com.example.fido2.model.DCRequest
import com.example.fido2.model.DCResponse
import com.example.fido2.model.SSARegRequest
import com.example.fido2.model.OIDCClient
import com.example.fido2.model.OPConfiguration
import com.example.fido2.retrofit.ApiClient
import utils.randomUUID

class DCRRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): DCRRepository {
    private var oidcClient: OIDCClient =
        OIDCClient("", null, null, null, null, null, null)

    suspend fun doDCRUsingSSA(ssaJwt: String?, scopeText: String?, dpoPProofFactory: DPoPProofFactoryProvider?): OIDCClient? {
        val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
        if (opConfiguration == null) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "OPConfiguration configuration not found in database.."
            return oidcClient
        }
        val issuer: String? = opConfiguration.issuer
        val registrationUrl: String? = opConfiguration.registrationEndpoint

        val dcrRequest = SSARegRequest(
            AppConfig.APP_NAME + randomUUID(),
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
        claims["seq"] = randomUUID()
        claims["app_id"] = AppConfig.APP_PACKAGE
        var checksum = ""
        try {
            checksum = dpoPProofFactory?.getChecksum() ?: ""
            claims["app_checksum"] = checksum
        } catch (e: IOException) {
            println("Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum.${e.message}".trimIndent()
            return oidcClient
        }

        val evidenceJwt: String? = dpoPProofFactory?.issueJWTToken(claims)
        dcrRequest.evidence = evidenceJwt

        val jwks = dpoPProofFactory?.getJWKS()
        dcrRequest.jwks = jwks
        println("Inside doDCR :: jwks :: $jwks")

        val response = apiService.doDCR(dcrRequest, registrationUrl ?: "")

        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
            val backendError: BackendError = response.body()
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${backendError.reason} Error message: ${backendError.error_description}"
            println(
                "Error in  DCR. Error code: ${backendError.reason} Error message: ${backendError.error_description}"
            )
            return oidcClient
        }
        val dcrResponse: DCResponse? = response.body()
        if (dcrResponse == null) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${response.status} Error message: $response"
            println(
                "Error in  DCR. Error code: ${response.status} Error message: $response"
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

        localDataSource.deleteAllOIDCClients()
        localDataSource.saveOIDCClient(oidcClient)
        println("DCR is successful")

        return oidcClient

    }
    suspend fun doDCR(scopeText: String?, dpoPProofFactory: DPoPProofFactoryProvider): OIDCClient? {
        val opConfiguration: OPConfiguration? = localDataSource.getOPConfiguration()
        if (opConfiguration == null) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "OPConfiguration configuration not found in database.."
            return oidcClient
        }
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
            AppConfig.APP_NAME + randomUUID(),
            "client_secret_basic",
            null,
            null,
        )

        val claims: MutableMap<String?, Any?> = HashMap()
        claims["appName"] = AppConfig.APP_NAME
        claims["seq"] = randomUUID()
        claims["app_id"] = AppConfig.APP_PACKAGE
        var checksum = ""
        try {
            checksum = dpoPProofFactory.getChecksum()
            claims["app_checksum"] = checksum
        } catch (e: IOException) {
            println("Error in generating app checksum. ${e.message}".trimIndent())
            oidcClient.isSuccessful = false
            oidcClient.errorMessage = "Error in generating app checksum.${e.message}".trimIndent()
            return oidcClient
        }

//        val appIntegrityList: List<AppIntegrityEntity> = appDatabase.appIntegrityDao().getAll()
//        if (appIntegrityList.isEmpty()) {
//            oidcClient.isSuccessful = false
//            oidcClient.errorMessage = "App Integrity not found in database"
//            return oidcClient
//        }
//        claims["app_integrity_result"] = appIntegrityList[0]

        val evidenceJwt: String? = dpoPProofFactory.issueJWTToken(claims)
        dcrRequest.evidence =  evidenceJwt
        println("Inside doDCR :: evidence :: $evidenceJwt")

        val jwks = dpoPProofFactory.getJWKS()
        dcrRequest.jwks = jwks
        println("Inside doDCR :: jwks :: $jwks")

        var response = apiService.doDCR(dcrRequest, registrationUrl ?: "")

        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
            val backendError: BackendError = response.body()
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${backendError.reason} Error message: ${backendError.error_description}"
            println(
                "Error in  DCR. Error code: ${backendError.reason} Error message: ${backendError.error_description}"
            )
            return oidcClient
        }
        val dcrResponse: DCResponse? = response.body()
        if (dcrResponse == null) {
            oidcClient.isSuccessful = false
            oidcClient.errorMessage =
                "Error in  DCR. Error code: ${response.status} Error message: $response"
            println(
                "Error in  DCR. Error code: ${response.status} Error message: $response}"
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

        localDataSource.deleteAllOIDCClients()
        localDataSource.saveOIDCClient(oidcClient)
        println("DCR is successful")

        return oidcClient

    }

    override suspend fun getOIDCClient(dpoPProofFactory: DPoPProofFactoryProvider?): OIDCClient? {
        var oidcClient: OIDCClient? = localDataSource.getOIDCClient()
        if(oidcClient != null) {
            oidcClient.isSuccessful = true
            return oidcClient
        }
        oidcClient = doDCRUsingSSA(AppConfig.SSA, AppConfig.ALLOWED_REGISTRATION_SCOPES, dpoPProofFactory)
        return oidcClient
    }

    suspend fun deleteClientInDatabase() {
        localDataSource.deleteAllOIDCClients()
    }

}