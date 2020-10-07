package com.rober.blogapp.ui.main.settings.preferences.utils

import android.app.Application
import com.rober.blogapp.R
import javax.inject.Inject

class Keys @Inject constructor(val application: Application){
    val PREFERENCE_DARK_THEME = getStringFromResource(R.string.preference_key_dark_theme)

    private fun getStringFromResource(resourceInt: Int): String{
        return application.resources.getString(resourceInt)
    }
}