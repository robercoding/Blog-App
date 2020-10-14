package com.rober.blogapp.ui.auth.login

sealed class LoginState {

    object Logging : LoginState()
    object CheckingUserLoggedIn : LoginState()
    object UserLoggedIn : LoginState()
    object UserLogout : LoginState()

    object Idle : LoginState()
    data class Error(val message: String) : LoginState()
}