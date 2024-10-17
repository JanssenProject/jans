package com.example.fido2.usecase

import com.example.fido2.repository.login.LoginRepository

class LoginUseCase(private val repository: LoginRepository) {

    suspend operator fun invoke(usernameText: String, passwordText: String?, authMethod: String, assertionResultRequest: String?) = runCatching {
        repository.processLogin(usernameText, passwordText, authMethod, assertionResultRequest)
    }
}