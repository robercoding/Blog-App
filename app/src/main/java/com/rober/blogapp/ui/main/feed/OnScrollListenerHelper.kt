package com.rober.blogapp.ui.main.feed

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.util.RecyclerViewActionInterface

class OnScrollListenerHelper(
    val context: Context,
    val recyclerViewClickInterface: RecyclerViewActionInterface,
    val onMoveRecyclerListener: OnMoveRecyclerListener
) : RecyclerView.OnScrollListener() {

    var hasUserReachedBottomAndDraggingBefore = false
    //private var layoutManager =linearLayoutManager


    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

//        Toast.makeText(context, "Here we are at least onscrolled", Toast.LENGTH_SHORT).show()
//
//        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
//        val totalItemCount = linearLayoutManager.itemCount
//        val lastVisible = linearLayoutManager.findLastVisibleItemPosition()
//
//        val hasUserReachedBottom = totalItemCount == lastVisible
//        Log.i("OnScrollListenerHelper", "OnScrolled = Total item count: $totalItemCount")
//        Log.i("OnScrollListenerHelper", "OnScrolled = Last Visible: $lastVisible")
//
//        Log.i("OnScrollListenerHelper", "OnScrolled = $hasUserReachedBottom")
//
//        if(totalItemCount-1 == lastVisible){
//            Log.i("OnScrollListenerHelper", "OnScrolled = Reached bottom")
//        }
//
//        if(totalItemCount == lastVisible && dy == 0){
//            Log.i("OnScrollListenerHelper", "OnScrolled = Reached bottom with dy 0")
//            //Toast.makeText(context, "Reached bottom with dy 0", Toast.LENGTH_SHORT).show()
//        }


//        if((linearLayoutManager.findLastVisibleItemPosition() == linearLayoutManager.itemCount -1) && !mHasReachedBottomonce){
//            recyclerViewState = linearLayoutManager.onSaveInstanceState()
//            viewModel.setIntention(FeedFragmentEvent.RetrieveOldFeedPosts(true))
//            mHasReachedBottomOnce = true
//        }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        onMoveRecyclerListener.onMove()
        super.onScrollStateChanged(recyclerView, newState)

//        Toast.makeText(context, "Here we are at least on scrollstatechanged", Toast.LENGTH_SHORT).show()

        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
        val totalItemCount = linearLayoutManager.itemCount
        val lastVisible = linearLayoutManager.findLastVisibleItemPosition()

//        Log.i("OnScrollListener", "OnScrollStateChanged: State = $newState")
        val hasUserReachedBottom = totalItemCount - 1 == lastVisible
        val isUserDragging = newState == RecyclerView.SCROLL_STATE_DRAGGING


        Log.i("OnScrollListener", "Has Reached it before?? ${hasUserReachedBottomAndDraggingBefore}")
        Log.i("OnScrollListener", "is user dragging?? ${isUserDragging}")
        Log.i("OnScrollListener", "Has user reached bottom?? ${hasUserReachedBottom}")
        if (hasUserReachedBottom && isUserDragging) {
            if (!hasUserReachedBottomAndDraggingBefore) {
//                Toast.makeText(context, "Request", Toast.LENGTH_SHORT).show()
                //Log.i("OnScrollListener", "Request")
                hasUserReachedBottomAndDraggingBefore = true

                val lastVisibleItemPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                recyclerViewClickInterface.requestMorePosts(lastVisibleItemPosition)
            } else {
                Log.i("OnScrollListener", "Can't request more data")
//                Toast.makeText(context, "Can't request more data", Toast.LENGTH_SHORT).show()

            }
        }
    }
}