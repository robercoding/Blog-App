package com.rober.blogapp.ui.main.settings.preferences

import com.rober.blogapp.ui.base.BaseViewModel

class PreferenceViewModel : BaseViewModel<PreferenceState, PreferenceFragmentEvent>() {

    override fun setIntention(event: PreferenceFragmentEvent) {
        when(event){
            is PreferenceFragmentEvent.TouchDarkThemeOption -> viewState = PreferenceState.ConfigureTheme(event.key, event.value)
        }
    }
}