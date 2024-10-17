package com.example.fido2.usecase

import com.example.fido2.repository.userinfo.UserInfoRepository

class GetUserInfoUseCase(private val repository: UserInfoRepository) {

    suspend operator fun invoke(accessToken: String?, clientId: String, clientSecret: String) = runCatching {
        repository.getUserInfo(accessToken, clientId, clientSecret)
    }
}