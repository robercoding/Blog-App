package com.rober.blogapp.ui.main.settings

import android.content.DialogInterface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

            is SettingsViewState.AskUserToDisableAccount -> {
                val materialAlertDialog =
                    MaterialAlertDialogBuilder(requireContext(), R.style.Settings_MaterialDialogTheme)

                materialAlertDialog.setTitle("Delete account")
                    .setMessage("Are you sure that you want to delete the account? Account will be disabled and after 30 days will be deleted")
                    .setBackground(ContextCompat.getDrawable(requireContext(), R.color.primaryBackground))
                    .setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })
                    .setPositiveButton("Disable account", DialogInterface.OnClickListener { dialog, which ->
                        viewModel.setIntention(SettingsFragmentEvent.DisableAccountAction)
                    })
                    .show()
            }

            is SettingsViewState.DisableAccountAction -> {
                displayOpaqueBackground(true)
                displayProgressBar(settings_progress_bar_middle, true)
                customActionOnBackPressed()
            }

            is SettingsViewState.SuccessDisabledAccount -> {
                restoreDefaultOnBackPressed()
                displayProgressBar(settings_progress_bar_middle, false)
                displayOpaqueBackground(false)

                findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
            }

            is SettingsViewState.ErrorDisablingAccount -> {
                displayOpaqueBackground(false)
                displayProgressBar(settings_progress_bar_middle, false)
                displayToast("Sorry, there was an error when trying to delete the account")
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

    //Block back button
    override fun customActionOnBackPressed(action: Int) {
        super.customActionOnBackPressed(action)
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }
}

sealed class SettingsFragmentEvent {
    object SayHello : SettingsFragmentEvent()
    object LoadSettings : SettingsFragmentEvent()

    object DisableAccountAction : SettingsFragmentEvent()

    data class ClickEventOnSettingsOption(val positionAdapter: Int) : SettingsFragmentEvent()
    object Idle : SettingsFragmentEvent()
}