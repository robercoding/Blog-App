package com.rober.blogapp.ui.main.post

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.util.state.DataState
import com.rober.blogapp.util.state.PostAddState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PostViewModel @ViewModelInject constructor(
    firebaseRepository: FirebaseRepository
): ViewModel() {

    val _statePost: MutableLiveData<PostAddState> = MutableLiveData()

    val statePost : LiveData<PostAddState>
        get() = _statePost

    init {
        viewModelScope.launch {
            firebaseRepository.getCurrentUser()
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success -> {
                            _statePost.value = PostAddState.Success(resultData.data)
                        }

                        is ResultData.Error -> {
                            _statePost.value = PostAddState.Error(resultData.exception.message.toString(), null)
                        }
                    }

                }
        }
    }





}