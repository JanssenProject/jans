package com.example.fido2.usecase

import com.example.fido2.repository.logout.LogoutRepository

class LogoutUseCase(private val repository: LogoutRepository) {

    suspend operator fun invoke() = runCatching { repository.logout() }
}