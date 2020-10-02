package com.rober.blogapp.ui.main.settings

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import com.rober.blogapp.ui.base.BaseEvent
import com.rober.blogapp.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : BaseFragment<SettingsViewState, SettingsFragmentEvent, SettingsViewModel>() {

    override val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.setIntention(SettingsFragmentEvent.SayHello)
    }

    override fun render(viewViewState: SettingsViewState) {
        Log.i("SeeSettingsRender", "This renders")
    }

    //    fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_settings, container, false)
//    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//
//    }
}

sealed class SettingsFragmentEvent : BaseEvent{
    object SayHello: SettingsFragmentEvent()
}