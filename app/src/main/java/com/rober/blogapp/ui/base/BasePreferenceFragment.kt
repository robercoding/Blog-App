package com.rober.blogapp.ui.base

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.preference.PreferenceFragmentCompat

abstract class BasePreferenceFragment<STATE, EVENT, VM : BaseViewModel<STATE, EVENT>>(val preferencesXml: Int) :
    PreferenceFragmentCompat() {

    abstract val viewModel : VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.viewStates().observe(this, Observer {
            render(it)
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupPreferenceListener()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferencesXml, rootKey)
    }

    abstract fun setupPreferenceListener()

    abstract fun render(viewState: STATE)

}