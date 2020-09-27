package com.rober.blogapp.ui.main.post.postdetail

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rober.blogapp.R
import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.post.postdetail.adapter.ListOptionsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_detail.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId

@AndroidEntryPoint
class PostDetailFragment : Fragment() {

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
        post_detail_toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.blueTwitter))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AndroidThreeTen.init(requireContext())

        setupObservers()

        val post = arguments?.getParcelable<Post>("post")

        if (post == null) {
            viewModel.setIntention(PostDetailFragmentEvent.GoBackToPreviousFragment)
        } else {
            viewModel.setIntention(PostDetailFragmentEvent.SetPost(post))
        }
    }

    private fun setupObservers() {
        viewModel.postDetailState.observe(viewLifecycleOwner, Observer { postDetailState ->
            render(postDetailState)
        })
    }

    private fun render(postDetailState: PostDetailState) {
        when (postDetailState) {
            is PostDetailState.SetPostDetails -> {
                setPostDetails(postDetailState.post)
                setUserDetails(postDetailState.user)
            }
            is PostDetailState.BackToPreviousFragment -> {
                moveToFeedFragment()
            }
            is PostDetailState.GoToProfileFragment -> {
                goToProfileFragment(postDetailState.user)
            }

            is PostDetailState.ShowPostOptions -> {
                val listOptions = postDetailState.listOptions
                post_detail_motion_layout_container.visibility = View.VISIBLE
                main_view_background_opaque.visibility = View.VISIBLE
//                val listOptionsTest = listOf<String>("Hey lets see", "Other one!")
                val listOptionsTest = listOf<Option>(Option(R.drawable.ic_location, "DeletePost"), Option(R.drawable.ic_location, "Test2"))
                post_detail_options_list.adapter =
                     ListOptionsAdapter(requireContext(), listOptionsTest)
//                post_detail_options_list.adapter =
//                    ArrayAdapter(requireContext(), android.R.layout.simple_expandable_list_item_1, listOptionsTest)


                val layout = post_detail_options_list.layoutParams
                val metrics = DisplayMetrics()
                activity?.windowManager?.defaultDisplay?.getMetrics(metrics)
                val width = metrics.widthPixels
                layout.width = width
                layout.height = ViewGroup.LayoutParams.WRAP_CONTENT
                post_detail_options_list.layoutParams = layout

                Toast.makeText(requireContext(), "To end theorically", Toast.LENGTH_SHORT).show()
                post_detail_motion_layout_container.transitionToEnd()
            }

            is PostDetailState.Idle -> {
                //Nothing
            }
        }
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

        val fmtDate = org.threeten.bp.format.DateTimeFormatter.ofPattern("dd/MM/yy")
        val fmtTime = org.threeten.bp.format.DateTimeFormatter.ofPattern("HH:mm")

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

    private fun moveToFeedFragment() {
        val navController = findNavController()
        navController.popBackStack()
    }

    private fun goToProfileFragment(user: User) {
        val navController = findNavController()
        val bundle = bundleOf("user" to user)
        navController.navigate(R.id.profileDetailFragment, bundle)
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

//        post_detail_space.setOnClickListener {
//            Log.i("PostDetailMotion", "Space touch")
//        }
        post_detail_options_list.setOnItemClickListener { parent, view, position, id ->
            Log.i(
                "PostDetailMotion",
                "List touch"
            )
        }

        post_detail_motion_layout_container.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
//                if(p0?.getConstraintSet(R.id.start) == post_detail_motion_layout_container.getConstraintSet(R.id.start)){
//                    Toast.makeText(requireContext(), "Is start", Toast.LENGTH_SHORT).show()
//                    post_detail_motion_layout_container.visibility = View.GONE
//                }
                if (p0?.currentState == post_detail_motion_layout_container.startState) {
                    Toast.makeText(requireContext(), "Is start", Toast.LENGTH_SHORT).show()
                    post_detail_motion_layout_container.visibility = View.GONE
                    main_view_background_opaque.visibility = View.GONE
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
}

sealed class PostDetailFragmentEvent {
    data class SetPost(val post: Post) : PostDetailFragmentEvent()
    object AddLike : PostDetailFragmentEvent()
    object AddRepost : PostDetailFragmentEvent()

    object ShowPostOptions : PostDetailFragmentEvent()

    object GoToProfileFragment : PostDetailFragmentEvent()
    object GoBackToPreviousFragment : PostDetailFragmentEvent()
}