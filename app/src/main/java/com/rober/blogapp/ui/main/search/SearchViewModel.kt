package com.rober.blogapp.ui.main.search

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
): BaseViewModel<SearchState, SearchFragmentEvent>() {

    var user : User? = null
    var listUsers = mutableListOf<User>()

    override fun setIntention(event: SearchFragmentEvent){
        when(event){
            is SearchFragmentEvent.LoadUserDetails -> getCurrentUser()

            is SearchFragmentEvent.RetrieveUserByUsername -> {
                searchUsersByUsername(event.searchUsername)
            }

            is SearchFragmentEvent.ReadySearchUser -> {
                viewState = SearchState.ReadySearchUser
            }

            is SearchFragmentEvent.GoToProfileFragment -> {
                val user = listUsers[event.positionAdapter]
                viewState = SearchState.GoToProfileFragment(user)
            }

            is SearchFragmentEvent.StopSearchUser -> {
                viewState = SearchState.StopSearchUser
            }
        }
    }

    private fun getCurrentUser(){
        user = firebaseRepository.getCurrentUser()

        user?.run {
            viewState = SearchState.SetUserDetails(this)
        }
    }

    private fun searchUsersByUsername(searchUsername: String){
            if(searchUsername.isEmpty()){
                viewState = SearchState.ShowResultSearch(listOf())
            } else{
                viewModelScope.launch {
                    firebaseRepository.getUserByString(searchUsername)
                        .collect {resultData ->
                            when(resultData){
                                is ResultData.Success -> {
                                    resultData.data?.let {resultListUsers ->
                                        if(resultListUsers.isEmpty())
                                            viewState = SearchState.EmptyResultsSearch(searchUsername)
                                        else{
                                            listUsers = resultListUsers.toMutableList()
                                            viewState = SearchState.ShowResultSearch(resultListUsers)
                                        }
                                    }
                                }

                                is ResultData.Loading -> {
                                    viewState = SearchState.Loading
                                }
                            }
                        }
                }
            }

    }
}