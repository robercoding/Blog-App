package com.rober.blogapp.ui.auth.register

sealed class RegisterState {
    object Registering : RegisterState()
    data class SuccessRegister(val email: String, val password: String) : RegisterState()
    object CheckFields : RegisterState()

    object Logging : RegisterState()
    object UserLoggedIn : RegisterState()

    data class SetErrorFields(
        var usernameError: String = "",
        var emailError: String = "",
        var passwordLengthError: String = "",
        var passwordRepeatError: String = ""
    ) : RegisterState()

    //Generics
    object Idle : RegisterState()
    data class Error(val message: String) : RegisterState()
}