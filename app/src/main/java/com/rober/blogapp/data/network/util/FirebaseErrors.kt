package com.rober.blogapp.data.network.util

class FirebaseErrors {
    var generalError = "Sorry there was an error in our servers"
    var authErrors = listOf(
        listOf("ERROR_USER_NOT_FOUND", "Sorry, credentials are incorrect, try again"),
        listOf("ERROR_WRONG_PASSWORD", "Sorry, credentials are incorrect, try again"),
        listOf("ERROR_USER_TOKEN_EXPIRE", "Lo sentimos, el token ha expirado"),
        listOf("ERROR_INVALID_USER_TOKEN", "Lo sentimos, el token no es correcto"),
        listOf("ERROR_USER_DISABLED", "Lo sentimos, el usuario ha sido desactivado")
    )
}