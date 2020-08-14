package com.rober.blogapp.ui.auth

sealed class AuthState {


    //Login
    object Logging: AuthState()
    object CheckingUserLoggedIn: AuthState()
    object UserLoggedIn: AuthState()
    object UserLogout: AuthState()
    object SetUserFirebaseSource: AuthState()

    //Register
    object Registering: AuthState()
    data class SuccessRegister(val email: String, val password: String): AuthState()

    //Generics
    object Idle : AuthState()
    data class Error(val message: String?): AuthState()
}