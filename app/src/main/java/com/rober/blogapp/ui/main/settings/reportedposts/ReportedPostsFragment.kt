package com.rober.blogapp.ui.main.settings.reportedposts

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rober.blogapp.R
import com.rober.blogapp.entity.ReportPost
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.settings.reportedposts.adapter.AdapterReportedPosts
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_reported_posts.*

@AndroidEntryPoint
class ReportedPostsFragment :
    BaseFragment<ReportedPostsState, ReportedPostsEvent, ReportedPostsViewModel>(R.layout.fragment_reported_posts),
    RecyclerViewActionInterface {

    override val viewModel: ReportedPostsViewModel by viewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.setIntention(ReportedPostsEvent.GetReportedPosts)
    }

    override fun render(viewState: ReportedPostsState) {
        when (viewState) {
            is ReportedPostsState.SetTotalPosts -> {
                reported_posts_total_number_reported_tv.text =
                    "Total reported posts: ${viewState.totalReportedPosts}"
            }

            is ReportedPostsState.SetTotalPostsAndList -> {
                reported_posts_total_number_reported_tv.text =
                    "Total reported posts: ${viewState.totalReportedPosts}"

                setRecyclerViewAdapter(viewState.listReportPost)
            }

            is ReportedPostsState.GoToPostReported -> {
                val bundleReportPost = bundleOf("reportPost" to viewState.postReported)
                findNavController().navigate(
                    R.id.action_reportedPostsFragment_to_postDetailFragment,
                    bundleReportPost
                )
            }

            is ReportedPostsState.Error -> {
                displayToast(viewState.message)
                findNavController().popBackStack()
            }
        }
    }

    private fun setRecyclerViewAdapter(listReportedPosts: List<ReportPost>) {
        val adapterReportPost = AdapterReportedPosts(listReportedPosts, this)

        reported_posts_recycler_view.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterReportPost
        }

    }

    override fun setupListeners() {
        reported_posts_material_toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun setupViewDesign() {
        super.setupViewDesign()
        reported_posts_material_toolbar.navigationIcon?.colorFilter =
            PorterDuffColorFilter(
                ContextCompat.getColor(requireContext(), R.color.blueTwitter),
                PorterDuff.Mode.SRC_ATOP
            )
    }

    override fun clickListenerOnItem(positionAdapter: Int) {
        viewModel.setIntention(ReportedPostsEvent.ClickOnReportedPost(positionAdapter))
    }

    override fun clickListenerOnPost(positionAdapter: Int) {}

    override fun clickListenerOnUser(positionAdapter: Int) {}

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {}
}

sealed class ReportedPostsEvent {
    object GetReportedPosts : ReportedPostsEvent()

    data class ClickOnReportedPost(val positionAdapter: Int) : ReportedPostsEvent()
}