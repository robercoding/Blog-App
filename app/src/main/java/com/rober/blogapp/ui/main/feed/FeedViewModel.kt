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
) : ViewModel() {
    private val TAG = "FeedViewModel"

    private val _feedState: MutableLiveData<FeedState> = MutableLiveData<FeedState>()

    val feedState: LiveData<FeedState>
        get() = _feedState

    private var scrollToPosition = 0
    private var mutableListPosts = mutableListOf<Post>()

    init {

    }

    fun setIntention(event: FeedFragmentEvent) {
        when (event) {
            is FeedFragmentEvent.RetrieveInitPosts -> {
                retrieveInitPosts()
            }
            is FeedFragmentEvent.RetrieveOldFeedPosts -> {
                retrieveOldFeedPosts(event.actualRecyclerViewPosition)
            }
            is FeedFragmentEvent.RetrieveNewFeedPosts -> {
            }
            is FeedFragmentEvent.GoToPostDetails -> {
                goToPostDetails(event.positionAdapter)
            }

            is FeedFragmentEvent.RetrieveSavedLocalPosts -> {
                retrieveSavedLocalPosts()
            }
            is FeedFragmentEvent.Idle -> {
                _feedState.value = FeedState.Idle
            }
        }
    }

    private fun retrieveInitPosts() {
        _feedState.value = FeedState.Loading

        viewModelScope.launch {
            firebaseRepository.retrieveInitPosts()
                .collect { resultData ->
                    when (resultData) {
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

    private fun retrieveOldFeedPosts(actualRecyclerViewPosition: Int) {
        viewModelScope.launch {
            firebaseRepository.retrieveOldFeedPosts()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            scrollToPosition = actualRecyclerViewPosition
                            if (mutableListPosts != resultData.data!!.toMutableList()) {
                                mutableListPosts = resultData.data.toMutableList()

                                _feedState.value = FeedState.LoadOldPosts(
                                    mutableListPosts,
                                    actualRecyclerViewPosition
                                )
                            } else {
                                Log.i(TAG, "They're the same sadly ${mutableListPosts.size} and ${resultData.data.size}")

                                _feedState.value = FeedState.StopRequestOldPosts
                            }

                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message)
                        }
                    }
                }
        }
    }

    private fun retrieveSavedLocalPosts() {
//        viewModelScope.launch {
//            firebaseRepository.retrieveSavedLocalPosts()
//                .collect { resultData ->
//                    when (resultData) {
//                        is ResultData.Success -> {
//                            mutableListPosts = resultData.data!!.toMutableList()
//                            _feedState.value = FeedState.SetListPosts(mutableListPosts)
//                        }
//                        is ResultData.Error -> {
//                            _feedState.value = FeedState.RequestOnlinePosts
//                        }
//                    }
//                }
//        }
    }

    private fun goToPostDetails(positionAdapter: Int) {
        val post = mutableListPosts[positionAdapter]
        _feedState.value = FeedState.GoToPostDetails(post)
    }

}
