package com.rober.blogapp.ui.main.search

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

    fun setIntention(event: SearchFragmentEvent){
        when(event){
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

    private fun searchUsersByUsername(searchUsername: String){
            if(searchUsername.isEmpty()){
                _searchState.value = SearchState.ShowResultSearch(listOf())
            } else{
                viewModelScope.launch {
                    firebaseRepository.getUserByString(searchUsername)
                        .collect {resultData ->
                            when(resultData){
                                is ResultData.Success -> {
                                    _searchState.value = SearchState.ShowResultSearch(resultData.data!!)
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