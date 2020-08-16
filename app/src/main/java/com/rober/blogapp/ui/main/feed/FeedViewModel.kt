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

    private var scrollToPosition = 0
    private var mutableListPosts = mutableListOf<Post>()

    init {

    }

    fun setIntention(event: FeedFragmentEvent){
        when(event){
            is FeedFragmentEvent.RetrieveInitPosts ->{
                retrievePosts()
            }
            is FeedFragmentEvent.RetrieveOldFeedPosts -> {
                retrievePosts()
            }
            is FeedFragmentEvent.RetrieveNewFeedPosts -> {
                retrieveNewPosts(event.actualRecyclerViewPosition)
            }
            is FeedFragmentEvent.GoToPostDetails -> {
                goToPostDetails(event.positionAdapter)
            }
            is FeedFragmentEvent.Idle ->{
                _feedState.value = FeedState.Idle
            }
        }
    }

    private fun retrievePosts(){
        _feedState.value = FeedState.Loading

        viewModelScope.launch {
            firebaseRepository.retrieveFeedPosts()
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success -> {
                            mutableListPosts = resultData.data!!.toMutableList()
                            _feedState.value = FeedState.SetListPosts(mutableListPosts)
                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message)
                        }
                    }
                }
        }
    }

    private fun retrieveNewPosts(actualRecyclerViewPosition: Int){
        viewModelScope.launch {
            firebaseRepository.retrieveFeedPosts()
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success -> {
                            scrollToPosition = actualRecyclerViewPosition
                            mutableListPosts = resultData.data!!.toMutableList()

                            _feedState.value = FeedState.LoadNewPosts(mutableListPosts,actualRecyclerViewPosition)
                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message)
                        }
                    }
                }
        }
    }

    private fun goToPostDetails(positionAdapter: Int){
        val post = mutableListPosts[positionAdapter]
        _feedState.value = FeedState.GoToPostDetails(post)
    }

}
