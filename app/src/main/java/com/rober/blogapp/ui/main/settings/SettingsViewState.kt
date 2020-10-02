package com.rober.blogapp.ui.main.settings

import com.rober.blogapp.ui.base.ViewState
import java.lang.Exception

sealed class SettingsViewState : ViewState {

    data class Error(val exception: Exception) : SettingsViewState()
    object Loading : SettingsViewState()
    object Idle : SettingsViewState()
}