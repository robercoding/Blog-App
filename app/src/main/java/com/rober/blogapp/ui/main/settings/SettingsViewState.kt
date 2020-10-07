package com.rober.blogapp.ui.main.settings

import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.ViewState
import java.lang.Exception

sealed class SettingsViewState {

    object Hello : SettingsViewState()
    data class LoadSettingsMenu(val listSettingsAccount: List<Option>, val listSettingsOptionOtherOptions: List<Option>, val user: User): SettingsViewState()

    object GoToPreferences : SettingsViewState()

    data class Error(val exception: Exception) : SettingsViewState()
    object Loading : SettingsViewState()
    object Idle : SettingsViewState()
}