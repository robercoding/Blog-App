package com.rober.blogapp.ui.main.post.postreply

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.entity.Username
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.post.postdetail.PostDetailFragmentArgs
import com.rober.blogapp.ui.main.post.postdetail.adapter.CommentsAdapter
import com.rober.blogapp.ui.main.post.postreply.adapter.CommentsHighlightAdapter
import com.rober.blogapp.ui.main.post.utils.Constants
import com.rober.blogapp.util.RecyclerViewActionInterface
import com.rober.blogapp.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_detail.*
import kotlinx.android.synthetic.main.fragment_post_reply.*

@AndroidEntryPoint
class PostReplyFragment :
    BaseFragment<PostReplyState, PostReplyEvent, PostReplyViewModel>(R.layout.fragment_post_reply),
    RecyclerViewActionInterface {

    override val viewModel: PostReplyViewModel by viewModels()
    val args: PostReplyFragmentArgs by navArgs()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setDetailsEvent()

    }

    override fun render(viewState: PostReplyState) {
        when (viewState) {
            is PostReplyState.SetSelectedCommentView -> {
                setPostDetails(viewState.post, viewState.postUser)
                setAdapterHighlights(viewState.listComments, viewState.listUsers, viewState.postUser)
            }
        }
    }

    private fun setDetailsEvent() {
        val post = args.post
        val postUser = args.postUser
        val listComments = args.listComment.toList()
        val listUsers = args.listUsers.toList()

        viewModel.setIntention(PostReplyEvent.SetDetails(post, postUser, listComments, listUsers))
    }

    private fun setPostDetails(post: Post, postUser: User) {
        post_reply_username.text = postUser.username
        post_reply_title.text = post.title
        post_reply_text.text = "hgeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
        post_reply_time.text = Utils.getDifferenceTimeMilliseconds(post.createdAt, true)

        if (postUser.profileImageUrl.isNotEmpty())
            Glide.with(requireView())
                .load(postUser.profileImageUrl)
                .into(post_reply_image_profile)
//        post_reply_nestedscrollview.fullScroll(View.FOCUS_DOWN)


    }

    private fun setAdapterHighlights(listComments: List<Comment>, listUsers: List<User>, postUser: User) {
        val listCommentsMock = listComments.toMutableList()
        val listUsersMock = listUsers.toMutableList()

        val comment = Comment(
            "Edited to have more height so we can see if that works correctly, just checking out haha thank!",
            "${listComments[0].commentId}",
            "${listComments[0].commentUserId}",
            "${listComments[0].replyToldId}",
            listComments[0].repliedAt
        )
        listCommentsMock[0] = comment
        listCommentsMock.add(
            Comment(
                "This is mocked but will be the second! now we will se if this works",
                "0",
                listUsers.get(0).userId,
                listComments.get(0).commentId,
                1603270909
            )
        )
        listCommentsMock.add(
            Comment(
                "This is mocked ",
                "0",
                listUsers.get(0).userId,
                listComments.get(0).commentId,
                1603270909
            )
        )
        listUsersMock.add(listUsers.get(0))
        val replyingTo = getReplyingToUsername(listUsersMock, postUser)

        val highlightAdapter = CommentsHighlightAdapter(listCommentsMock, listUsersMock, this, replyingTo)
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.scrollToPositionWithOffset(listCommentsMock.size - 1, 20)
        post_reply_recycler_highlight.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = highlightAdapter
        }

        val listCommentsTest = listOf<Comment>(listCommentsMock[0])
        val listUsersTest = listOf<User>(listUsers[0])
        val commentsAdapter = CommentsAdapter(listCommentsTest, listUsersTest, this)
        post_reply_recycler_comments.apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())

            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }



        post_reply_nestedscrollview.requestFocus()
        post_reply_nestedscrollview.requestLayout()
        post_reply_nestedscrollview.requestFocus()
        post_reply_nestedscrollview.requestLayout()
