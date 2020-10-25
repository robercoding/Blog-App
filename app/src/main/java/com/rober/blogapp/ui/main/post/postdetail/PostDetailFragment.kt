package com.rober.blogapp.ui.main.post.postdetail

import android.content.DialogInterface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rober.blogapp.R
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.ReportPost
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.post.postdetail.adapter.CommentsAdapter
import com.rober.blogapp.ui.main.post.postdetail.adapter.ListOptionsAdapter
import com.rober.blogapp.ui.main.post.postdetail.adapter.OnListOptionsClickInterface
import com.rober.blogapp.ui.main.post.postreply.PostReplyClickListener
import com.rober.blogapp.ui.main.post.utils.Constants
import com.rober.blogapp.util.EmojiUtils.OK_HAND
import com.rober.blogapp.util.RecyclerViewActionInterface
import com.rober.blogapp.util.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_report_post.view.*
import kotlinx.android.synthetic.main.fragment_post_detail.*

@AndroidEntryPoint
class PostDetailFragment :
    BaseFragment<PostDetailState, PostDetailFragmentEvent, PostDetailViewModel>(R.layout.fragment_post_detail),
    OnListOptionsClickInterface, PostReplyClickListener {

    override val viewModel: PostDetailViewModel by viewModels()
    val args: PostDetailFragmentArgs by navArgs()


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AndroidThreeTen.init(requireContext())

        viewModel.setIntention(PostDetailFragmentEvent.GetParcelableUpdatedPost)
    }

    override fun render(viewState: PostDetailState) {
        when (viewState) {

            is PostDetailState.GetParcelableUpdatedPost -> {
                enableLoadingPost(true)
                getParcelableUpdatedPostAndSetIntention()
            }

            is PostDetailState.GetParcelablePost -> {
                getParcelablePostAndSetIntention()
            }

            is PostDetailState.GetParcelableReportedPost -> {
                getParcelableReportedPostAndSetIntention()
            }

            is PostDetailState.SetPostDetails -> {
                setPostDetails(viewState.post)
                setUserDetails(viewState.user)
                enableLoadingPost(false)
                displayReplyView(false)

                viewModel.setIntention(PostDetailFragmentEvent.GetCommentsPost)
                displayProgressBar(post_detail_comments_progressbar, true)
            }

            is PostDetailState.SetPostCommments -> {
//                displayHighlightCommentView(false)b
                setCommentAdapter(viewState.listComment, viewState.listUser)
                post_detail_comments_progressbar.hide()
                post_detail_sending_reply_progressbar.hide()
            }

            is PostDetailState.SetSelectedCommentView -> {
//                displayHighlightCommentView(true)
//                setHighlightCommentAdapter(
//                    viewState.listSelectedComment,
//                    viewState.listUsers,
//                    viewState.highlightCommentPosition,
//                    viewState.usernameReply
//                )


                val action = PostDetailFragmentDirections.actionPostDetailFragmentToPostReply(
                    viewState.post,
                    viewState.postUser,
                    viewState.listSelectedComment.toTypedArray(),
                    viewState.listUsers.toTypedArray()
                )
                findNavController().navigate(action)
            }

            is PostDetailState.PostCommentsEmpty -> {
                displayProgressBar(post_detail_comments_progressbar, false)
            }

            is PostDetailState.SetReportPostDetails -> {
                setPostDetails(viewState.post)
                setUserDetails(viewState.user)

                displayViewReportPost()
                enableLoadingPost(false)
            }

            is PostDetailState.RedirectToEditPost -> {
                goToPostAdd(viewState.post)
            }

            is PostDetailState.BackToPreviousFragment -> {
                goBackToPreviousFragment()
            }

            is PostDetailState.GoToProfileFragment -> {
                goToProfileFragment(viewState.user)
            }

            is PostDetailState.ReplySuccess -> {
                setCommentAdapter(viewState.listComment, viewState.listUser)
                displaySnackbar("Reply is successful!")
                post_detail_sending_reply_progressbar.hide()
            }

            is PostDetailState.ShowPostOptions -> {
                val listOptions = viewState.listOptions
                post_detail_options_list.adapter =
                    ListOptionsAdapter(requireContext(), listOptions, this)

                enablePostDetailOptionsMode(true)
                setListViewMaxWidth()

                post_detail_motion_layout_container.transitionToEnd()
            }

            is PostDetailState.PostDeleted -> {
                displayToast("Post has been successfully deleted! ${getEmoji(OK_HAND)}")
                goBackToPreviousFragment()
            }

            is PostDetailState.OpenDialogReport -> {
                openDialogReport()
            }

            is PostDetailState.HideOptions -> {
                enablePostDetailOptionsMode(false)
            }

            is PostDetailState.ErrorLoadingPost -> {
                displayToast(viewState.message)
                findNavController().popBackStack()
            }

            is PostDetailState.ErrorExecuteOption -> {
                enablePostDetailOptionsMode(false)
            }

            is PostDetailState.NotifyUser -> {
                displayToast(viewState.message)
            }

            is PostDetailState.Error -> {
                viewState.exception.message?.let { displayToast(it) }
            }

            is PostDetailState.Idle -> {
                //Nothing
            }
        }
    }

    private fun setCommentAdapter(listComment: List<Comment>, listUser: List<User>) {
        val commentsAdapter = CommentsAdapter(listComment, listUser, this)
        recyclerview_post_detail_comments.apply {
            adapter = commentsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

//    private fun setHighlightCommentAdapter(
//        listComment: List<Comment>,
//        listUser: List<User>,
//        highlighCommentPosition: Int,
//        usernameReply: String?
//    ) {
//        val commentsAdapter = CommentsHighlightAdapter(
//            listComment,
//            listUser,
//            this,
//            highlighCommentPosition,
//            usernameReply
//        )
//
//        recyclerview_post_detail_comments.apply {
//            adapter = commentsAdapter
//            layoutManager = LinearLayoutManager(requireContext())
//            addItemDecoration(DividerItemDecoration(requireContext(), 0))
//        }
//    }

    private fun enablePostDetailOptionsMode(displayOptionsMode: Boolean) {
        if (displayOptionsMode) {
            post_detail_motion_layout_container.visibility = View.VISIBLE
            main_view_background_opaque.visibility = View.VISIBLE
        } else {
            post_detail_motion_layout_container.visibility = View.GONE
            main_view_background_opaque.visibility = View.GONE
        }
    }

    //Set max_width of every row in listview, so when you click you are able to select the entire row (XML doesn't do match set full width)
    private fun setListViewMaxWidth() {
        val layout = post_detail_options_list.layoutParams
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
        val width = metrics.widthPixels
        layout.width = width
        layout.height = ViewGroup.LayoutParams.WRAP_CONTENT
        post_detail_options_list.layoutParams = layout
    }

    private fun setPostDetails(post: Post) {
        post_detail_heart_number.text = "${post.likes}"
        post_detail_text.text = post.text
        post_detail_title.text = post.title

//        val instantDate = Instant.ofEpochSecond(post.createdAt)
//        val zdt = ZoneId.systemDefault()
//        val instantDateZoneId = instantDate.atZone(ZoneId.of(zdt.toString()))
//
//        val fmtDate = DateTimeFormatter.ofPattern("dd/MM/yy")
//        val fmtTime = DateTimeFormatter.ofPattern("HH:mm")

        val date = Utils.getDateDayMonthYearInSeconds(post.createdAt)
        val time = Utils.getDateHourMinutesInSeconds(post.createdAt)

        post_detail_date.text = "${date}   |   ${time}"
    }

    private fun setUserDetails(user: User) {
        post_detail_username.text = "@${user.username}"
        post_detail_textview_username_reply_to.text = "@${user.username}"

        val imageProfile: Any = if (user.profileImageUrl.isEmpty())
            R.drawable.cat_sleep
        else
            user.profileImageUrl

        Glide.with(requireView())
            .load(imageProfile)
            .dontAnimate()
            .into(post_detail_image_profile)
    }

    private fun goToPostAdd(postToEdit: Post) {
        val postToEditBundle = bundleOf("postToEdit" to postToEdit)

        if (findNavController().currentDestination?.id == R.id.postDetailFragment) {
            findNavController().navigate(R.id.action_postDetailFragment_to_postAddFragment, postToEditBundle)
        }
    }

    private fun goBackToPreviousFragment() {
        findNavController().popBackStack()
    }

    private fun displayReplyView(display: Boolean) {
        if (display) {
            post_detail_textview_username_reply_to.show()
            post_detail_textview_reply_text.show()
            post_detail_button_reply.show()
            post_detail_sending_reply_progressbar.hide()
        } else {
            post_detail_textview_username_reply_to.hide()
            post_detail_textview_reply_text.hide()
            post_detail_button_reply.hide()
            post_detail_sending_reply_progressbar.hide()
        }
    }

    private fun goToProfileFragment(user: User) {
        val navController = findNavController()
        val bundleUserId = bundleOf("userId" to user.userId)
        navController.navigate(R.id.profileDetailFragment, bundleUserId)
    }

    private fun openDialogReport() {
        val viewLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_post, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())

        dialog.setView(viewLayout)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.color.primaryBackground))
            .setPositiveButton(R.string.dialog_positive_button, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val reportCause = viewLayout.dialog_report_spinner.selectedItem as String
                    val message = viewLayout.dialog_report_message_box.text.toString()
                    viewModel.setIntention(PostDetailFragmentEvent.SendReport(reportCause, message))
                }
            })
            .setNegativeButton(R.string.dialog_negative_button, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    viewModel.setIntention(PostDetailFragmentEvent.CancelReport)
                    dialog?.dismiss()
                }
            })
            .show()
    }

    private fun displayViewReportPost() {
        post_detail_material_toolbar.title = "Reported Post"

        post_detail_options.hide()
        post_detail_top_divider.hide()

        post_detail_comment_icon.hide()
        post_detail_heart_icon.hide()
        post_detail_heart_number.hide()
        post_detail_heart_text.hide()
        post_detail_container_reply.hide()

    }

