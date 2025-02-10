package com.example.zitadelapp.network

import com.example.zitadelapp.model.UserInfo
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {
    @GET("oidc/v1/userinfo")
    suspend fun getUserInfo(
        @Header("Authorization") authHeader: String
    ): UserInfo
}
