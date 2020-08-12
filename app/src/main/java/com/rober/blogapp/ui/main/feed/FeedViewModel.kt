package com.rober.blogapp.ui.main.feed

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
//import com.rober.blogapp.data.room.repository.RoomRepository
import com.rober.blogapp.entity.Post
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FeedViewModel
@ViewModelInject
constructor(
    private val firebaseRepository: FirebaseRepository,
    @Assisted savedStateHandle: SavedStateHandle
): ViewModel(){
    private val TAG = "FeedViewModel"

    private val _feedState: MutableLiveData<FeedState> = MutableLiveData<FeedState>()

    val feedState: LiveData<FeedState>
        get() =_feedState

    init {}

    fun setIntention(event: FeedFragmentEvent){
        when(event){
            is FeedFragmentEvent.RetrieveOldFeedPosts -> {
                retrievePosts()
            }
            is FeedFragmentEvent.RetrieveNewFeedPosts -> {
                retrievePosts()
            }
            is FeedFragmentEvent.Idle ->{
                _feedState.value = FeedState.Idle
            }
        }
    }

    fun retrievePosts(){
        _feedState.value = FeedState.Loading

        viewModelScope.launch {
            firebaseRepository.retrieveFeedPosts()
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success -> {
                            _feedState.value = FeedState.SetListPosts(resultData.data!!)
                            Log.i(TAG, "Result Feed: ${resultData.data!!}")
                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message)
                        }
                    }
                }
        }
    }

}