//    private fun displayHighlightCommentView(display: Boolean) {
//        if (display) {
//            post_detail_text.textSize = 14f
//            post_detail_username.textSize = 12f
//            post_detail_title.textSize = 12f
//
//            post_detail_container_post_selected_comment.show()
//            post_detail_container_post.hide()
//            val constraintLayoutParams =
//                ConstraintLayout.LayoutParams(recyclerview_post_detail_comments.layoutParams)
//            constraintLayoutParams.topToBottom = R.id.post_detail_container_post_selected_comment
//            constraintLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
//            constraintLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
//            recyclerview_post_detail_comments.layoutParams = constraintLayoutParams
//
//        } else {
//
//            post_detail_heart_icon.show()
//            post_detail_heart_text.show()
//            post_detail_heart_number.show()
//            post_detail_comment_icon.show()
//            post_detail_top_divider.show()
//            post_detail_date.show()
//            post_detail_options.show()
//            post_detail_text.textSize = 16f
//            post_detail_username.textSize = 12f
//
//            post_detail_container_post_selected_comment.hide()
//            post_detail_container_post.show()
//            val constraintLayoutParams =
//                ConstraintLayout.LayoutParams(recyclerview_post_detail_comments.layoutParams)
//            constraintLayoutParams.topToBottom = R.id.post_detail_container_post
//            constraintLayoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
//            constraintLayoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
//            recyclerview_post_detail_comments.layoutParams = constraintLayoutParams
//        }
//    }


    private fun getParcelableUpdatedPostAndSetIntention() {
        val updatedPost = arguments?.getParcelable<Post>("updatedPost")

        updatedPost?.run {
            viewModel.setIntention(PostDetailFragmentEvent.SaveUpdatedPost(updatedPost))
        } ?: kotlin.run {
            viewModel.setIntention(PostDetailFragmentEvent.GetParcelablePost)
        }
    }

    private fun getParcelablePostAndSetIntention() {
        val post = arguments?.getParcelable<Post>("post")

        post?.run {
            viewModel.setIntention(PostDetailFragmentEvent.SetPost(this))
        } ?: kotlin.run {
            viewModel.setIntention(PostDetailFragmentEvent.GetParcelableReportedPost)
        }
    }

    private fun getParcelableReportedPostAndSetIntention() {
        val reportedPost = arguments?.getParcelable<ReportPost>("reportPost")

        reportedPost?.run {
            viewModel.setIntention(PostDetailFragmentEvent.GetReportedPostAndUser(reportedPost))
        } ?: kotlin.run {
            viewModel.setIntention(PostDetailFragmentEvent.GoBackToPreviousFragment)
        }
    }

    private fun enableLoadingPost(display: Boolean) {
        if (display) post_detail_background_loading.visibility =
            View.VISIBLE else post_detail_background_loading.visibility = View.GONE
    }

    override fun setupListeners() {
        post_detail_material_toolbar.setNavigationOnClickListener {
            viewModel.setIntention(PostDetailFragmentEvent.GoBackToPreviousFragment)
        }
        post_detail_text.movementMethod = ScrollingMovementMethod()

        post_detail_username.setOnClickListener {
            viewModel.setIntention(PostDetailFragmentEvent.GoToProfileFragment)
        }

        post_detail_image_profile.setOnClickListener {
            viewModel.setIntention(PostDetailFragmentEvent.GoToProfileFragment)
        }

        post_detail_options.setOnClickListener {
            viewModel.setIntention(PostDetailFragmentEvent.ShowPostOptions)
        }

        post_detail_comment_icon.setOnClickListener {
            //
        }

        post_detail_heart_icon.setOnClickListener {
            //
        }

        post_detail_button_reply.setOnClickListener {
            val replyText = post_detail_edittext_reply.text.toString()
            if (replyText.isEmpty()) {
                displayToast("Reply can't be empty")
                return@setOnClickListener
            }

            displayReplyView(false)
            post_detail_edittext_reply.clearFocus()
            post_detail_edittext_reply.setText("")
            post_detail_sending_reply_progressbar.show()
            hideKeyBoard()
            viewModel.setIntention(PostDetailFragmentEvent.AddReply(replyText))
        }

        post_detail_edittext_reply.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                displayReplyView(true)
                customActionOnBackPressed(Constants.BACK_REPLY)
            } else {
                displayReplyView(false)
                restoreDefaultOnBackPressed()
            }
        }

        post_detail_edittext_reply.addTextChangedListener {
            it?.let {
                if (it.isEmpty()) {
                    post_detail_button_reply.isEnabled = false
                    post_detail_button_reply.setTextColor(getColor(R.color.secondaryText))
                    post_detail_button_reply.background.setTint(getColor(R.color.blueGray))
                } else {
                    post_detail_button_reply.isEnabled = true
                    post_detail_button_reply.setTextColor(getColor(R.color.primaryText))
                    post_detail_button_reply.background.setTint(getColor(R.color.blueTwitter))
                }
            }
        }

        post_detail_motion_layout_container.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                if (p0?.currentState == post_detail_motion_layout_container.startState) {
                    viewModel.setIntention(PostDetailFragmentEvent.HideOptions)
                    displayToast("Is start")
                }
                if (p0?.currentState == post_detail_motion_layout_container.endState) {
                    displayToast("Is end")
                }
            }

            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
        })
    }

    override fun setupViewDesign() {
        post_detail_material_toolbar.navigationIcon?.colorFilter =
            PorterDuffColorFilter(
                ContextCompat.getColor(requireContext(), R.color.blueTwitter),
                PorterDuff.Mode.SRC_ATOP
            )

//        post_detail_edittext_reply.backgroundTintList = ColorUtils(requireContext()).postDetailReplyEditText
//        post_detail_edittext_reply.background.colorFilter
        requireActivity().window.setSoftInputMode(0) //Don't move up all views when keyboard appears to reply
    }

    override fun customActionOnBackPressed(action: Int) {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (action) {
                    Constants.BACK_REPLY -> {
                        post_detail_edittext_reply.clearFocus()
                    }
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onClickListOption(position: Int) {
        viewModel.setIntention(PostDetailFragmentEvent.ExecuteOption(position))
        Log.i(
            "PostDetailMotion",
            "List touch interface, position = $position"
        )
    }

    override fun onClickHighlightComment(positionAdapter: Int) {}

    override fun onClickReplyComment(positionAdapter: Int) {
        viewModel.setIntention(PostDetailFragmentEvent.SelectComment(positionAdapter))
    }
}

sealed class PostDetailFragmentEvent {
    object GetParcelableUpdatedPost : PostDetailFragmentEvent()
    object GetParcelablePost : PostDetailFragmentEvent()
    object GetParcelableReportedPost : PostDetailFragmentEvent()

    object GetCommentsPost : PostDetailFragmentEvent()

    data class SetPost(val post: Post) : PostDetailFragmentEvent()
    data class GetReportedPostAndUser(val reportedPost: ReportPost) : PostDetailFragmentEvent()
    data class AddReply(val message: String) : PostDetailFragmentEvent()
    object AddLike : PostDetailFragmentEvent()
    object AddRepost : PostDetailFragmentEvent()

    data class SelectComment(val position: Int) : PostDetailFragmentEvent()

    object HideOptions : PostDetailFragmentEvent()
    object ShowPostOptions : PostDetailFragmentEvent()

    data class ExecuteOption(val optionPositionIndex: Int) : PostDetailFragmentEvent()

    data class SaveUpdatedPost(val editedPost: Post) : PostDetailFragmentEvent()

    object CancelReport : PostDetailFragmentEvent()
    data class SendReport(val reportCause: String, val message: String) : PostDetailFragmentEvent()

    object GoToProfileFragment : PostDetailFragmentEvent()
    object GoBackToPreviousFragment : PostDetailFragmentEvent()
}