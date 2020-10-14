package com.rober.blogapp.ui.auth.register

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RegisterViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
) : BaseViewModel<RegisterState, RegisterFragmentEvent>() {

    private var alreadySigningUp = false

    override fun setIntention(event: RegisterFragmentEvent) {
        when (event) {
            is RegisterFragmentEvent.SignUp -> {
                signUpWithEmailCloud(event.username, event.email, event.password)
            }

            is RegisterFragmentEvent.Login -> {
                loginByEmail(event.email, event.password)
            }

            is RegisterFragmentEvent.CheckFields -> {
                viewState = RegisterState.CheckFields
            }

            is RegisterFragmentEvent.SetErrorFields -> setErrorFields(
                event.isUsernameLengthOk,
                event.isEmailOk,
                event.isPasswordLengthOk,
                event.isPasswordRepeatOk
            )
        }
    }

    private fun signUpWithEmailCloud(username: String, email: String, password: String) {
        if (alreadySigningUp)
            return

        viewModelScope.launch {
            firebaseRepository.signUpWithEmailCloud(email, password, username)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Loading -> {
                            viewState = RegisterState.Registering
                        }
                        is ResultAuth.Success -> {
                            viewState = RegisterState.SuccessRegister(email, password)
                            alreadySigningUp = false
                        }
                        is ResultAuth.Error -> {
                            val message = resultAuth.exception.message
                            setErrorState(message)
                            alreadySigningUp = false
                        }
                    }
                }
        }
    }

    private fun setErrorFields(
        isUsernameLengthOk: Boolean,
        isEmailOk: Boolean,
        isPasswordLengthOk: Boolean,
        isPasswordRepeatOk: Boolean
    ) {
        val setErrorFields = RegisterState.SetErrorFields("", "", "", "")
        if (!isUsernameLengthOk) {
            setErrorFields.usernameError = "Username must contain between 5 and 15 characters"
        }
        if (!isEmailOk) {
            setErrorFields.emailError = "Email must be valid"
        }
        if (!isPasswordLengthOk) {
            setErrorFields.passwordLengthError = "Password length must contain at least 6 characters"
        }
        if (!isPasswordRepeatOk) {
            setErrorFields.passwordRepeatError = "Password must be the same"
        }

        viewState = setErrorFields
    }

    private fun loginByEmail(email: String, password: String) {
        viewModelScope.launch {
            firebaseRepository.loginByEmail(email, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            viewState = RegisterState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            val message = resultAuth.exception.message
                            setErrorState(message)

                        }
                        is ResultAuth.Loading -> viewState = RegisterState.Logging
                    }
                }
        }
    }

    private fun setErrorState(message: String?) {
        viewState = if (message.isNullOrEmpty())
            RegisterState.Error("There was an unknown error when trying to login.")
        else
            RegisterState.Error(message)
    }
}