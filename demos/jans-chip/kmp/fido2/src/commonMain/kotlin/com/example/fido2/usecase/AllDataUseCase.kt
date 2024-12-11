package com.example.fido2.usecase

import com.example.fido2.repository.settings.AllDataRepository

class AllDataUseCase(private val repository: AllDataRepository) {
    suspend operator fun invoke() = runCatching { repository.clearAll() }
}