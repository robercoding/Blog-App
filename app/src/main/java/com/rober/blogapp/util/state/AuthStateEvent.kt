package com.rober.blogapp.util.state

sealed class AuthStateEvent {


    //Login
    object Logging: AuthStateEvent()
    object CheckingUserLoggedIn: AuthStateEvent()
    object UserLoggedIn: AuthStateEvent()
    object UserLogout: AuthStateEvent()

    //Register
    object Registering: AuthStateEvent()
    object SuccessRegister: AuthStateEvent()

    //Generics
    object Idle : AuthStateEvent()
    data class Error(val message: String?): AuthStateEvent()
}