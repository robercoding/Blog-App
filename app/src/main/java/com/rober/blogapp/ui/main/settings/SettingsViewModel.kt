package com.rober.blogapp.ui.main.settings

import androidx.hilt.lifecycle.ViewModelInject
import com.rober.blogapp.ui.base.BaseViewModel

class SettingsViewModel @ViewModelInject constructor() : BaseViewModel<SettingsViewState, SettingsFragmentEvent>(){

    override fun setIntention(event: SettingsFragmentEvent) {
    }

}