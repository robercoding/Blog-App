package com.rober.blogapp.util.state

sealed class AuthState {


    //Login
    object Logging: AuthState()
    object CheckingUserLoggedIn: AuthState()
    object UserLoggedIn: AuthState()
    object UserLogout: AuthState()
    object SetUserFirebaseSource: AuthState()

    //Register
    object Registering: AuthState()
    object SuccessRegister: AuthState()

    //Generics
    object Idle : AuthState()
    data class Error(val message: String?): AuthState()
}