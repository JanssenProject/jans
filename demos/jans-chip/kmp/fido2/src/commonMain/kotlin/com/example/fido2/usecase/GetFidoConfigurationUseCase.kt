package com.example.fido2.usecase

import com.example.fido2.repository.main.MainRepository

class GetFidoConfigurationUseCase(private val repository: MainRepository) {

    suspend operator fun invoke() = runCatching { repository.getFidoConfiguration() }
}