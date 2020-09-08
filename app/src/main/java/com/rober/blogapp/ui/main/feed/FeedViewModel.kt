package com.rober.blogapp.ui.main.feed

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
//import com.rober.blogapp.data.room.repository.RoomRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.MessageUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class FeedViewModel
@ViewModelInject
constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    private val TAG = "FeedViewModel"

    private val _feedState: MutableLiveData<FeedState> = MutableLiveData<FeedState>()

    val feedState: LiveData<FeedState>
        get() = _feedState

    var scrollToPosition = 0
    var user: User? = null
    private var mutableListPosts = mutableListOf<Post>()

    fun setIntention(event: FeedFragmentEvent) {
        when (event) {
            is FeedFragmentEvent.GetUserPicture -> getUserPicture()

            is FeedFragmentEvent.RetrieveInitPosts -> {
                retrieveInitPosts()
            }
            is FeedFragmentEvent.RetrieveOldFeedPosts -> {
                retrieveOldFeedPosts(event.actualRecyclerViewPosition)
            }
            is FeedFragmentEvent.RetrieveNewFeedPosts -> {
                retrieveNewFeedPosts()
            }
            is FeedFragmentEvent.GoToPostDetailsFragment -> {
                goToPostDetails(event.positionAdapter)
            }
            is FeedFragmentEvent.GoToProfileDetailsFragment -> {
                goToProfileDetailsFragment(event.positionAdapter)
            }
            is FeedFragmentEvent.RetrieveSavedLocalPosts -> {
//                retrieveSavedLocalPosts()
            }
            is FeedFragmentEvent.StopRequestOldPosts -> {
                _feedState.value = FeedState.StopRequestOldPosts
            }
            is FeedFragmentEvent.Idle -> {
                _feedState.value = FeedState.Idle
            }
        }
    }

    private fun getUserPicture() {
        user = firebaseRepository.getCurrentUser()

        user?.run {
            _feedState.value = FeedState.SetUserDetails(this)
        }
    }

    private fun retrieveInitPosts() {
        _feedState.value = FeedState.Loading

        val endOfTimeline = firebaseRepository.getEndOfTimeline()

        viewModelScope.launch {
            firebaseRepository.retrieveInitFeedPosts()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            mutableListPosts = resultData.data!!.toMutableList()
                            if (endOfTimeline)
                                addEndOfTimelineToMutableListPosts()

                            _feedState.value = FeedState.SetListPosts(mutableListPosts)
                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message)
                        }
                    }
                }
        }
    }

    private fun retrieveNewFeedPosts() {
        viewModelScope.launch {
            firebaseRepository.retrieveNewFeedPosts()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            if (resultData.data!! == mutableListPosts) {
                                _feedState.value =
                                    FeedState.StopRequestNewPosts(MessageUtil("Sorry, there aren't new posts right now " + ("\ud83d\ude27")))
                            } else {
                                sendNewFeedPosts(resultData.data!!)
                            }
                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message!!)
                        }
                    }
                }
        }
    }

    private fun sendNewFeedPosts(newListPosts: List<Post>) {
        val firstPostBeforeLoadingNewPosts = mutableListPosts[0]

        mutableListPosts = newListPosts.toMutableList() //Is already ordered

        val positionOfTheFirstPost = mutableListPosts.indexOf(firstPostBeforeLoadingNewPosts)
        //Where's user now?
        val endOfTimeLine = firebaseRepository.getEndOfTimeline()
        if (endOfTimeLine)
            addEndOfTimelineToMutableListPosts()

        _feedState.value = FeedState.LoadNewPosts(mutableListPosts, positionOfTheFirstPost - 1)
    }

    private fun retrieveOldFeedPosts(actualRecyclerViewPosition: Int) {
        val endOfTimeline = firebaseRepository.getEndOfTimeline()

        if (!endOfTimeline) {
            _feedState.value = FeedState.LoadingMorePosts
            viewModelScope.launch {
                firebaseRepository.retrieveOldFeedPosts()
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                Log.i("CheckEndOfTimeline", "Retrieve Success Feed Posts $endOfTimeline")
                                sendOldFeedPosts(actualRecyclerViewPosition, resultData.data!!)
                            }
                            is ResultData.Error -> {
                                Log.i("CheckEndOfTimeline", "Retrieve Error Feed Posts $endOfTimeline")
                                _feedState.value = FeedState.Error(resultData.exception.message)
                            }
                        }
                    }
            }
        } else {
            _feedState.value = FeedState.StopRequestOldPosts
        }
    }

    private fun sendOldFeedPosts(actualRecyclerViewPosition: Int, resultData: List<Post>) {
        //SaveScroll Position
        scrollToPosition = actualRecyclerViewPosition

        if (mutableListPosts != resultData || mutableListPosts == resultData) {

            mutableListPosts = resultData.toMutableList()

            val endOfTimeline = firebaseRepository.getEndOfTimeline()

            if (endOfTimeline)
                addEndOfTimelineToMutableListPosts()

            _feedState.value = FeedState.LoadOldPosts(
                mutableListPosts,
                actualRecyclerViewPosition,
                endOfTimeline
            )
        } else {
            _feedState.value = FeedState.StopRequestOldPosts
        }
    }

    private fun addEndOfTimelineToMutableListPosts() {
        mutableListPosts.add(Post(0, "no_more_posts", "", "", "", 0, 0))
    }

    private fun goToPostDetails(positionAdapter: Int) {
        val post = mutableListPosts[positionAdapter]
        _feedState.value = FeedState.GoToPostDetailsFragment(post)
    }

    private fun goToProfileDetailsFragment(positionAdapter: Int) {
        val user_id = mutableListPosts[positionAdapter].user_creator_id
        _feedState.value = FeedState.GoToProfileDetailsFragment(user_id)
    }
}
