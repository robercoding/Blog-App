package com.rober.blogapp.ui.auth

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.util.state.AuthStateEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AuthViewModel
@ViewModelInject
constructor(
private val firebaseRepository: FirebaseRepository
): ViewModel() {
    private val TAG = "AuthViewModel"

    private val _loginAuthState = MutableStateFlow<AuthStateEvent>(
        AuthStateEvent.CheckingUserLoggedIn)

    private val _registerAuthState = MutableStateFlow<AuthStateEvent>(AuthStateEvent.Idle)

    val loginAuthState: StateFlow<AuthStateEvent>
        get() = _loginAuthState

    val registerAuthState: StateFlow<AuthStateEvent>
        get() = _registerAuthState

    init {
        viewModelScope.launch {
            firebaseRepository.signOut()
                .collect {resultAuthSignOut->

                    when(resultAuthSignOut){
                        is ResultAuth.SuccessSignout -> {
                            _loginAuthState.value = AuthStateEvent.UserLogout
                        }
                        is ResultAuth.FailureSignout -> {
                            _loginAuthState.value = AuthStateEvent.Idle
                        }
                    }
                }

        }

    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            firebaseRepository.login(email, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            _loginAuthState.value = AuthStateEvent.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            Log.i(TAG, "${resultAuth.exception}")
                            _loginAuthState.value = AuthStateEvent.Error(resultAuth.exception.message)
                        }
                        is ResultAuth.Loading ->
                            _loginAuthState.value = AuthStateEvent.Logging
                    }
                }
        }
    }

    fun signUpWithEmail(email: String, password: String, name: String){
        viewModelScope.launch {
            firebaseRepository.signUpWithEmail(email, password, name)
                .collect {resultAuth ->
                    when(resultAuth) {
                        is ResultAuth.Loading -> {
                            _registerAuthState.value = AuthStateEvent.Registering
                        }
                        is ResultAuth.Success -> {
                            _registerAuthState.value = AuthStateEvent.SuccessRegister
                        }
                        is ResultAuth.Error -> {
                            _registerAuthState.value = AuthStateEvent.Error(resultAuth.exception.message)
                        }
                }

            }
        }
    }

    fun getAndSetCurrentUser(){
        viewModelScope.launch {
            firebaseRepository.getAndSetCurrentUser()
        }
    }
}