//        post_reply_nestedscrollview.fullScroll(View.FOCUS_DOWN)
//        post_reply_nestedscrollview.scrollTo(700, 700)
        post_reply_nestedscrollview.requestLayout()


        post_reply_recycler_highlight.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val postHeight = post_reply_container_post.y
                val recyclerHightlightHeight = post_reply_recycler_highlight.y
                val childHeight = post_reply_recycler_highlight.getChildAt(0).height
                Log.i("SeeHeight", "Post Height = ${postHeight}")
                Log.i("SeeHeight", "RecyclerHighlight y= ${recyclerHightlightHeight}")
                Log.i("SeeHeight", "Child 0 = ${childHeight}")
                var childHeights = 0
                for(index in 0..listCommentsMock.size-2){
                    childHeights += post_reply_recycler_highlight.getChildAt(index).height
                }
                post_reply_recycler_highlight.viewTreeObserver.removeOnGlobalLayoutListener(this)
                post_reply_nestedscrollview.post {
//            post_reply_nestedscrollview.fullScroll()
                    Log.i("SeeHeight", "${postHeight + recyclerHightlightHeight + childHeights}")
                    post_reply_nestedscrollview.scrollTo(
                        0,
                        (postHeight + recyclerHightlightHeight + childHeights).toInt()
                    )
                }

            }
        })

//        post_reply_nestedscrollview.post {
////            post_reply_nestedscrollview.fullScroll()
//            Log.i("SeeHeight", "${postHeight + recyclerHightlightHeight}")
//            post_reply_nestedscrollview.scrollTo(0, (postHeight + recyclerHightlightHeight).toInt())
//        }
//        post_reply_nestedscrollview.smoothScrollTo(0, 800)

//        post_reply_nestedscrollview.scrollTo(0, 700)
//        post_reply_nestedscrollview.fullScroll(View.FOCUS_DOWN)
//        post_reply_nestedscrollview.scrollY = 700
//        post_reply_nestedscrollview.smoothScrollTo(0, 1000)

//        post_reply_nestedscrollview.smoothScrollTo(0, 700)
//        post_reply_nestedscrollview.scrollTo(0, View.FOCUS_DOWN)
//        post_reply_nestedscrollview.parent.requestChildFocus(post_reply_nestedscrollview, post_reply_nestedscrollview)
//        post_reply_nestedscrollview.isSmoothScrollingEnabled = true
//        post_reply_nestedscrollview.scrollTo(0, 300)
//        post_reply_nestedscrollview.fullScroll(View.FOCUS_DOWN)

//        post_reply_nestedscrollview.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//
//        }

//        post_reply_nestedscrollview.setOnScrollChangeListener(object: ScrollView.OnScroll{
//            override fun onScrollChange(
//                v: NestedScrollView?,
//                scrollX: Int,
//                scrollY: Int,
//                oldScrollX: Int,
//                oldScrollY: Int
//            ) {
//                v?.scrollTo(0, 700)
//                Log.i("SeeListen", "Listening to scroll ScrollY = ${scrollY} and old scrollY ${oldScrollY}")
//            }
//        })
//        post_reply_nestedscrollview.scrollY = 700
//        post_reply_recycler_comments.isFocusable = false
//        post_reply_recycler_highlight.isFocusable = false
//
//        post_reply_nestedscrollview.invalidate()
//        post_reply_nestedscrollview.requestFocus()
//        post_reply_nestedscrollview.requestLayout()
//        post_reply_nestedscrollview.scrollBy(0, 700)
//        post_reply_nestedscrollview.scrollTo(0, 600)
    }

    private fun getReplyingToUsername(listUsers: List<User>, postUser: User): String {
        return if (listUsers.size > 1) {
            listUsers[listUsers.size - 2].username
        } else {
            postUser.username
        }
    }

    private fun displayReplyView(display: Boolean) {
        if (display) {
            post_reply_textview.show()
            post_reply_textview_username.show()
            post_reply_button_reply.show()
        } else {
            post_reply_textview.hide()
            post_reply_textview_username.hide()
            post_reply_button_reply.hide()
        }
    }

    override fun setupViewDesign() {
        super.setupViewDesign()
        displayReplyView(false)
    }

    override fun setupListeners() {
        post_reply_edittext.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                displayReplyView(true)
                customActionOnBackPressed(Constants.BACK_REPLY)
            } else {
                displayReplyView(false)
                restoreDefaultOnBackPressed()
            }
        }
    }

    override fun customActionOnBackPressed(action: Int) {
        super.customActionOnBackPressed(action)
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (action) {
                    Constants.BACK_REPLY -> {
                        post_reply_edittext.clearFocus()
                    }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    override fun clickListenerOnItem(positionAdapter: Int) {
        //
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        //
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //
    }

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {
        //
    }
}

sealed class PostReplyEvent {
    data class SetDetails(
        val post: Post,
        val postUser: User,
        val listComments: List<Comment>,
        val listUsers: List<User>
    ) : PostReplyEvent()

}