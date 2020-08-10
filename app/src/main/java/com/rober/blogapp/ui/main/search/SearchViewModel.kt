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
import com.rober.blogapp.util.state.SearchState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SearchViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
): ViewModel() {
    private val TAG = "SearchViewModel"

    private val _usersList : MutableLiveData<DataState<List<User>>> = MutableLiveData()

    private val _viewState: MutableLiveData<SearchState> = MutableLiveData()

    val viewState : LiveData<SearchState>
        get() = _viewState

    val userList: LiveData<DataState<List<User>>>
        get() = _usersList

    fun setIntention(event: SearchFragmentEvent){
        when(event){
            is SearchFragmentEvent.RetrieveUserByUsername -> {

                if(event.searchUsername.isEmpty())
                    _usersList.value = DataState.Success(listOf())
                else
                    viewModelScope.launch {
                        firebaseRepository.getUserByString(event.searchUsername)
                            .collect {resultData ->
                                when(resultData){
                                    is ResultData.Success -> {
                                        _usersList.value = DataState.Success(resultData.data!!)
                                    }
                                }
                            }
                    }
            }

            is SearchFragmentEvent.ReadySearchUser -> {
                _viewState.value = SearchState.ReadySearchUser
            }

            is SearchFragmentEvent.StopSearchUser -> {
            _viewState.value = SearchState.StopSearchUser
        }
        }
    }
}