package com.example.fido2.repository.userinfo

import com.example.fido2.model.UserInfoResponse

interface UserInfoRepository {
    suspend fun getUserInfo(accessToken: String?, clientId: String, clientSecret: String): UserInfoResponse
}