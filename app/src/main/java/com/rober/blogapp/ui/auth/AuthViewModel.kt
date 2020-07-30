package com.rober.blogapp.ui.auth

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.util.state.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class AuthViewModel
@ViewModelInject
constructor(
private val firebaseRepository: FirebaseRepository
): ViewModel() {

    private val _loginAuthState = MutableStateFlow<AuthState>(
        AuthState.CheckingUserLoggedIn)

    private val _registerAuthState = MutableStateFlow<AuthState>(AuthState.Idle)

    val loginAuthState: StateFlow<AuthState>
        get() = _loginAuthState

    val registerAuthState: StateFlow<AuthState>
        get() = _registerAuthState

    init {

        viewModelScope.launch {
            val signOut = firebaseRepository.signOut()
            if(signOut)
                _loginAuthState.value = AuthState.UserLogout
            else
                _loginAuthState.value = AuthState.Idle
        }

    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            firebaseRepository.login(email, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            _loginAuthState.value = AuthState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            _loginAuthState.value = AuthState.Error(resultAuth.exception.toString())
                        }
                        is ResultAuth.Loading ->
                            _loginAuthState.value = AuthState.Logging
                    }
                }
        }
    }

    fun signUpWithEmail(email: String, password: String){
        viewModelScope.launch {
            firebaseRepository.signUpWithEmail(email, password)
                .collect {resultAuth ->
                    when(resultAuth) {
                        is ResultAuth.Loading -> {
                            _registerAuthState.value = AuthState.Registering
                        }
                        is ResultAuth.Success -> {
                            _registerAuthState.value = AuthState.SuccessRegister
                        }
                        is ResultAuth.Error -> {
                            _registerAuthState.value = AuthState.Error(resultAuth.exception.toString())
                        }
                }

            }

        }
    }
}