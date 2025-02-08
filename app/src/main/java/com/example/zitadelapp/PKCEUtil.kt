package com.example.zitadelapp

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PKCEUtil {
    /**
     * Генерирует случайный code verifier
     */
    fun generateCodeVerifier(length: Int = 64): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(length)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Вычисляет code challenge как SHA-256 хэш code verifier
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
