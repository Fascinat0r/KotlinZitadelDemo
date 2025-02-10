package com.example.zitadelapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zitadelapp.model.UserInfo
import com.example.zitadelapp.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> = _userInfo

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchUserInfo(accessToken: String) {
        viewModelScope.launch {
            try {
                val info = repository.fetchUserInfo(accessToken)
                _userInfo.postValue(info)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Unknown error")
            }
        }
    }
}
