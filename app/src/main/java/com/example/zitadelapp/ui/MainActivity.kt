package com.example.zitadelapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.zitadelapp.R
import com.example.zitadelapp.auth.OAuthManager
import com.example.zitadelapp.viewmodel.AuthViewModel
import net.openid.appauth.AuthorizationService

class MainActivity : AppCompatActivity() {

    private lateinit var btnLogin: Button
    private lateinit var tvResult: TextView
    private lateinit var oauthManager: OAuthManager
    private lateinit var authViewModel: AuthViewModel

    private val authLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(
                "MainActivity",
                "ActivityResult: resultCode=${result.resultCode}, data=${result.data}"
            )
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                oauthManager.handleAuthorizationResponse(result.data!!) { accessToken ->
                    authViewModel.fetchUserInfo(accessToken)
                }
            } else {
                Log.e(
                    "MainActivity",
                    "Authorization canceled or failed: resultCode=${result.resultCode}"
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLogin = findViewById(R.id.btnLogin)
        tvResult = findViewById(R.id.tvResult)

        // Инициализируем ViewModel через ViewModelProvider
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Инициализируем OAuthManager с использованием AppAuth
        val authService = AuthorizationService(this)
        oauthManager = OAuthManager(authService)

        btnLogin.setOnClickListener {
            // Получаем Intent авторизации из OAuthManager и запускаем его через ActivityResultLauncher
            val authIntent = oauthManager.createAuthIntent()
            authLauncher.launch(authIntent)
        }

        authViewModel.userInfo.observe(this) { info ->
            tvResult.text = "User Info:\n$info"
        }

        authViewModel.error.observe(this) { err ->
            tvResult.text = "Error: $err"
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null) {
            Log.d("MainActivity", "handleIntent: received data: $data")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        oauthManager.authService.dispose()
    }
}
