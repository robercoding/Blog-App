package com.rober.blogapp.ui.main.feed

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
//import com.rober.blogapp.data.room.repository.RoomRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.util.state.FeedState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FeedViewModel
@ViewModelInject
constructor(
    private val firebaseRepository: FirebaseRepository,
    @Assisted savedStateHandle: SavedStateHandle
): ViewModel(){
    private val TAG = "FeedViewModel"

    private val _feedState: MutableLiveData<FeedState<List<Post>>> = MutableLiveData<FeedState<List<Post>>>()

    val feedState: LiveData<FeedState<List<Post>>>
        get() =_feedState


    init {
        retrievePosts(false)
    }

    fun setIntention(event: FeedFragmentEvent){
        when(event){
            is FeedFragmentEvent.RetrieveFeedPosts -> {
                retrievePosts(event.morePosts)
            }
        }
    }

    fun retrievePosts(morePosts: Boolean){
        _feedState.value = FeedState.GettingPostState
        viewModelScope.launch {
            firebaseRepository.retrieveFeedPosts(morePosts)
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success -> {
                            Log.i(TAG, "Data: ${resultData.data}")
                            if(resultData.data != null){
                                _feedState.value = FeedState.SuccessListPostState(resultData.data)
                            }
                        }
                        is ResultData.Error -> {
                            if(resultData.exception.message != null)
                                _feedState.value = FeedState.Error(resultData.exception.message)

                        }
                    }
                }
        }
    }

}
