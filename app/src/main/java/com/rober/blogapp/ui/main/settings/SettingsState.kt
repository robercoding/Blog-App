package com.rober.blogapp.ui.main.settings

import java.lang.Exception

sealed class SettingsState {

    data class Error(val exception: Exception) : SettingsState()
    object Loading : SettingsState()
    object Idle : SettingsState()
}