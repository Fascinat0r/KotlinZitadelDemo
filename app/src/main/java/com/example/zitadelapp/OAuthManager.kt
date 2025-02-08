package com.example.zitadelapp

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object OAuthManager {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    @Serializable
    data class TokenResponse(
        @SerialName("access_token")
        val accessToken: String,
        @SerialName("token_type")
        val tokenType: String,
        @SerialName("expires_in")
        val expiresIn: Int,
        @SerialName("id_token")
        val idToken: String? = null,
        @SerialName("refresh_token")
        val refreshToken: String? = null,
        val scope: String? = null
    )

    /**
     * Запрашивает данные пользователя (userinfo) с использованием access token.
     */
    suspend fun getUserInfo(accessToken: String): String {
        val userInfoEndpoint = "https://iam.remystorage.ru/oidc/v1/userinfo"
        Log.d("OAuthManager", "Запрос userinfo с access token: $accessToken")
        val response: HttpResponse = client.get(userInfoEndpoint) {
            headers {
                append("Authorization", "Bearer $accessToken")
            }
        }
        val body = response.bodyAsText()
        Log.d("OAuthManager", "Ответ userinfo: $body")
        return body
    }
}
