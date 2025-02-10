package com.example.zitadelapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLogin = findViewById(R.id.btnLogin)
        tvResult = findViewById(R.id.tvResult)

        // Инициализируем ViewModel через ViewModelProvider
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Инициализируем AppAuth
        val authService = AuthorizationService(this)
        oauthManager = OAuthManager(authService)

        btnLogin.setOnClickListener {
            oauthManager.startAuthentication(this)
        }

        // Подписываемся на изменения userInfo
        authViewModel.userInfo.observe(this, Observer { info ->
            tvResult.text = "User Info:\n$info"
        })

        authViewModel.error.observe(this, Observer { err ->
            tvResult.text = "Error: $err"
        })

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(
            "MainActivity",
            "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, data=$data"
        )
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OAuthManager.RC_AUTH && resultCode == Activity.RESULT_OK && data != null) {
            oauthManager.handleAuthorizationResponse(data) { accessToken ->
                authViewModel.fetchUserInfo(accessToken)
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null) {
            Log.d("MainActivity", "handleIntent: received data: $data")
        }
    }
}
