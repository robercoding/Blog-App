package com.rober.blogapp.data.network.util

import javax.inject.Inject

class FirebaseErrors @Inject constructor() {

    val NO_ERROR = 100
    val GENERAL_ERROR = 101
    val VALUE_NOT_FOUND = 102

    //Authentication error codes Firebase
    val ERROR_NOT_FOUND = "ERROR_USER_NOT_FOUND"
    val ERROR_WRONG_PASSWORD = "ERROR_WRONG_PASSWORD"
    val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"

    val ERROR_NOT_FOUND_CODE = 1
    val ERROR_WRONG_PASSWORD_CODE = 2

    //Disabled error codes
    val ACCOUNT_DISABLED_LESS_30_DAYS = 3
    val ACCOUNT_DISABLED_MORE_30_DAYS = 4
    val ACCOUNT_NOT_DISABLED = 5

    //NODE-JS S


//    //Only response to errors.
//    //Response that aren't errors are not included here.
//    var nodeJsResponse = listOf(
//        listOf("no-exists", "Sorry, there"),
//        listOf("")
//
//    )
}