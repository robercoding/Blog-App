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
import com.rober.blogapp.ui.base.BaseViewModel
import com.rober.blogapp.util.MessageUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FeedViewModel
@ViewModelInject
constructor(
    private val firebaseRepository: FirebaseRepository
) : BaseViewModel<FeedState, FeedFragmentEvent>() {

    var scrollToPosition = 0
    var user: User? = null
    private var feedListPosts = mutableListOf<Post>()
    private var feedListUsers = mutableListOf<User>()

    override fun setIntention(event: FeedFragmentEvent) {
        when (event) {
            is FeedFragmentEvent.GetUserPicture -> getUserPicture()

            is FeedFragmentEvent.RetrieveInitPosts -> {
                retrieveInitPosts()
            }
            is FeedFragmentEvent.RetrieveOldFeedPosts -> {
                if (feedListPosts.size > 30) {
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
                viewState = FeedState.StopRequestOldPosts
            }
            is FeedFragmentEvent.Idle -> {
                viewState = FeedState.Idle
            }
            is FeedFragmentEvent.SignOut -> {
                signOut()
            }
        }
    }

    private fun getUserPicture() {
        user = firebaseRepository.getCurrentUser()

        user?.run {
            viewState = FeedState.SetUserDetails(this)
        }
    }

    private fun retrieveInitPosts() {
        viewState = FeedState.Loading

        viewModelScope.launch {
            firebaseRepository.retrieveInitFeedPosts()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            feedListPosts = resultData.data!!.toMutableList()
                            Log.i("SetPosts", "ViewModel = here are ${feedListPosts.size}")

                        }
                        is ResultData.Error -> {
                            viewState = FeedState.Error(resultData.exception.message)
                        }
                    }
                }

            user?.let { feedListUsers.add(it) }
            if (feedListPosts.size == 0) {
                viewState = FeedState.LoadMessageZeroPosts
                return@launch
            }

            firebaseRepository.getUsersFromCurrentFollowings(feedListUsers)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            resultData.data?.let { newListUsers ->
                                feedListUsers.addAll(newListUsers)
                                addEndOfTimelineToMutableListPosts()

                                viewState = FeedState.SetListPosts(feedListPosts, feedListUsers)
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
                            viewState = FeedState.Error(resultData.exception.message!!)
                        }
                    }
                }
            if (newListPosts.isEmpty()) {
                viewState =
                    FeedState.StopRequestNewPosts(MessageUtil("Sorry, there aren't new posts right now "))
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

        if (feedListPosts.size == 0 && newListPosts.size == 0) {
            viewState = FeedState.LoadMessageZeroPosts
            return
        }

//        for (post in newListPosts){
//            feedListPosts.add(0, post)
//        }


        var firstPostBeforeLoadingNewPosts: Post? = null
        if (feedListPosts.isNotEmpty())
            firstPostBeforeLoadingNewPosts = feedListPosts[0]

        feedListPosts.addAll(newListPosts.toMutableList())
        feedListPosts =
            feedListPosts.sortedByDescending { post -> post.createdAt }.toMutableList() //Is already ordered
        feedListUsers.addAll(newListUsers.toMutableList())

        var positionOfTheFirstPost = 1
        firstPostBeforeLoadingNewPosts?.also { tempFirstPostBeforeLoadingNewPosts ->
            positionOfTheFirstPost = feedListPosts.indexOf(tempFirstPostBeforeLoadingNewPosts)
        }

        //Where's user now?
        viewState = FeedState.LoadNewPosts(feedListPosts, feedListUsers, positionOfTheFirstPost - 1)
    }

    private fun retrieveOldFeedPosts(actualRecyclerViewPosition: Int) {
        val endOfTimeline = firebaseRepository.getEndOfTimeline()
        var newOldListPosts = listOf<Post>()
        var newListUsers = listOf<User>()

        if (!endOfTimeline) {
            viewState = FeedState.LoadingMorePosts
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
                                viewState = FeedState.Error(resultData.exception.message)
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
                                        sendOldFeedPosts(
                                            actualRecyclerViewPosition,
                                            newOldListPosts,
                                            newListUsers
                                        )
                                    }
                                }
                            }
                        }
                } else {
                    sendOldFeedPosts(actualRecyclerViewPosition, newOldListPosts, newListUsers)
                }
            }

        } else {
            viewState = FeedState.StopRequestOldPosts
        }
    }

    private fun sendOldFeedPosts(
        actualRecyclerViewPosition: Int,
        newOldListPosts: List<Post>,
        newListUsers: List<User>
    ) {
        //SaveScroll Position
        scrollToPosition = actualRecyclerViewPosition

        if (feedListPosts != newOldListPosts || feedListPosts == newOldListPosts) {

            feedListPosts = newOldListPosts.toMutableList()
            feedListUsers.addAll(newListUsers.toMutableList())

            val endOfTimeline = firebaseRepository.getEndOfTimeline()

            if (endOfTimeline)
                addEndOfTimelineToMutableListPosts()


            viewState = FeedState.LoadOldPosts(
                feedListPosts,
                feedListUsers,
                actualRecyclerViewPosition,
                endOfTimeline
            )
        } else {
            viewState = FeedState.StopRequestOldPosts
        }
    }

    private fun isNewUserInListPosts(listPosts: List<Post>): Boolean {
        val listPostsHashSet = listPosts.map { post -> post.userCreatorId }.toHashSet()

        for (user in feedListUsers) {
            val containsUser = listPostsHashSet.indexOf(user.userId)
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
        viewState = FeedState.GoToPostDetailsFragment(post)
    }

    private fun goToProfileDetailsFragment(positionAdapter: Int) {
        val userId = feedListPosts[positionAdapter].userCreatorId
        viewState = FeedState.GoToProfileDetailsFragment(userId)
    }

    private fun clearSignout() {
        firebaseRepository.clearListsAndMapsLocalDatabase()
        firebaseRepository.clearFirebaseSource()
    }

    private fun signOut() {
        clearSignout()

        viewModelScope.launch {
            firebaseRepository.signOut()
                .collect {
                    when (it) {
                        is ResultAuth.Success -> viewState = FeedState.SignOut

                        is ResultAuth.Error -> viewState = FeedState.Idle
                    }
                }
        }
    }
}
