package com.rober.blogapp.ui.main.post

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.util.state.DataState
import com.rober.blogapp.util.state.PostAddState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PostAddViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
): ViewModel() {

    private val _statePost: MutableLiveData<PostAddState> = MutableLiveData()

    val statePost : LiveData<PostAddState>
        get() = _statePost

    init {
        //Get the current username
//        viewModelScope.launch {
//            firebaseRepository.getCurrentUser()
//                .collect {resultData ->
//                    when(resultData){
//                        is ResultData.Success -> {
//                            _statePost.value = PostAddState.Success(resultData.data)
//                        }
//
//                        is ResultData.Error -> {
//                            _statePost.value = PostAddState.Error(resultData.exception.message.toString(), null)
//                        }
//                    }
//
//                }
//        }
    }

    fun setIntention(event: PostAddEvent){
        when(event) {
            is PostAddEvent.savePost -> {
                viewModelScope.launch {
                    firebaseRepository.savePost(event.post)
                        .collect {resultData ->
                            when(resultData){
                                is ResultData.Success ->{
                                    _statePost.value = PostAddState.Success(null)
                                }
                                is ResultData.Error -> {
                                    if(resultData.exception.message != null)
                                    _statePost.value = PostAddState.Error(resultData.exception, "")
                                }
                                is ResultData.Loading -> {
                                    _statePost.value = PostAddState.Idle
                                }
                            }
                        }
                }
            }
        }

    }





}