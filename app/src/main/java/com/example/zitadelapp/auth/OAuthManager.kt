package com.example.zitadelapp.auth

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.example.zitadelapp.util.PKCEUtil
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

class OAuthManager(private val authService: AuthorizationService) {

    companion object {
        const val RC_AUTH = 100
    }

    private var authState: AuthState? = null
    private var codeVerifier: String? = null

    /**
     * Запускает процесс авторизации с поддержкой PKCE через AppAuth и Custom Tabs.
     */
    fun startAuthentication(activity: Activity) {
        Log.d("OAuthManager", "Начало аутентификации")

        val authEndpoint = Uri.parse("https://iam.remystorage.ru/oauth/v2/authorize")
        val tokenEndpoint = Uri.parse("https://iam.remystorage.ru/oauth/v2/token")
        val serviceConfig = AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint)

        val clientId = "303649605919244516"
        val redirectUri = Uri.parse("com.example.zitadelapp://oauth2redirect")
        val scope = "openid profile email"

        // Генерация PKCE-параметров
        codeVerifier = PKCEUtil.generateCodeVerifier()
        val codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier!!)
        Log.d("OAuthManager", "codeVerifier: $codeVerifier")
        Log.d("OAuthManager", "codeChallenge: $codeChallenge")

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
            .setScope(scope)
            .setCodeVerifier(codeVerifier, codeChallenge, "S256")
            .build()

        // Используем CustomTabsIntent из androidx.browser.customtabs
        val customTabsIntent = CustomTabsIntent.Builder().build()
        val authIntent = authService.getAuthorizationRequestIntent(authRequest, customTabsIntent)
        activity.startActivityForResult(authIntent, RC_AUTH)
    }

    /**
     * Обрабатывает ответ авторизации и выполняет обмен кода на токен.
     */
    fun handleAuthorizationResponse(
        data: android.content.Intent,
        onTokenReceived: (String) -> Unit
    ) {
        val response = AuthorizationResponse.fromIntent(data)
        val ex = AuthorizationException.fromIntent(data)
        if (response != null) {
            Log.d("OAuthManager", "AuthorizationResponse: $response")
            authState = AuthState(response, ex)
            val tokenRequest = response.createTokenExchangeRequest()
            Log.d("OAuthManager", "TokenExchangeRequest: $tokenRequest")
            authService.performTokenRequest(tokenRequest) { tokenResponse, exception ->
                if (tokenResponse != null) {
                    authState?.update(tokenResponse, exception)
                    val accessToken = tokenResponse.accessToken
                    if (accessToken != null) {
                        Log.d("OAuthManager", "Access token получен: $accessToken")
                        onTokenReceived(accessToken)
                    } else {
                        Log.e("OAuthManager", "Access token отсутствует")
                    }
                } else {
                    Log.e("OAuthManager", "Ошибка token exchange: ${exception?.errorDescription}")
                }
            }
        } else {
            Log.e(
                "OAuthManager",
                "Ошибка авторизации: ${ex?.errorDescription ?: "Response is null"}"
            )
        }
    }
}
