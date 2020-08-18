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
    @Assisted val stateHandle: SavedStateHandle
) : ViewModel() {
    private val TAG = "FeedViewModel"

    private val _feedState: MutableLiveData<FeedState> = MutableLiveData<FeedState>()

    val feedState: LiveData<FeedState>
        get() = _feedState

    var scrollToPosition = 0
    private var mutableListPosts = getMutableListPosts()
    private var endOfTimeline: LiveData<Boolean> = getEndOfTimeline()

    companion object {
        private val END_OF_TIMELINE = "endOfTimeline"
        private val MUTABLE_LIST_POSTS = "mutableListPosts"
    }

    private fun getMutableListPosts(): LiveData<MutableList<Post>> {
        return stateHandle.getLiveData(MUTABLE_LIST_POSTS, mutableListOf())
    }

    private fun saveMutableListPosts(mutableListPosts: MutableList<Post>) {
        stateHandle.set(MUTABLE_LIST_POSTS, mutableListPosts)
    }

    private fun getEndOfTimeline(): LiveData<Boolean> {
        return stateHandle.getLiveData(END_OF_TIMELINE, false)
    }

    fun saveEndOfTimeline(endOfTimeline: Boolean) {
        stateHandle.set(END_OF_TIMELINE, endOfTimeline)
    }

    override fun onCleared() {
        super.onCleared()
        Log.i("CheckingLifecycle", "${endOfTimeline.value}")
        Log.i(
            "CheckingLifecycle",
            "CLEARED Contains any? = ${stateHandle.contains(END_OF_TIMELINE)} OR ${stateHandle.contains(
                MUTABLE_LIST_POSTS
            )}"
        )
    }

    init {
        Log.i("CheckingLifecycle", "Init")
//        mutableListPosts = stateHandle.get<MutableList<Post>>("mutableListPosts")
//        endOfTimeline = stateHandle.get("endOfTimeline")!!

//        if(endOfTimeline.value == null)
//            saveEndOfTimeline(false)
        Log.i(
            "CheckingLifecycle",
            "INIT Contains any? = ${stateHandle.contains(END_OF_TIMELINE)} OR ${stateHandle.contains(
                MUTABLE_LIST_POSTS
            )}"
        )
        Log.i("CheckingLifecycle", "MutableList is null? = ${getMutableListPosts().value}")
        Log.i("CheckingLifecycle", "End of timeline is null? = ${getEndOfTimeline().value}")
        //Log.i("CheckingLifecycle", "End of timeline = ${endOfTimeline.value}")
    }

    fun setIntention(event: FeedFragmentEvent) {
        when (event) {
            is FeedFragmentEvent.RetrieveInitPosts -> {
                Log.i("EndOfTimeline", "Set endoftimeline")
                saveEndOfTimeline(false)
                //saveEndOfTimeline(endOfTimeline = false)
                retrieveInitPosts()
            }
            is FeedFragmentEvent.RetrieveOldFeedPosts -> {
                Log.i("EndOfTimeline", "We just checking how is endoftimeline = ${endOfTimeline}")
                retrieveOldFeedPosts(event.actualRecyclerViewPosition)
            }
            is FeedFragmentEvent.RetrieveNewFeedPosts -> {
            }
            is FeedFragmentEvent.GoToPostDetails -> {
                goToPostDetails(event.positionAdapter)
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

    private fun retrieveInitPosts() {
        _feedState.value = FeedState.Loading

        viewModelScope.launch {
            firebaseRepository.retrieveInitPosts()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            saveMutableListPosts(resultData.data!!.toMutableList())
                            _feedState.value = FeedState.SetListPosts(mutableListPosts.value!!)
                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message)
                        }
                    }
                }
        }
    }

    private fun retrieveOldFeedPosts(actualRecyclerViewPosition: Int) {
        Log.i(
            "CheckingLifecycle",
            "End of timeline is null when retrieve? = ${endOfTimeline.value}"
        )
        //Log.i("EndOfTimeline", "Checking if we can retrieve more posts ${endOfTimeline}")
        Log.i(
            "EndOfTimeline",
            "Does mutablelist contains no_more_posts = ${mutableListPosts.value?.find { post -> post.post_id == "no_more_posts" }}"
        )
        if (!endOfTimeline.value!!) {
            viewModelScope.launch {
                firebaseRepository.retrieveOldFeedPosts()
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                sendOldFeedPosts(actualRecyclerViewPosition, resultData.data!!)
                            }
                            is ResultData.Error -> {
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
        saveEndOfTimeline(containsEndOfTimeline(resultData))
        Log.i(TAG, "Old feed post check value now ${endOfTimeline.value}")
        if (mutableListPosts != resultData.toMutableList()) {
            val tempEndOfTimeline = containsEndOfTimeline(resultData)
            //endOfTimeline = tempEndOfTimeline
            Log.i("EndOfTimeline", "Contains endoftl? ${endOfTimeline}")

            saveMutableListPosts(resultData.toMutableList())

            _feedState.value = FeedState.LoadOldPosts(
                mutableListPosts.value!!,
                actualRecyclerViewPosition,
                endOfTimeline.value!!
            )
        } else {
            _feedState.value = FeedState.StopRequestOldPosts
        }
    }

    private fun containsEndOfTimeline(resultListPosts: List<Post>): Boolean {
        val findPost = resultListPosts.find { post -> post.post_id == "no_more_posts" }

        return findPost != null
    }

//    private fun retrieveSavedLocalPosts() {
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
//    }

    private fun goToPostDetails(positionAdapter: Int) {
        val post = mutableListPosts.value!![positionAdapter]
        _feedState.value = FeedState.GoToPostDetails(post)
    }

}
