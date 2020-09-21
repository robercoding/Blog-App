package com.rober.blogapp.ui.main.feed

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
//import com.rober.blogapp.data.room.repository.RoomRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.MessageUtil
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
    private var feedListPosts = mutableListOf<Post>()
    private var feedListUsers = mutableListOf<User>()

    override fun onCleared() {
        super.onCleared()
        Log.i("CheckFirebaseBug", "OnCleared ViewModel")
    }

    fun setIntention(event: FeedFragmentEvent) {
        when (event) {
            is FeedFragmentEvent.GetUserPicture -> getUserPicture()

            is FeedFragmentEvent.RetrieveInitPosts -> {
                retrieveInitPosts()
            }
            is FeedFragmentEvent.RetrieveOldFeedPosts -> {
                if(feedListPosts.size > 30){
                    retrieveOldFeedPosts(event.actualRecyclerViewPosition)
                }
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
            is FeedFragmentEvent.SignOut ->{
                clearListsAndMapsLocalDatabase()
                _feedState.value = FeedState.SignOut
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

        viewModelScope.launch {
            firebaseRepository.retrieveInitFeedPosts()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            feedListPosts = resultData.data!!.toMutableList()
                            Log.i("SetPosts", "ViewModel = here are ${feedListPosts.size}")

                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message)
                        }
                    }
                }

            user?.let { feedListUsers.add(it) }
            if(feedListPosts.size == 0){
                _feedState.value = FeedState.LoadMessageZeroPosts
                return@launch
            }

            firebaseRepository.getUsersFromCurrentFollowings(feedListUsers)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            resultData.data?.let { newListUsers ->
                                feedListUsers.addAll(newListUsers)
                                addEndOfTimelineToMutableListPosts()

                                _feedState.value = FeedState.SetListPosts(feedListPosts, feedListUsers)
                            }
                        }
                    }
                }
        }
    }

    private fun retrieveNewFeedPosts() {
        var newListPosts = listOf<Post>()
        var newListUsers = listOf<User>()

        viewModelScope.launch {
            firebaseRepository.retrieveNewFeedPosts()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            newListPosts = resultData.data!!
                        }
                        is ResultData.Error -> {
                            _feedState.value = FeedState.Error(resultData.exception.message!!)
                        }
                    }
                }
            if(newListPosts.isEmpty()){
                _feedState.value =
                    FeedState.StopRequestNewPosts(MessageUtil("Sorry, there aren't new posts right now " + ("\ud83d\ude27")))
                return@launch
            }

            if (isNewUserInListPosts(newListPosts)) {
                firebaseRepository.getUsersFromCurrentFollowings(feedListUsers)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                resultData.data?.run {
                                    newListUsers = this
                                    sendNewFeedPosts(newListPosts, newListUsers)
                                }
                            }
                        }
                    }
            } else {
                sendNewFeedPosts(newListPosts, newListUsers)
            }

        }
    }

    private fun sendNewFeedPosts(newListPosts: List<Post>, newListUsers: List<User>) {

        if(feedListPosts.size == 0 && newListPosts.size == 0){
            _feedState.value = FeedState.LoadMessageZeroPosts
            return
        }

//        for (post in newListPosts){
//            feedListPosts.add(0, post)
//        }


        var firstPostBeforeLoadingNewPosts: Post? = null
        if(feedListPosts.isNotEmpty())
            firstPostBeforeLoadingNewPosts = feedListPosts[0]

        feedListPosts.addAll(newListPosts.toMutableList())
        feedListPosts = feedListPosts.sortedByDescending { post-> post.created_at }.toMutableList() //Is already ordered
        feedListUsers.addAll(newListUsers.toMutableList())

        var positionOfTheFirstPost = 1
        firstPostBeforeLoadingNewPosts?.also {tempFirstPostBeforeLoadingNewPosts->
             positionOfTheFirstPost = feedListPosts.indexOf(tempFirstPostBeforeLoadingNewPosts)
        }

        //Where's user now?
        _feedState.value = FeedState.LoadNewPosts(feedListPosts, feedListUsers, positionOfTheFirstPost - 1)
    }

    private fun retrieveOldFeedPosts(actualRecyclerViewPosition: Int) {
        val endOfTimeline = firebaseRepository.getEndOfTimeline()
        var newOldListPosts = listOf<Post>()
        var newListUsers = listOf<User>()

        if (!endOfTimeline) {
            _feedState.value = FeedState.LoadingMorePosts
            viewModelScope.launch {
                firebaseRepository.retrieveOldFeedPosts()
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                 resultData.data?.run {
                                    newOldListPosts = this
                                }
                            }
                            is ResultData.Error -> {
                                _feedState.value = FeedState.Error(resultData.exception.message)
                            }
                        }
                    }

                if (isNewUserInListPosts(newOldListPosts)) {
                    firebaseRepository.getUsersFromCurrentFollowings(feedListUsers)
                        .collect { resultData ->
                            when (resultData) {

                                is ResultData.Success -> {
                                    resultData.data?.run {
                                        newListUsers = this
                                        sendOldFeedPosts(actualRecyclerViewPosition, newOldListPosts, newListUsers)
                                    }
                                }
                            }
                        }
                } else {
                    sendOldFeedPosts(actualRecyclerViewPosition, newOldListPosts, newListUsers)
                }
            }

        } else {
            _feedState.value = FeedState.StopRequestOldPosts
        }
    }

    private fun sendOldFeedPosts(actualRecyclerViewPosition: Int, newOldListPosts: List<Post>, newListUsers: List<User>) {
        //SaveScroll Position
        scrollToPosition = actualRecyclerViewPosition

        if (feedListPosts != newOldListPosts || feedListPosts == newOldListPosts) {

            feedListPosts = newOldListPosts.toMutableList()
            feedListUsers.addAll(newListUsers.toMutableList())

            val endOfTimeline = firebaseRepository.getEndOfTimeline()

            if (endOfTimeline)
                addEndOfTimelineToMutableListPosts()


            _feedState.value = FeedState.LoadOldPosts(
                feedListPosts,
                feedListUsers,
                actualRecyclerViewPosition,
                endOfTimeline
            )
        } else {
            _feedState.value = FeedState.StopRequestOldPosts
        }
    }

    private fun isNewUserInListPosts(listPosts: List<Post>): Boolean {
        val listPostsHashSet = listPosts.map { post -> post.userCreatorId }.toHashSet()

        for (user in feedListUsers) {
            val containsUser = listPostsHashSet.indexOf(user.user_id)
            if (containsUser == -1) {
                return true
            }
        }
        return false
    }

    private fun addEndOfTimelineToMutableListPosts() {
        feedListPosts.add(Post(0, "", "", "", "", 0, 0))
    }

    private fun goToPostDetails(positionAdapter: Int) {
        val post = feedListPosts[positionAdapter]
        _feedState.value = FeedState.GoToPostDetailsFragment(post)
    }

    private fun goToProfileDetailsFragment(positionAdapter: Int) {
        val user_id = feedListPosts[positionAdapter].userCreatorId
        _feedState.value = FeedState.GoToProfileDetailsFragment(user_id)
    }

    private fun clearListsAndMapsLocalDatabase(){
        firebaseRepository.clearListsAndMapsLocalDatabase()
    }

    private fun signOut(){
        viewModelScope.launch {
            firebaseRepository.signOut()
                .collect {
                    when(it){
                        is ResultAuth.Success -> _feedState.value = FeedState.SignOut

                        is ResultAuth.Error -> _feedState.value = FeedState.Idle
                    }
                }
        }
    }
}
