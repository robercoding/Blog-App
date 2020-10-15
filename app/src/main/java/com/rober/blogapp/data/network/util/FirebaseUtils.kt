package com.rober.blogapp.data.network.util

import javax.inject.Inject

class FirebaseUtils @Inject constructor() {
    //General message error
    var GENERAL_MESSAGE_ERROR = "Sorry there was an error in our servers"

    //Authentication error codes Firebase Message
    val ERROR_NOT_FOUND_MESSAGE = "Sorry, we couldn't find an user in our servers."
    val ERROR_WRONG_PASSWORD = "Sorry, there's an error with credentials."
//    val ERROR_USER_DISABLED = "Sorry, user has been disabled"


    //Messages
    val ACCOUNT_DISABLED_LESS_30_DAYS_MESSAGE =
        "Woah! Looks like you want to login after disabling the account before the 30 days have passed. Do you want to enable it again?"
    val ACCOUNT_DISABLED_MORE_30_DAYS_MESSAGE =
        "Your account has been deleted as you requested it and we can't get the information again."
}