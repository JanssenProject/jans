package com.example.fido2.usecase

import com.example.fido2.services.Analytic
import com.example.fido2.repository.main.MainRepository

class GetOPConfigurationUseCase(private val repository: MainRepository, private val analytic: Analytic) {

    suspend operator fun invoke() = runCatching { repository.getOPConfiguration(analytic) }
}