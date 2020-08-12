package com.rober.blogapp.ui.main.feed

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OnScrollListenerHelper(val linearLayoutManager: LinearLayoutManager) : RecyclerView.OnScrollListener() {

    private var mHasReachedBottomOnce = false
    private var layoutManager =linearLayoutManager

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

//        if((linearLayoutManager.findLastVisibleItemPosition() == linearLayoutManager.itemCount -1) && !mHasReachedBottomonce){
//            recyclerViewState = linearLayoutManager.onSaveInstanceState()
//            viewModel.setIntention(FeedFragmentEvent.RetrieveOldFeedPosts(true))
//            mHasReachedBottomOnce = true
//        }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
    }
}