package com.rober.blogapp.ui.main.post.postreply

import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
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
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.post.postdetail.adapter.CommentsAdapter
import com.rober.blogapp.ui.main.post.postreply.adapter.CommentsHighlightAdapter
import com.rober.blogapp.ui.main.post.utils.Constants
import com.rober.blogapp.util.RecyclerViewActionInterface
import com.rober.blogapp.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_reply.*

@AndroidEntryPoint
class PostReplyFragment :
    BaseFragment<PostReplyState, PostReplyEvent, PostReplyViewModel>(R.layout.fragment_post_reply),
    PostReplyClickListener {

    override val viewModel: PostReplyViewModel by viewModels()
    val args: PostReplyFragmentArgs by navArgs()
//    var commentAdapter = CommentsAdapter(listOf(), listOf(), this)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setDetailsEvent()

    }

    override fun render(viewState: PostReplyState) {
        when (viewState) {
            is PostReplyState.SetSelectedCommentView -> {
                setPostDetails(viewState.post, viewState.postUser)
                setAdapterHighlights(viewState.listComments, viewState.listUsers, viewState.postUser)
//                commentAdapter.clear()
                cleanAdapterCommentReplies()
                viewModel.setIntention(PostReplyEvent.GetCommentReplies)
            }

            is PostReplyState.SetCommentReplies -> {
                setAdapterCommentReplies(viewState.listComments, viewState.listUsers)
            }

            is PostReplyState.RestoreCommentsAdapter -> {
                Log.i("SeeRestore", "Restoring!")
                setAdapterHighlights(viewState.listHighlightComments, viewState.listUsers, viewState.postUser)
                setAdapterCommentReplies(viewState.listComments, viewState.listUsers)
            }

            is PostReplyState.ReplySuccess -> {
                setAdapterCommentReplies(viewState.listComment, viewState.listUser)
                displayToast("Your reply is succesful!")
            }

            is PostReplyState.CommentRepliesEmpty -> {
                //Nothing
            }

            is PostReplyState.Error -> {
                displayToast(viewState.message)
            }
            is PostReplyState.PopBackStack -> {
                findNavController().popBackStack()
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
        post_reply_text.text = post.text
        post_reply_time.text = Utils.getDifferenceTimeMilliseconds(post.createdAt, true)

        if (postUser.profileImageUrl.isNotEmpty())
            Glide.with(requireView())
                .load(postUser.profileImageUrl)
                .into(post_reply_image_profile)
//        post_reply_nestedscrollview.fullScroll(View.FOCUS_DOWN)


    }

    private fun setAdapterHighlights(listComments: List<Comment>, listUsers: List<User>, postUser: User) {
        Log.i("SeeRestore", "ListComments ${listComments}")
//        val listCommentsMock = listComments.toMutableList()
//        val listUsersMock = listUsers.toMutableList()

//        val comment = Comment(
//            "Edited to have more height so we can see if that works correctly, just checking out haha thank!",
//            "${listComments[0].commentId}",
//            "${listComments[0].commentUserId}",
//            "${listComments[0].replyToldId}",
//            listComments[0].repliedAt
//        )
//        listCommentsMock[0] = comment
//        listCommentsMock.add(
//            Comment(
//                "This is mocked but will be the second! now we will se if this works",
//                "0",
//                listUsers.get(0).userId,
//                listComments.get(0).commentId,
//                1603270909
//            )
//        )
//        listCommentsMock.add(
//            Comment(
//                "This is mocked ",
//                "0",
//                listUsers.get(0).userId,
//                listComments.get(0).commentId,
//                1603270909
//            )
//        )
//        listUsersMock.add(listUsers.get(0))
        val replyingTo = getReplyingToUsername(listUsers, postUser)

        val highlightAdapter = CommentsHighlightAdapter(listComments, listUsers, this, replyingTo)
        post_reply_recycler_highlight.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = highlightAdapter
        }

//        val listCommentsTest = listOf<Comment>(listCommentsMock[0])
//        val listUsersTest = listOf<User>(listUsers[0])

        post_reply_nestedscrollview.requestFocus()
        post_reply_nestedscrollview.requestLayout()

        post_reply_recycler_highlight.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                post_reply_recycler_highlight.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val recyclerHightlightHeight = post_reply_recycler_highlight.y
                var childHeights = 0
                for (index in 0..listComments.size - 2) { //-2 to get the first child that selected comment is replying
                    childHeights += post_reply_recycler_highlight.getChildAt(index).height
                }
                post_reply_nestedscrollview.post {
                    post_reply_nestedscrollview.smoothScrollBy(
                        0,
                        (recyclerHightlightHeight + childHeights).toInt(),
                        2000
                    )
                }
            }
        })
    }

    private fun setAdapterCommentReplies(listComments: List<Comment>, listUsers: List<User>) {
        val commentsAdapter = CommentsAdapter(listComments, listUsers, this)
        post_reply_recycler_comments.apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())

            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun cleanAdapterCommentReplies() {
        val commentsAdapter = CommentsAdapter(listOf(), listOf(), this)
        post_reply_recycler_comments.apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
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

        post_reply_material_toolbar.setNavigationOnClickListener {
            viewModel.setIntention(PostReplyEvent.PopBackStack)
//            findNavController().popBackStack()
        }

        post_reply_edittext.addTextChangedListener {
            it?.let {
                if (it.isEmpty()) {
                    post_reply_button_reply.isEnabled = false
                    post_reply_button_reply.setTextColor(getColor(R.color.secondaryText))
                    post_reply_button_reply.background.setTint(getColor(R.color.blueGray))
                } else {
                    post_reply_button_reply.isEnabled = true
                    post_reply_button_reply.setTextColor(getColor(R.color.primaryText))
                    post_reply_button_reply.background.setTint(getColor(R.color.blueTwitter))
                }
            }
        }

        post_reply_button_reply.setOnClickListener {
            val text = post_reply_edittext.text.toString()

            if (text.isEmpty()) {
                return@setOnClickListener
            }

            displayReplyView(false)
            post_reply_edittext.clearFocus()
            post_reply_edittext.setText("")
            hideKeyBoard()

            viewModel.setIntention(PostReplyEvent.ReplyComment(text))
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

    override fun onClickHighlightComment(positionAdapter: Int) {
//        viewModel.setIntention(PostReplyEvent.SelectReplyComment(positionAdapter))
    }

    override fun onClickReplyComment(positionAdapter: Int) {
        viewModel.setIntention(PostReplyEvent.SelectReplyComment(positionAdapter))

    }
}

sealed class PostReplyEvent {
    data class SetDetails(
        val post: Post,
        val postUser: User,
        val listComments: List<Comment>,
        val listUsers: List<User>
    ) : PostReplyEvent()

    object GetCommentReplies : PostReplyEvent()
    data class SelectReplyComment(val positionAdapter: Int) : PostReplyEvent()

    object PopBackStack : PostReplyEvent()

    data class ReplyComment(val text: String) : PostReplyEvent()
}