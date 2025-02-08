package com.example.zitadelapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

class MainActivity : AppCompatActivity() {

    companion object {
        private const val RC_AUTH = 100
    }

    private lateinit var btnLogin: Button
    private lateinit var tvResult: TextView
    private lateinit var authService: AuthorizationService
    private var authState: AuthState? = null

    // Сохраняем codeVerifier (для логирования и отладки, хотя AppAuth сам передаёт PKCE-параметры)
    private var codeVerifier: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLogin = findViewById(R.id.btnLogin)
        tvResult = findViewById(R.id.tvResult)

        authService = AuthorizationService(this)

        btnLogin.setOnClickListener {
            startAuthentication()
        }

        // Если Activity запущено через deep link (например, если Custom Tabs не вернул результат корректно), обработаем intent
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    /**
     * Запускает процесс аутентификации:
     * – Создаёт конфигурацию сервиса (authorization и token endpoints)
     * – Генерирует PKCE-параметры с помощью PKCEUtil
     * – Формирует AuthorizationRequest с поддержкой PKCE
     * – Создаёт intent с Custom Tabs и запускает его
     */
    private fun startAuthentication() {
        Log.d("MainActivity", "Начало аутентификации")

        // Конфигурация эндпоинтов
        val authEndpoint = Uri.parse("https://iam.remystorage.ru/oauth/v2/authorize")
        val tokenEndpoint = Uri.parse("https://iam.remystorage.ru/oauth/v2/token")
        val serviceConfig = AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint)

        val clientId = "303649605919244516"
        val redirectUri = Uri.parse("com.example.zitadelapp://oauth2redirect")
        val scope = "openid profile email"

        // Генерация PKCE-параметров
        codeVerifier = PKCEUtil.generateCodeVerifier()
        val codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier!!)
        Log.d("MainActivity", "Сгенерирован codeVerifier: $codeVerifier")
        Log.d("MainActivity", "Сгенерирован codeChallenge: $codeChallenge")

        // Создаем запрос авторизации с PKCE
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        )
            .setScope(scope)
            .setCodeVerifier(codeVerifier, codeChallenge, "S256")
            .build()

        Log.d("MainActivity", "Сформирован AuthorizationRequest: $authRequest")

        // Создаем intent для Custom Tabs
        val customTabsIntent = CustomTabsIntent.Builder().build()
        val authIntent = authService.getAuthorizationRequestIntent(authRequest, customTabsIntent)
        Log.d("MainActivity", "Запуск authorization intent через Custom Tab")
        startActivityForResult(authIntent, RC_AUTH)
    }

    /**
     * Обрабатывает результат аутентификации, полученный через onActivityResult.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(
            "MainActivity",
            "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, data=$data"
        )
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_AUTH) {
            val response = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)

            if (response != null) {
                Log.d("MainActivity", "Получен AuthorizationResponse: $response")
                authState = AuthState(response, ex)
                // Начинаем обмен кода на токен
                exchangeToken(response)
            } else {
                Log.e(
                    "MainActivity",
                    "Ошибка авторизации: ${ex?.errorDescription ?: "Response is null"}"
                )
                tvResult.text = "Ошибка авторизации: ${ex?.errorDescription ?: "Response is null"}"
            }
        }
    }

    /**
     * Обменивает authorization code на токен с помощью AppAuth.
     */
    private fun exchangeToken(authResponse: AuthorizationResponse) {
        val tokenRequest = authResponse.createTokenExchangeRequest()
        Log.d("MainActivity", "Выполнение token exchange, запрос: $tokenRequest")

        authService.performTokenRequest(tokenRequest) { response, ex ->
            if (response != null) {
                authState?.update(response, ex)
                Log.d("MainActivity", "Получен token exchange response: $response")
                val accessToken = response.accessToken
                if (accessToken != null) {
                    Log.d("MainActivity", "Получен access token: $accessToken")
                    lifecycleScope.launch {
                        try {
                            val userInfo = OAuthManager.getUserInfo(accessToken)
                            Log.d("MainActivity", "Получен userinfo: $userInfo")
                            tvResult.text = "Access Token: $accessToken\n\nUser Info:\n$userInfo"
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Ошибка получения userinfo", e)
                            tvResult.text = "Ошибка получения данных пользователя: ${e.message}"
                        }
                    }
                } else {
                    Log.e("MainActivity", "Access token отсутствует в ответе token exchange")
                    tvResult.text = "Ошибка: Access token не получен"
                }
            } else {
                Log.e(
                    "MainActivity",
                    "Token exchange завершился с ошибкой: ${ex?.errorDescription}"
                )
                tvResult.text = "Ошибка обмена токена: ${ex?.errorDescription}"
            }
        }
    }

    /**
     * Обрабатывает intent, если приложение запущено через deep link.
     * (В данном примере AppAuth ожидает результат через onActivityResult, но мы логируем данные для отладки.)
     */
    private fun handleIntent(intent: Intent) {
        Log.d("MainActivity", "handleIntent вызван с intent: $intent")
        val data: Uri? = intent.data
        if (data != null) {
            Log.d("MainActivity", "Получены данные intent: $data")
            // Если deep link пришёл, можно обработать его здесь (но стандартный flow через AppAuth использует onActivityResult)
        } else {
            Log.d("MainActivity", "Intent не содержит данных")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }
}
