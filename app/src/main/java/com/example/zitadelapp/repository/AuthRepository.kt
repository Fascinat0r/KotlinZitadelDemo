package com.example.zitadelapp.repository

import com.example.zitadelapp.model.UserInfo
import com.example.zitadelapp.network.RetrofitInstance

class AuthRepository {
    suspend fun fetchUserInfo(accessToken: String): UserInfo {
        return RetrofitInstance.api.getUserInfo("Bearer $accessToken")
    }
}
