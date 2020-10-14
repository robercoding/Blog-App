package com.rober.blogapp.ui.auth

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.ui.auth.login.LoginFragmentEvent
import com.rober.blogapp.ui.auth.register.RegisterFragmentEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AuthViewModel
@ViewModelInject
constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    private val TAG = "AuthViewModel"

    private val _authState: MutableLiveData<AuthState> = MutableLiveData()

    val authState: LiveData<AuthState>
        get() = _authState

    var alreadySigningUp = false

    init {
        viewModelScope.launch {
            firebaseRepository.signOut()
                .collect { resultAuthSignOut ->

                    when (resultAuthSignOut) {
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
        when (state) {
            is LoginFragmentEvent.LoginByEmail -> {
                loginByEmail(state.email, state.password)
            }

            is LoginFragmentEvent.LoginByUsername -> {
                loginByUsername(state.username, state.password)
            }
        }
    }

    private fun loginByEmail(email: String, password: String) {
        viewModelScope.launch {
            firebaseRepository.loginByEmail(email, password)
                .collect { resultAuth ->
                    when (resultAuth) {
                        is ResultAuth.Success -> {
                            _authState.value = AuthState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            _authState.value = AuthState.Error(resultAuth.exception.message)
                        }
                        is ResultAuth.Loading -> _authState.value = AuthState.Logging
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
                            _authState.value = AuthState.UserLoggedIn
                        }
                        is ResultAuth.Error -> {
                            _authState.value = AuthState.Error(resultAuth.exception.message)
                        }
                        is ResultAuth.Loading -> _authState.value = AuthState.Logging
                    }
                }
        }
    }

    fun setRegisterIntention(event: RegisterFragmentEvent) {
        when (event) {
            is RegisterFragmentEvent.SignUp -> {
                signUpWithEmailCloud(event.username, event.email, event.password)
            }

            is RegisterFragmentEvent.LogIn -> {
                loginByEmail(event.email, event.password)
            }

            is RegisterFragmentEvent.CheckFields -> {
                _authState.value = AuthState.CheckFields
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
                            _authState.value = AuthState.Registering
                        }
                        is ResultAuth.Success -> {
                            _authState.value = AuthState.SuccessRegister(email, password)
                            alreadySigningUp = false
                        }
                        is ResultAuth.Error -> {
                            _authState.value = AuthState.Error(resultAuth.exception.message)
                            alreadySigningUp = false
                        }
                    }
                }
        }
    }

//    private fun signUpWithEmail(username: String, email: String, password: String) {
//        viewModelScope.launch {
//            var isUsernameAvailable = false
//            firebaseRepository.checkIfUsernameAvailable(username)
//                .collect { resultData ->
//                    when (resultData) {
//                        is ResultData.Success -> isUsernameAvailable = resultData.data!!
//                    }
//                }
//
//            if (!isUsernameAvailable) {
//                _authState.value =
//                    AuthState.SetErrorFields(
//                        "Sorry, username is not available, try with other username.",
//                        "",
//                        "",
//                        ""
//                    )
//                return@launch
//            }
//
//            var isEmailAvailable = false
//            Log.i(TAG, "CheckEmailAvailable Now")
//            firebaseRepository.checkIfEmailAlreadyExists(email)
//                .collect { resultData ->
//                    when (resultData) {
//                        is ResultAuth.Success -> isEmailAvailable = true
//                        is ResultAuth.Error -> isEmailAvailable = false
//                    }
//                }
//            Log.i(TAG, "IsEmailAvailable?= $isEmailAvailable")
//            if (!isEmailAvailable) {
//                _authState.value =
//                    AuthState.SetErrorFields(
//                        "",
//                        "Sorry, email is not available, try with other email.",
//                        "",
//                        ""
//                    )
//                return@launch
//            }
//
//            firebaseRepository.signUpWithEmail(email, password, username)
//                .collect { resultAuth ->
//                    when (resultAuth) {
//                        is ResultAuth.Loading -> {
//                            _authState.value = AuthState.Registering
//                        }
//                        is ResultAuth.Success -> {
//                            _authState.value = AuthState.SuccessRegister(email, password)
//
//                        }
//                        is ResultAuth.Error -> {
//                            _authState.value = AuthState.Error(resultAuth.exception.message)
//                        }
//                    }
//                }
//        }
//    }

    private fun setErrorFields(
        isUsernameLengthOk: Boolean,
        isEmailOk: Boolean,
        isPasswordLengthOk: Boolean,
        isPasswordRepeatOk: Boolean
    ) {
        val setErrorFields = AuthState.SetErrorFields("", "", "", "")
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

        _authState.value = setErrorFields
    }

    private fun getAndSetCurrentUser() {
        viewModelScope.launch {
            firebaseRepository.getAndSetCurrentUser()
        }
    }
}