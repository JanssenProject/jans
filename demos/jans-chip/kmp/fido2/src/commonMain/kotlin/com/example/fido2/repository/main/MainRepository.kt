package com.example.fido2.repository.main

import com.example.fido2.services.Analytic
import com.example.fido2.model.OPConfiguration
import com.example.fido2.model.fido.config.FidoConfiguration

interface MainRepository {
    suspend fun getOPConfiguration(analytic: Analytic): OPConfiguration
    suspend fun getFidoConfiguration(): FidoConfiguration
}