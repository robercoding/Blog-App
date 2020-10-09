package com.rober.blogapp.ui.main.settings.reportedposts

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.ReportPost
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ReportedPostsViewModel @ViewModelInject constructor(
    val firebaseRepository: FirebaseRepository
) : BaseViewModel<ReportedPostsState, ReportedPostsEvent>() {

    var user: User? = null

    var mutableListReportedPosts = mutableListOf<ReportPost>()

    override fun setIntention(event: ReportedPostsEvent) {
        viewModelScope.launch {
            when (event) {
                is ReportedPostsEvent.GetReportedPosts -> {
                    getReportedPosts()
                }

                is ReportedPostsEvent.ClickOnReportedPost -> {
                    if (mutableListReportedPosts.isNotEmpty()) {
                        viewState =
                            ReportedPostsState.GoToPostReported(mutableListReportedPosts[event.positionAdapter])
                    }

                }
            }
        }
    }

    private suspend fun getReportedPosts() {
        user = firebaseRepository.getCurrentUser()

        user?.let { tempUser ->
            viewModelScope.launch {
                firebaseRepository.getListReportedPosts(tempUser)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                mutableListReportedPosts = resultData.data!!.toMutableList()

                                if (mutableListReportedPosts.isEmpty()) {
                                    viewState = ReportedPostsState.SetTotalPosts(0.toString())
                                } else {
                                    viewState = ReportedPostsState.SetTotalPostsAndList(
                                        mutableListReportedPosts.size.toString(),
                                        mutableListReportedPosts
                                    )
                                }
                            }

                            is ResultData.Error -> {
                                viewState =
                                    ReportedPostsState.Error("Sorry, there was en error, try again later")
                            }
                        }
                    }
            }
        }
    }
}