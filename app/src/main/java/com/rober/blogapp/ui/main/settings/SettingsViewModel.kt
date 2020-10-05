package com.rober.blogapp.ui.main.settings

import android.app.Application
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.R
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class SettingsViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val application: Application
) : BaseViewModel<SettingsViewState, SettingsFragmentEvent>() {

    private lateinit var currentUser: User

    init {
        viewState = SettingsViewState.Loading
    }

    override fun setIntention(event: SettingsFragmentEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsFragmentEvent.SayHello -> viewState = SettingsViewState.Hello
                is SettingsFragmentEvent.LoadSettings -> loadSettings()
            }
        }
    }

    private suspend fun loadSettings() {
        val job = viewModelScope.launch {

            val user = firebaseRepository.getCurrentUser()
            currentUser = user

        }
        job.join()

        val listSettingsString = application.applicationContext.resources.getStringArray(R.array.list_settings_fragment_text).toList()
        val listSettingsIconTypedArray = application.applicationContext.resources.obtainTypedArray(R.array.list_settings_fragment_icons)

        Log.i("SeeSize", "String Size = ${listSettingsString.size}")

        val listSettingsIcon = mutableListOf<Int>()
        for(index in 0..listSettingsString.size-1){
            Log.i("SeeSize", "Index Settings string size =  ${index}")
            listSettingsIcon.add(listSettingsIconTypedArray.getResourceId(index, -1))
        }

        Log.i("SeeSize", "String Size = ${listSettingsString.size}")
        Log.i("SeeSize", "Icon Size = ${listSettingsIcon.size}")

        val listSettingsOption = mutableListOf<Option>()
        for(index in 0..listSettingsIcon.size-1){
            Log.i("SeeSize", "index Setting icon size = ${index}")
            val text = listSettingsString[index]
            val icon = listSettingsIcon[index]
            val option = Option(listSettingsIcon[index], listSettingsString[index])
            listSettingsOption.add(option)
        }

        viewState = SettingsViewState.LoadSettingsMenu(listSettingsOption, currentUser)
    }
}