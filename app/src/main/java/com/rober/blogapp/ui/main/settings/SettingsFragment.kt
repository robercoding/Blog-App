package com.rober.blogapp.ui.main.settings

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.rober.blogapp.R
import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.settings.adapter.AdapterSettings
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*

@AndroidEntryPoint
class SettingsFragment : BaseFragment<SettingsViewState, SettingsFragmentEvent, SettingsViewModel>(R.layout.fragment_settings), RecyclerViewActionInterface{

    override val viewModel: SettingsViewModel by viewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupViewDesign()
        setupListeners()
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

    override fun setupListeners() {
        settings_material_toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun setupViewDesign(){
        val backArrow = resources.getDrawable(R.drawable.ic_arrow_left, null)
        backArrow.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(requireContext(), R.color.blueTwitter), PorterDuff.Mode.SRC_ATOP)
        settings_material_toolbar.navigationIcon = backArrow
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