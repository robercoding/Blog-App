package com.rober.blogapp.ui.main.search

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.state.DataState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
): ViewModel() {
    private val TAG = "SearchViewModel"

    private val _searchState: MutableLiveData<SearchState> = MutableLiveData()

    val searchState : LiveData<SearchState>
        get() = _searchState

    var user : User? = null

    fun setIntention(event: SearchFragmentEvent){
        when(event){
            is SearchFragmentEvent.LoadUserDetails -> getCurrentUser()

            is SearchFragmentEvent.RetrieveUserByUsername -> {
                searchUsersByUsername(event.searchUsername)
            }

            is SearchFragmentEvent.ReadySearchUser -> {
                _searchState.value = SearchState.ReadySearchUser
            }

            is SearchFragmentEvent.StopSearchUser -> {
                _searchState.value = SearchState.StopSearchUser
            }
        }
    }

    private fun getCurrentUser(){
        user = firebaseRepository.getCurrentUser()

        user?.run {
            _searchState.value = SearchState.SetUserDetails(this)
        }
    }

    private fun searchUsersByUsername(searchUsername: String){
            if(searchUsername.isEmpty()){
                _searchState.value = SearchState.ShowResultSearch(listOf())
            } else{
                viewModelScope.launch {
                    firebaseRepository.getUserByString(searchUsername)
                        .collect {resultData ->
                            Log.i(TAG, "ResultData: $resultData with letter $searchUsername")
                            when(resultData){

                                is ResultData.Success -> {
                                    resultData.data?.let {listUsers ->
                                        if(listUsers.isEmpty())
                                            _searchState.value = SearchState.EmptyResultsSearch(searchUsername)
                                        else
                                            _searchState.value = SearchState.ShowResultSearch(listUsers)
                                    }
                                }

                                is ResultData.Loading -> {
                                    _searchState.value = SearchState.Loading
                                }
                            }
                        }
                }
            }

    }
}