package com.rober.blogapp.ui.main.settings

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
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
import com.rober.blogapp.ui.main.settings.utils.RowsNaming
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment :
    BaseFragment<SettingsViewState, SettingsFragmentEvent, SettingsViewModel>(R.layout.fragment_settings),
    RecyclerViewActionInterface {

    override val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var rowsNaming: RowsNaming

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.setIntention(SettingsFragmentEvent.LoadSettings)
    }

    override fun render(viewState: SettingsViewState) {
        when (viewState) {
            is SettingsViewState.LoadSettingsMenu -> renderSettingsView(
                viewState.user,
                viewState.listSettingsAccount,
                viewState.listSettingsOptionOtherOptions,
                viewState.totalNumberPosts
            )

            is SettingsViewState.GoToPreferences -> {
                findNavController().navigate(R.id.action_settingsFragment_to_preferencesFragment)
                viewModel.setIntention(SettingsFragmentEvent.Idle)
            }

            is SettingsViewState.GoToReportedPosts -> {
                findNavController().navigate(R.id.action_settingsFragment_to_reportedPostsFragment)
                viewModel.setIntention(SettingsFragmentEvent.Idle)
            }

            is SettingsViewState.Idle -> {
            }
        }
    }

    private fun renderSettingsView(
        user: User,
        listSettingsOptionAccount: List<Option>,
        listSettingsOptionOtherOptions: List<Option>,
        totalNumberPosts: Int
    ) {
        renderSettingsViewAccount(user, listSettingsOptionAccount, totalNumberPosts)
        renderSettingsViewOtherOptions(listSettingsOptionOtherOptions, listSettingsOptionAccount.size)
    }

    private fun renderSettingsViewAccount(
        user: User,
        listSettingsOptionAccount: List<Option>,
        totalNumberPosts: Int
    ) {
        settings_text_username.text = "@${user.username}"

        val settingsAdapter =
            AdapterSettings(listSettingsOptionAccount, this, 0, rowsNaming, totalNumberPosts)
        val linearLayoutManager = LinearLayoutManager(requireContext())
        val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)

        settings_recycler_account.apply {
            layoutManager = linearLayoutManager
            adapter = settingsAdapter
            addItemDecoration(decoration)
        }
    }

    /* There are 2 recyclerviews and I want the adapter position of the second recycler
    // continuing the number of the first recycler view
    // (First position of second recycler view equals to the total size of first Recycler view + position adapter second)
    // When user click on option it will evaluate on the list of the options (Together in viewModel)
     */
    private fun renderSettingsViewOtherOptions(
        listSettingsOptionOtherOptions: List<Option>,
        sumAdapterPositionToOtherOptions: Int
    ) {
        val linearLayoutManager = LinearLayoutManager(requireContext())
        val settingsAdapter =
            AdapterSettings(
                listSettingsOptionOtherOptions,
                this,
                sumAdapterPositionToOtherOptions,
                rowsNaming,
                0
            )
        val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)

        settings_recycler_other_options.apply {
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

    override fun setupViewDesign() {
        val backArrow = resources.getDrawable(R.drawable.ic_arrow_left, null)
        backArrow.colorFilter = PorterDuffColorFilter(
            ContextCompat.getColor(requireContext(), R.color.blueTwitter),
            PorterDuff.Mode.SRC_ATOP
        )
        settings_material_toolbar.navigationIcon = backArrow
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        //Nothing
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //Nothing
    }

    override fun clickListenerOnItem(positionAdapter: Int) {
        viewModel.setIntention(SettingsFragmentEvent.ClickEventOnSettingsOption(positionAdapter))
    }

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {
        //Nothing
    }
}

sealed class SettingsFragmentEvent {
    object SayHello : SettingsFragmentEvent()
    object LoadSettings : SettingsFragmentEvent()

    data class ClickEventOnSettingsOption(val positionAdapter: Int) : SettingsFragmentEvent()
    object Idle : SettingsFragmentEvent()
}