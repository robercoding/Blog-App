package com.rober.blogapp.ui.main.post.postdetail

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_detail.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

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

    private fun goToProfileFragment(user: User){
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
    }
}

sealed class PostDetailFragmentEvent {
    data class SetPost(val post: Post) : PostDetailFragmentEvent()
    object AddLike : PostDetailFragmentEvent()
    object AddRepost : PostDetailFragmentEvent()

    object GoToProfileFragment: PostDetailFragmentEvent()
    object GoBackToPreviousFragment : PostDetailFragmentEvent()
}