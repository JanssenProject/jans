package com.example.fido2.repository.main

import com.example.fido2.services.Analytic
import com.example.fido2.database.local.LocalDataSource
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import com.example.fido2.model.BackendError
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.fido.config.FidoConfiguration
import com.example.fido2.model.fido.config.FidoConfigurationResponse
import com.example.fido2.retrofit.ApiClient
import com.example.fido2.utils.AppConfig

class MainRepositoryImpl(
    private val apiService: ApiClient,
    private val localDataSource: LocalDataSource
): MainRepository {

    override suspend fun getOPConfiguration(analytic: Analytic): OPConfiguration {
        var opConfiguration = localDataSource.getOPConfiguration()
        val configurationUrl = localDataSource.getServerUrl() + AppConfig.OP_CONFIG_URL
        analytic.logEvent("getOPConfiguration from DB $opConfiguration")
        if (opConfiguration == null) {
            opConfiguration =
                OPConfiguration("", null, null, null, null, null, null)
            val result: OPConfiguration? = apiOPConfigurationFetch(configurationUrl, opConfiguration)
            println("getOPConfiguration from API $result")
            if (result != null) {
                result.isSuccessful = true
                localDataSource.saveOPConfiguration(result)
                return result
            }
            return opConfiguration
        }

        return opConfiguration
    }

    override suspend fun getFidoConfiguration(): FidoConfiguration {
        var fidoConfiguration = localDataSource.getFidoConfiguration()
        if (fidoConfiguration == null) {
            println("getFidoConfiguration from DB --> $fidoConfiguration")
            val configurationUrl = localDataSource.getServerUrl() + AppConfig.FIDO_CONFIG_URL
            fidoConfiguration = FidoConfiguration("", null, null, null, null, null)
            val fidoConfigurationResponse: FidoConfigurationResponse? = apiFidoConfigurationFetch(configurationUrl)
            println("getFidoConfiguration from API --> $fidoConfigurationResponse")
            if (fidoConfigurationResponse == null) {
                fidoConfiguration.isSuccessful = false
                fidoConfiguration.errorMessage =
                    "Error in fetching FIDO Configuration"
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
            println("getFidoConfiguration --> $fidoConfiguration")
            localDataSource.saveFidoConfiguration(fidoConfiguration)
            return fidoConfiguration
        }
        fidoConfiguration.isSuccessful = true
        return fidoConfiguration
    }

    private suspend fun apiOPConfigurationFetch(configurationUrl: String, opConfiguration: OPConfiguration?): OPConfiguration? {
        val response: HttpResponse = apiService.getOPConfiguration(configurationUrl)
        if (response.status != HttpStatusCode.OK) {
            val backendError: BackendError = response.body()
            opConfiguration?.isSuccessful = false
            opConfiguration?.errorMessage = "Error in fetching OP configuration. ${backendError.error_description}"
            return opConfiguration
        }
        val opConfiguration: OPConfiguration = response.body()
        return opConfiguration
    }

    private suspend fun apiFidoConfigurationFetch(configurationUrl: String): FidoConfigurationResponse? {
        var fidoConfiguration = FidoConfigurationResponse()
        val response: HttpResponse = apiService.getFidoConfiguration(configurationUrl)
        if (response.status != HttpStatusCode.OK) {
            val backendError: BackendError = response.body()
            fidoConfiguration.isSuccessful = false
            fidoConfiguration.errorMessage = "Error in fetching OP configuration. ${backendError.error_description}"
            return fidoConfiguration
        }
        val fidoConfigurationResponse: FidoConfigurationResponse? = response.body()
        return fidoConfigurationResponse
    }
}