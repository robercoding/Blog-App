package com.rober.blogapp.ui.main.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseEvent
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.settings.adapter.AdapterSettings
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*

@AndroidEntryPoint
class SettingsFragment : BaseFragment<SettingsViewState, SettingsFragmentEvent, SettingsViewModel>(R.layout.fragment_settings), RecyclerViewActionInterface{

    override val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return LayoutInflater.from(requireContext()).inflate(R.layout.fragment_settings, container, false)
//    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        viewModel.setIntention(SettingsFragmentEvent.SayHello)
        viewModel.setIntention(SettingsFragmentEvent.LoadSettings)
    }

    override fun render(viewState: SettingsViewState) {
        when(viewState){
            is SettingsViewState.LoadSettingsMenu -> renderSettingsView(viewState.user, viewState.listSettings)

        }
        Log.i("SeeSettingsRender", "This renders ${viewState}")
    }

    private fun renderSettingsView(user: User, listSettingsOption: List<Option>){

        settings_text_username.text = "@${user.username}"

        val settingsAdapter = AdapterSettings(listSettingsOption, this)
        val linearLayoutManager = LinearLayoutManager(requireContext())
        val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)

        settings_recycler.apply {
            layoutManager = linearLayoutManager
            adapter = settingsAdapter
            addItemDecoration(decoration)
        }
    }

    override fun displayLoadingFragment(display: Boolean) {
        //
    }


    override fun clickListenerOnPost(positionAdapter: Int) {
        //Nothing
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //Nothing
    }

    override fun clickListenerOnSettings(positionAdapter: Int) {
        Log.i("SeeSettings", "Clicked on settings position = ${positionAdapter}")
    }

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {
        //Nothing
    }
}

sealed class SettingsFragmentEvent {
    object SayHello : SettingsFragmentEvent()
    object LoadSettings: SettingsFragmentEvent()
}