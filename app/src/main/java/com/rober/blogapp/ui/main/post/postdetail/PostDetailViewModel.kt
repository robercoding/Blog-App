package com.rober.blogapp.ui.main.post.postdetail

import android.app.Application
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.R
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.post.postdetail.utils.ArrayUtils
import com.rober.blogapp.ui.main.post.postdetail.utils.OptionsUtils
import com.rober.blogapp.ui.main.post.postdetail.utils.OptionsUtils.DELETE_POST
import com.rober.blogapp.ui.main.post.postdetail.utils.OptionsUtils.EDIT_POST
import com.rober.blogapp.ui.main.post.postdetail.utils.OptionsUtils.FOLLOW_USER
import com.rober.blogapp.ui.main.post.postdetail.utils.OptionsUtils.REPORT_POST
import com.rober.blogapp.ui.main.post.postdetail.utils.OptionsUtils.UNFOLLOW_USER
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class PostDetailViewModel @ViewModelInject constructor(
    val firebaseRepository: FirebaseRepository,
    val application: Application
) : ViewModel() {

    private val _postDetailState: MutableLiveData<PostDetailState> = MutableLiveData()

    val postDetailState: LiveData<PostDetailState>
        get() = _postDetailState

    private var user: User? = null
    private var post: Post? = null
    private var isOptionsVisible = false

    private var listOptionsIcons = listOf<Int>()
    private var listOptionsText = listOf<String>()
    private var listOptions = listOf<Option>()


    fun setIntention(event: PostDetailFragmentEvent) {
        when (event) {
            is PostDetailFragmentEvent.SetPost -> {
                post = event.post
                setPost(event.post, event.post.userCreatorId)
            }
            is PostDetailFragmentEvent.AddLike -> {
            }

            is PostDetailFragmentEvent.GoToProfileFragment -> {
                user?.run {
                    _postDetailState.value = PostDetailState.GoToProfileFragment(this)
                } ?: kotlin.run { _postDetailState.value = PostDetailState.Idle }
            }

            is PostDetailFragmentEvent.GoBackToPreviousFragment -> {
                _postDetailState.value = PostDetailState.BackToPreviousFragment
            }

            is PostDetailFragmentEvent.ShowPostOptions -> {
                if (isOptionsVisible) {
                    _postDetailState.value = PostDetailState.Idle
                    return
                }

                isOptionsVisible = true
                val currentUser = getCurrentUser()
                viewModelScope.launch {
                    post?.also { tempPost ->
                        if (tempPost.userCreatorId == currentUser.user_id) {
                            loadPostOptionsFromCurrentUser()
                        } else {
                            loadPostOptionsFromOtherUser()
                        }
                    }
                }
            }

            is PostDetailFragmentEvent.HideOptions -> {
                isOptionsVisible = false
                _postDetailState.value = PostDetailState.HideOptions
            }

            is PostDetailFragmentEvent.ExecuteOption -> {
                isOptionsVisible = false
                _postDetailState.value = PostDetailState.HideOptions
                val optionPositionIndex = event.optionPositionIndex
                val option = listOptions[optionPositionIndex]

                viewModelScope.launch {
                    when (option.text) {
                        EDIT_POST -> redirectToEditPostFragment()
                        DELETE_POST -> deletePost()
                        REPORT_POST -> reportPost()
                        FOLLOW_USER -> followUser()
                        UNFOLLOW_USER -> unfollowUser()
                    }
                }
            }

            is PostDetailFragmentEvent.GetParcelableUpdatedPost -> {
                _postDetailState.value = PostDetailState.GetParcelableUpdatedPost
            }

            is PostDetailFragmentEvent.GetParcelablePost -> {
                _postDetailState.value = PostDetailState.GetParcelablePost
            }

            is PostDetailFragmentEvent.SaveUpdatedPost -> {
                saveUpdatedPost(event.editedPost)
                setPost(event.editedPost, event.editedPost.userCreatorId)
            }
        }
    }

    private fun setPost(post: Post, userUID: String) {
        viewModelScope.launch {
            val userProfile = getUserProfile(userUID)

            userProfile?.also { tempUserProfile ->
                user = tempUserProfile
                _postDetailState.value = PostDetailState.SetPostDetails(post, tempUserProfile)
            } ?: kotlin.run {
                _postDetailState.value = PostDetailState.BackToPreviousFragment
            }
        }
    }

    private suspend fun getUserProfile(userUID: String): User? {
        var tempUser: User? = null
        val job = viewModelScope.launch {
            firebaseRepository.getUserProfile(userUID)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> tempUser = resultData.data!!

                    }
                }
        }
        job.join()
        return tempUser
    }

    private suspend fun loadPostOptionsFromOtherUser() {
        val doesUserFollowPostCreatorUser = doesCurrentUserFollowsUserPost()

        //Get TypedArray from text
        val optionsTextTypedArray = application.applicationContext.resources.obtainTypedArray(
            R.array.list_post_detail_options_text_if_post_is_created_by_other_user
        )

        //Get TypedArray from icons
        val optionsIconsTypedArray = application.applicationContext.resources.obtainTypedArray(
            R.array.list_post_detail_options_icons_if_post_is_created_by_other_user
        )

        //Get resource id from items and resourcesid from arrays
        val tempListOptionsText = mutableListOf<String>()
        val tempListOptionsIcons = mutableListOf<Int>()
        for (index in 0..optionsIconsTypedArray.indexCount + 1) {
            if (index == 1) { //Index where array is found
                //Get array id
                val textResourceId = optionsTextTypedArray.getResourceId(index, -1)
                val iconResourceId = optionsIconsTypedArray.getResourceId(index, -1)

                if (doesUserFollowPostCreatorUser) {
                    //Text
                    val optionTextArray = application.applicationContext.resources.getStringArray(textResourceId) //Get array
                    val optionTextResource = optionTextArray[ArrayUtils.UNFOLLOW] //Get item string from array
                    tempListOptionsText.add(optionTextResource)

                    //Icon
                    val optionIconArray =
                        application.applicationContext.resources.obtainTypedArray(iconResourceId) //Get typed array
                    val optionIconResource = optionIconArray.getResourceId(ArrayUtils.UNFOLLOW, -1) //Get item int from array
                    tempListOptionsIcons.add(optionIconResource)

                    optionIconArray.recycle()
                } else {
                    //Text
                    val optionTextArray = application.applicationContext.resources.getStringArray(textResourceId) //Get array
                    val optionTextResource = optionTextArray[ArrayUtils.FOLLOW] //Get item string from array
                    tempListOptionsText.add(optionTextResource)

                    //Icon
                    val optionIconArray =
                        application.applicationContext.resources.obtainTypedArray(iconResourceId) //Get typed array
                    val optionIconResource = optionIconArray.getResourceId(ArrayUtils.FOLLOW, -1) //Get item int from array
                    tempListOptionsIcons.add(optionIconResource)
                    optionIconArray.recycle()
                }

            } else { //Get items
                optionsTextTypedArray.getString(index)?.let { tempListOptionsText.add(it) }
                tempListOptionsIcons.add(optionsIconsTypedArray.getResourceId(index, -1))
            }
        }
        listOptionsText = tempListOptionsText
        listOptionsIcons = tempListOptionsIcons

        optionsIconsTypedArray.recycle()
        optionsTextTypedArray.recycle()

        //Create a list of Option for ListOptionsAdapter
        val tempListOptions = mutableListOf<Option>()
        for (index in listOptionsText.indices) {
            val option = Option(listOptionsIcons[index], listOptionsText[index])
            tempListOptions.add(option)
        }

        listOptions = tempListOptions
        _postDetailState.value = PostDetailState.ShowPostOptions(listOptions)
    }

    private suspend fun doesCurrentUserFollowsUserPost(): Boolean {
        var doesUserFollowPostCreatorUser = false
        val job = viewModelScope.launch {
            post?.also { tempPost ->
                firebaseRepository.checkIfCurrentUserFollowsOtherUser(tempPost.userCreatorId)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                doesUserFollowPostCreatorUser = resultData.data!!
                            }
                            is ResultData.Error -> {
                            }
                        }
                    }
            }
        }
        job.join()

        return doesUserFollowPostCreatorUser
    }

    private fun loadPostOptionsFromCurrentUser() {
        //Get strings arraylist
        listOptionsText = application.applicationContext.resources.getStringArray(
            R.array.list_post_detail_options_text_if_post_is_created_by_logged_in_user
        ).toList()

        //Get typed array of resources
        val optionsIconsTypedArray = application.applicationContext.resources.obtainTypedArray(
            R.array.list_post_detail_options_icons_if_post_is_created_by_logged_in_user
        )

        //Get resources Int from typedarray
        val tempListOptionsIcons = mutableListOf<Int>()
        for (index in 0..optionsIconsTypedArray.indexCount + 1) {
            tempListOptionsIcons.add(optionsIconsTypedArray.getResourceId(index, -1))
        }
        listOptionsIcons = tempListOptionsIcons
        optionsIconsTypedArray.recycle()

        //Combine text and icon into a list of Option
        val tempListOptions = mutableListOf<Option>()
        for (index in listOptionsText.indices) {
            val option = Option(listOptionsIcons[index], listOptionsText[index])
            tempListOptions.add(option)
        }

        listOptions = tempListOptions
        _postDetailState.value = PostDetailState.ShowPostOptions(listOptions)
    }

    private fun redirectToEditPostFragment() {
        if (user?.user_id != post?.userCreatorId) {
            _postDetailState.value = PostDetailState.ErrorExecuteOption
            return
        }

        post?.run {
            _postDetailState.value = PostDetailState.RedirectToEditPost(this)
        }
    }

    private fun saveUpdatedPost(editedPost: Post) {
        post = editedPost

        viewModelScope.launch {
            firebaseRepository.saveEditedPost(editedPost)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            val hasPostBeenUpdated = resultData.data!!

                            Log.i("SeePostUpdated", "Has been post updated = ${hasPostBeenUpdated}")
                        }

                        is ResultData.Error -> {
                            Log.i("SeePostUpdated", "Error updating post")
                        }
                    }
                }
        }

    }

    private suspend fun deletePost() {
        var deletedPost = false
        val job = viewModelScope.launch {
            post?.let { tempPost ->
                firebaseRepository.deletePost(tempPost)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                deletedPost = resultData.data!!
                            }

                            is ResultData.Error -> {
                                deletedPost = false
                            }
                        }
                    }
            }
        }
        job.join()

        if (deletedPost)
            _postDetailState.value = PostDetailState.PostDeleted
        else
            _postDetailState.value = PostDetailState.ErrorExecuteOption
    }

    private fun reportPost() {

    }

    private fun followUser() {

    }

    private fun unfollowUser() {

    }

    private fun getCurrentUser(): User {
        return firebaseRepository.getCurrentUser()
    }
}