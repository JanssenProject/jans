package com.example.fido2.repository.logout

import com.example.fido2.model.LogoutResponse

interface LogoutRepository {
    suspend fun logout(): LogoutResponse
}