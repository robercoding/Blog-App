package com.rober.blogapp.ui.auth

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.ui.auth.login.LoginFragmentEvent
import com.rober.blogapp.ui.auth.register.RegisterFragmentEvent
import com.rober.blogapp.util.state.AuthState
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

    private val _loginAuthState : MutableLiveData<AuthState> = MutableLiveData()

    private val _registerAuthState = MutableStateFlow<AuthState>(AuthState.Idle)

    val loginAuthState: LiveData<AuthState>
        get() = _loginAuthState

    val registerAuthState: StateFlow<AuthState>
        get() = _registerAuthState

    init {
        viewModelScope.launch {
            firebaseRepository.signOut()
                .collect {resultAuthSignOut->

                    when(resultAuthSignOut){
                        is ResultAuth.SuccessSignout -> {
                            _loginAuthState.value = AuthState.UserLogout
                        }
                        is ResultAuth.FailureSignout -> {
                            _loginAuthState.value = AuthState.Idle
                        }
                    }
                }

        }
    }

    fun setLoginIntention(state: LoginFragmentEvent) {
        when(state){
            is LoginFragmentEvent.Login ->{
                login(state.email, state.password)
            }
        }
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            firebaseRepository.login(email, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            _loginAuthState.value = AuthState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            Log.i(TAG, "${resultAuth.exception}")
                            _loginAuthState.value = AuthState.Error(resultAuth.exception.message)
                        }
                        is ResultAuth.Loading ->
                            _loginAuthState.value = AuthState.Logging
                    }
                }
        }
    }
    
    fun setRegisterIntetion(state: RegisterFragmentEvent){
        when(state){
            is RegisterFragmentEvent.SignUp -> {
                signUpWithEmail(state.email, state.password, state.name)
            }
        }
    }

    private fun signUpWithEmail(email: String, password: String, name: String){
        viewModelScope.launch {
            firebaseRepository.signUpWithEmail(email, password, name)
                .collect {resultAuth ->
                    when(resultAuth) {
                        is ResultAuth.Loading -> {
                            _registerAuthState.value = AuthState.Registering
                        }
                        is ResultAuth.Success -> {
                            _registerAuthState.value = AuthState.SuccessRegister
                        }
                        is ResultAuth.Error -> {
                            _registerAuthState.value = AuthState.Error(resultAuth.exception.message)
                        }
                }

            }
        }
    }

    private fun getAndSetCurrentUser(){
        viewModelScope.launch {
            firebaseRepository.getAndSetCurrentUser()
        }
    }
}