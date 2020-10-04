package com.rober.blogapp.ui.main.post.postdetail

import android.content.DialogInterface
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.post.postdetail.adapter.ListOptionsAdapter
import com.rober.blogapp.ui.main.post.postdetail.adapter.OnListOptionsClickInterface
import com.rober.blogapp.util.EmojiUtils.OK_HAND
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.dialog_report_post.view.*
import kotlinx.android.synthetic.main.fragment_post_detail.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

@AndroidEntryPoint
class PostDetailFragment : Fragment(), OnListOptionsClickInterface {

    private val viewModel: PostDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        post_detail_toolbar.navigationIcon?.colorFilter =
            PorterDuffColorFilter(
                ContextCompat.getColor(requireContext(), R.color.blueTwitter),
                PorterDuff.Mode.SRC_ATOP
            )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AndroidThreeTen.init(requireContext())
        setupObservers()

        viewModel.setIntention(PostDetailFragmentEvent.GetParcelableUpdatedPost)
    }

    private fun setupObservers() {
        viewModel.postDetailState.observe(viewLifecycleOwner, Observer { postDetailState ->
            render(postDetailState)
        })
    }

    private fun render(postDetailState: PostDetailState) {
        when (postDetailState) {

            is PostDetailState.GetParcelableUpdatedPost -> {
                enableLoadingPost(true)
                getParcelableUpdatedPostAndSetIntention()
            }

            is PostDetailState.GetParcelablePost -> {
                getParcelablePostAndSetIntention()
            }

            is PostDetailState.SetPostDetails -> {
                setPostDetails(postDetailState.post)
                setUserDetails(postDetailState.user)
                enableLoadingPost(false)
            }

            is PostDetailState.RedirectToEditPost -> {
                goToPostAdd(postDetailState.post)
            }

            is PostDetailState.BackToPreviousFragment -> {
                moveToFeedFragment()
            }

            is PostDetailState.GoToProfileFragment -> {
                goToProfileFragment(postDetailState.user)
            }

            is PostDetailState.ShowPostOptions -> {
                val listOptions = postDetailState.listOptions
                post_detail_options_list.adapter =
                    ListOptionsAdapter(requireContext(), listOptions, this)

                enablePostDetailOptionsMode(true)
                setListViewMaxWidth()

                post_detail_motion_layout_container.transitionToEnd()
            }

            is PostDetailState.PostDeleted -> {
                Toast.makeText(
                    requireContext(),
                    "Post has been successfully deleted! ${getEmoji(OK_HAND)}",
                    Toast.LENGTH_SHORT
                ).show()
                moveToFeedFragment()
            }

            is PostDetailState.OpenDialogReport -> {
                openDialogReport()
            }

            is PostDetailState.HideOptions -> {
                enablePostDetailOptionsMode(false)
            }

            is PostDetailState.ErrorExecuteOption -> {
                enablePostDetailOptionsMode(false)
            }

            is PostDetailState.NotifyUser -> {
                Toast.makeText(requireContext(), "${postDetailState.message}", Toast.LENGTH_SHORT).show()
            }

            is PostDetailState.Error -> {
                Toast.makeText(requireContext(), "${postDetailState.exception.message}", Toast.LENGTH_SHORT)
                    .show()
            }

            is PostDetailState.Idle -> {
                //Nothing
            }
        }
    }

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
//        val dateSecondWithZoneId = Instant.ofEpochSecond(post.created_at).toEpochMilli()

//        Log.i("ZoneId", "${z.getAvailableZoneIds()}")

        val instantDate = Instant.ofEpochSecond(post.created_at)
        val zdt = ZoneId.systemDefault()
        val instantDateZoneId = instantDate.atZone(ZoneId.of(zdt.toString()))

        val fmtDate = DateTimeFormatter.ofPattern("dd/MM/yy")
        val fmtTime = DateTimeFormatter.ofPattern("HH:mm")

        val date = fmtDate.format(instantDateZoneId)
        val time = fmtTime.format(instantDateZoneId)

        post_detail_date.text = "${date}   |   ${time}"
    }

    private fun setUserDetails(user: User) {
        post_detail_username.text = "@${user.username}"

        val imageProfile: Any = if (user.profileImageUrl.isEmpty())
            R.drawable.user
        else
            user.profileImageUrl

        Glide.with(requireView())
            .load(imageProfile)
            .into(post_detail_image_profile)
    }

    private fun goToPostAdd(postToEdit: Post) {
        val postToEditBundle = bundleOf("postToEdit" to postToEdit)

        if (findNavController().currentDestination?.id == R.id.postDetailFragment) {
            findNavController().navigate(R.id.action_postDetailFragment_to_postAddFragment, postToEditBundle)
        }
    }

    private fun moveToFeedFragment() {
        val navController = findNavController()
        navController.navigate(R.id.feedFragment)
    }

    private fun goToProfileFragment(user: User) {
        val navController = findNavController()
        val bundleUserId = bundleOf("userId" to user.user_id)
        navController.navigate(R.id.profileDetailFragment, bundleUserId)
    }

    private fun openDialogReport() {
        val viewLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_post, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())

        dialog.setView(viewLayout)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.color.background))
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

    private fun setupListeners() {
        post_detail_toolbar.setNavigationOnClickListener {
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

        post_detail_motion_layout_container.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                if (p0?.currentState == post_detail_motion_layout_container.startState) {
                    viewModel.setIntention(PostDetailFragmentEvent.HideOptions)
                    Toast.makeText(requireContext(), "Is start", Toast.LENGTH_SHORT).show()
                }
                if (p0?.currentState == post_detail_motion_layout_container.endState) {
                    Toast.makeText(requireContext(), "Is end", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
        })
    }

    override fun onClickListOption(position: Int) {
        viewModel.setIntention(PostDetailFragmentEvent.ExecuteOption(position))
        Log.i(
            "PostDetailMotion",
            "List touch interface, position = $position"
        )
    }

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

        if (post == null) {
            viewModel.setIntention(PostDetailFragmentEvent.GoBackToPreviousFragment)
        } else {
            viewModel.setIntention(PostDetailFragmentEvent.SetPost(post))
        }
    }

    private fun enableLoadingPost(display: Boolean) {
        if (display) post_detail_background_loading.visibility =
            View.VISIBLE else post_detail_background_loading.visibility = View.GONE
    }

    private fun getEmoji(codePoint: Int) {

    }
}

sealed class PostDetailFragmentEvent {
    object GetParcelableUpdatedPost : PostDetailFragmentEvent()
    object GetParcelablePost : PostDetailFragmentEvent()

    data class SetPost(val post: Post) : PostDetailFragmentEvent()
    object AddLike : PostDetailFragmentEvent()
    object AddRepost : PostDetailFragmentEvent()

    object HideOptions : PostDetailFragmentEvent()
    object ShowPostOptions : PostDetailFragmentEvent()

    data class ExecuteOption(val optionPositionIndex: Int) : PostDetailFragmentEvent()

    data class SaveUpdatedPost(val editedPost: Post) : PostDetailFragmentEvent()

    object CancelReport : PostDetailFragmentEvent()
    data class SendReport(val reportCause: String, val message: String) : PostDetailFragmentEvent()

    object GoToProfileFragment : PostDetailFragmentEvent()
    object GoBackToPreviousFragment : PostDetailFragmentEvent()
}