package com.rober.blogapp.ui.main.settings

import android.app.Application
import android.content.res.TypedArray
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.R
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import com.rober.blogapp.ui.main.settings.utils.RowsNaming
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val application: Application,
    private val rowsNaming: RowsNaming
) : BaseViewModel<SettingsViewState, SettingsFragmentEvent>() {

    private lateinit var currentUser: User
    private val listOptionsAccountAndOtherOptions = mutableListOf<Option>()

    init {
        viewState = SettingsViewState.Loading
        currentUser = firebaseRepository.getCurrentUser()
    }

    override fun setIntention(event: SettingsFragmentEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsFragmentEvent.SayHello -> viewState = SettingsViewState.Hello

                is SettingsFragmentEvent.LoadSettings -> loadSettings()

                is SettingsFragmentEvent.ClickEventOnSettingsOption -> {
                    val option = listOptionsAccountAndOtherOptions[event.positionAdapter]
                    when (option.text) {
                        rowsNaming.PREFERENCES -> viewState = SettingsViewState.GoToPreferences
                        rowsNaming.REPORTED_POSTS -> viewState = SettingsViewState.GoToReportedPosts
                        rowsNaming.DELETE_ACCOUNT -> viewState = SettingsViewState.AskUserToDisableAccount
                    }
                }

                is SettingsFragmentEvent.DisableAccountAction -> disableAccount()

                is SettingsFragmentEvent.Idle -> {
                    viewState = SettingsViewState.Idle
                }
            }
        }
    }

    private suspend fun loadSettings() {
        job = viewModelScope.launch {

            val user = firebaseRepository.getCurrentUser()
            currentUser = user

        }
        job?.join()

        //Account section
        val listSettingsTextAccount =
            application.applicationContext.resources.getStringArray(R.array.list_settings_fragment_account_text)
                .toList()
        val listSettingsIconTypedArray =
            application.applicationContext.resources.obtainTypedArray(R.array.list_settings_fragment_account_icons)

        //Get list from typed array
        val listSettingsIconAccount =
            obtainListFromTypedArrayResourceId(listSettingsIconTypedArray, listSettingsTextAccount.indices)

        //Get a list of Option
        val listSettingsOptionAccount = getListOptions(listSettingsIconAccount, listSettingsTextAccount)


        //Other options section
        val listSettingsTextOtherOptions =
            application.applicationContext.resources.getStringArray(R.array.list_settings_fragment_other_options_text)
                .toList()
        val settingsIconOtherOptionsTypedArray =
            application.applicationContext.resources.obtainTypedArray(R.array.list_settings_fragment_other_options_icons)

        val listSettingsIconOtherOptions = obtainListFromTypedArrayResourceId(
            settingsIconOtherOptionsTypedArray,
            listSettingsTextOtherOptions.indices
        )

        val listSettingsOptionOtherOptions =
            getListOptions(listSettingsIconOtherOptions, listSettingsTextOtherOptions)

        listOptionsAccountAndOtherOptions.addAll(listSettingsOptionAccount)
        listOptionsAccountAndOtherOptions.addAll(listSettingsOptionOtherOptions)

        val totalNumberPosts = getTotalNumberPosts()

        viewState = SettingsViewState.LoadSettingsMenu(
            listSettingsOptionAccount,
            listSettingsOptionOtherOptions,
            currentUser,
            totalNumberPosts
        )
    }

    private fun getListOptions(listIcons: List<Int>, listString: List<String>): List<Option> {
        val listOptions = mutableListOf<Option>()
        for (index in listString.indices) {
            val option = Option(listIcons[index], listString[index])
            listOptions.add(option)
        }
        return listOptions
    }

    private fun obtainListFromTypedArrayResourceId(typedArray: TypedArray, rangeIcons: IntRange): List<Int> {
        val listOptionsFromTypedArray = mutableListOf<Int>()
        for (index in rangeIcons) {
            val otherOptionIcon = typedArray.getResourceId(index, -1)
            listOptionsFromTypedArray.add(otherOptionIcon)
        }

        return listOptionsFromTypedArray
    }

    private suspend fun getTotalNumberPosts(): Int {
        var totalNumberPosts = 0
        job = viewModelScope.launch {
            firebaseRepository.getTotalNumberPosts(currentUser)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            totalNumberPosts = resultData.data!!
                        }

                        is ResultData.Error -> {
                            viewState =
                                SettingsViewState.Error(Exception("Sorry we found an error getting the number of posts"))
                        }
                    }
                }
        }
        job?.join()

        return totalNumberPosts
    }

    private suspend fun disableAccount() {
        viewState = SettingsViewState.DisableAccountAction

        firebaseRepository.disableAccount()
            .collect { resultData ->
                when (resultData) {
                    is ResultData.Success -> {
                        val hasAccountBeenDisabled = resultData.data!!
                        viewState = if (hasAccountBeenDisabled)
                            SettingsViewState.SuccessDisabledAccount
                        else
                            SettingsViewState.ErrorDisablingAccount
                    }

                    is ResultData.Error -> {
                        viewState = SettingsViewState.ErrorDisablingAccount
                    }
                }
            }
    }
}