package com.example.zitadelapp.auth

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.example.zitadelapp.util.Config
import com.example.zitadelapp.util.PKCEUtil
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

class OAuthManager(val authService: AuthorizationService) {

    var authState: AuthState? = null
    var codeVerifier: String? = null

    /**
     * Создает Intent для запуска авторизации.
     */
    fun createAuthIntent(): Intent {
        Log.d("OAuthManager", "Starting authentication")
        val authEndpoint = Uri.parse(Config.AUTH_ENDPOINT)
        val tokenEndpoint = Uri.parse(Config.TOKEN_ENDPOINT)
        val serviceConfig = AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint)
        val clientId = Config.CLIENT_ID
        val redirectUri = Uri.parse(Config.REDIRECT_URI)
        val scope = Config.SCOPE

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

        val customTabsIntent = CustomTabsIntent.Builder().build()
        return authService.getAuthorizationRequestIntent(authRequest, customTabsIntent)
    }

    /**
     * Обрабатывает ответ авторизации и выполняет обмен кода на токен.
     */
    fun handleAuthorizationResponse(data: Intent, onTokenReceived: (String) -> Unit) {
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
                        Log.d("OAuthManager", "Access token received: $accessToken")
                        onTokenReceived(accessToken)
                    } else {
                        Log.e("OAuthManager", "Access token is missing")
                    }
                } else {
                    Log.e("OAuthManager", "Token exchange error: ${exception?.errorDescription}")
                }
            }
        } else {
            Log.e(
                "OAuthManager",
                "Authorization error: ${ex?.errorDescription ?: "Response is null"}"
            )
        }
    }
}
