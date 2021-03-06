package com.rober.blogapp.ui.auth.login

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.data.network.util.FirebaseUtils
import com.rober.blogapp.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LoginViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val firebaseUtils: FirebaseUtils
) : BaseViewModel<LoginState, LoginFragmentEvent>() {

    var alreadyLogging = false
    override fun setIntention(event: LoginFragmentEvent) {
        when (event) {
            is LoginFragmentEvent.LoginByEmail -> {
                loginByEmail(event.email, event.password)
            }

            is LoginFragmentEvent.LoginByUsername -> {
                loginByUsername(event.username, event.password)
            }

            is LoginFragmentEvent.EnableAccount -> {
                enableAccount()
            }

            is LoginFragmentEvent.CheckIfAlreadyLogin -> {
                val user = firebaseRepository.getCurrentUser()
                if(user.username.isNotEmpty()){
                    viewState = LoginState.UserLoggedIn
                }
            }
        }
    }

    private fun loginByEmail(email: String, password: String) {
        if(alreadyLogging){
            return
        }
        alreadyLogging = true
        viewModelScope.launch {
            firebaseRepository.loginByEmail(email, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            alreadyLogging = false
                            viewState = LoginState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            alreadyLogging = false
                            val message = resultAuth.exception.message
                            setErrorState(message)
                        }
                        is ResultAuth.Loading -> viewState = LoginState.Logging
                    }
                }
        }
    }

    private fun loginByUsername(username: String, password: String) {
        if(alreadyLogging){
            return
        }
        alreadyLogging = true
        viewModelScope.launch {
            firebaseRepository.loginByUsername(username, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            alreadyLogging = false
                            viewState = LoginState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            alreadyLogging = false
                            val message = resultAuth.exception.message
                            setErrorState(message)
                        }
                        is ResultAuth.Loading -> viewState = LoginState.Logging
                    }
                }
        }
    }

    private fun enableAccount() {
        viewModelScope.launch {
            firebaseRepository.enableAccount()
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Loading -> viewState = LoginState.EnablingAccount

                        is ResultAuth.Success -> viewState =
                            LoginState.EnabledAccount("Congratulations, your account has been enabled! Wait a moment we'll login you in a second!")

                        is ResultAuth.Error -> {
                            val message = resultAuth.exception.message
                            setErrorState(message)
                        }
                    }
                }
        }
    }

    private fun setErrorState(message: String?) {
        if (message.isNullOrEmpty()) {
            viewState = LoginState.Error("There was an unknown error when trying to login.")
        }
        viewState = when (message) {
            firebaseUtils.ACCOUNT_DISABLED_LESS_30_DAYS_MESSAGE -> LoginState.OfferEnableAccount(message)
            firebaseUtils.ACCOUNT_DISABLED_MORE_30_DAYS_MESSAGE -> LoginState.Error(message)
            firebaseUtils.ERROR_ENABLING_ACCOUNT_MESSAGE -> LoginState.Error(message)
            else -> LoginState.Error(message!!)
        }
    }
}