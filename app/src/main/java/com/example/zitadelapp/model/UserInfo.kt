package com.example.zitadelapp.model

data class UserInfo(
    val sub: String,
    val name: String,
    val given_name: String,
    val family_name: String,
    val nickname: String,
    val gender: String,
    val locale: String,
    val updated_at: Long,
    val preferred_username: String,
    val email: String,
    val email_verified: Boolean
)
