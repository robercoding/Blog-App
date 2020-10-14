package com.rober.blogapp.ui.auth.login

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LoginViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
) : BaseViewModel<LoginState, LoginFragmentEvent>() {

    override fun setIntention(event: LoginFragmentEvent) {
        when (event) {
            is LoginFragmentEvent.LoginByEmail -> {
                loginByEmail(event.email, event.password)
            }

            is LoginFragmentEvent.LoginByUsername -> {
                loginByUsername(event.username, event.password)
            }
        }
    }

    private fun loginByEmail(email: String, password: String) {
        viewModelScope.launch {
            firebaseRepository.loginByEmail(email, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            viewState = LoginState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            val message = resultAuth.exception.message
                            viewState = if (message.isNullOrEmpty())
                                LoginState.Error("There was an unknown error when trying to login.")
                            else
                                LoginState.Error(message)
                        }
                        is ResultAuth.Loading -> viewState = LoginState.Logging
                    }
                }
        }
    }

    private fun loginByUsername(username: String, password: String) {
        viewModelScope.launch {
            firebaseRepository.loginByUsername(username, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            viewState = LoginState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            val message = resultAuth.exception.message
                            viewState = if (message.isNullOrEmpty())
                                LoginState.Error("There was an unknown error when trying to login.")
                            else
                                LoginState.Error(message)
                        }
                        is ResultAuth.Loading -> viewState = LoginState.Logging
                    }
                }
        }
    }
}