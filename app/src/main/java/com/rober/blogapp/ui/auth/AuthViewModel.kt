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

    private val _authState : MutableLiveData<AuthState> = MutableLiveData()

    val authState: LiveData<AuthState>
        get() = _authState

    init {
        viewModelScope.launch {
            firebaseRepository.signOut()
                .collect {resultAuthSignOut->

                    when(resultAuthSignOut){
                        is ResultAuth.SuccessSignout -> {
                            _authState.value = AuthState.UserLogout
                        }
                        is ResultAuth.FailureSignout -> {
                            _authState.value = AuthState.Idle
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
                            _authState  .value = AuthState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            Log.i(TAG, "${resultAuth.exception}")
                            _authState.value = AuthState.Error(resultAuth.exception.message)
                        }
                        is ResultAuth.Loading ->
                            _authState.value = AuthState.Logging
                    }
                }
        }
    }
    
    fun setRegisterIntetion(state: RegisterFragmentEvent){
        when(state){
            is RegisterFragmentEvent.SignUp -> {
                signUpWithEmail(state.email, state.password, state.name)
            }

            is RegisterFragmentEvent.LogIn -> {
                login(state.email, state.password)
            }
        }
    }

    private fun signUpWithEmail(email: String, password: String, name: String){
        viewModelScope.launch {
            firebaseRepository.signUpWithEmail(email, password, name)
                .collect {resultAuth ->
                    when(resultAuth) {
                        is ResultAuth.Loading -> {
                            _authState.value = AuthState.Registering
                        }
                        is ResultAuth.Success -> {
                            _authState.value = AuthState.SuccessRegister(email, password)

                        }
                        is ResultAuth.Error -> {
                            _authState.value = AuthState.Error(resultAuth.exception.message)
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