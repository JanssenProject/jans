package com.example.fido2.repository.login

import com.example.fido2.model.LoginResponse

interface LoginRepository {
    suspend fun processLogin(usernameText: String, passwordText: String?, authMethod: String, assertionResultRequest: String?): LoginResponse
}