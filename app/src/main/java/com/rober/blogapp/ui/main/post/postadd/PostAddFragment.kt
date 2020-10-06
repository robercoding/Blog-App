package com.rober.blogapp.ui.main.post.postadd

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_add.*
import org.threeten.bp.Instant


@AndroidEntryPoint
class PostAddFragment :
    BaseFragment<PostAddState, PostAddEvent, PostAddViewModel>(R.layout.fragment_post_add) {

    override val viewModel: PostAddViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayActionBar(false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.setIntention(PostAddEvent.LoadUserDetails)
    }

    override fun render(viewState: PostAddState) {
        when (viewState) {
            is PostAddState.SetUserDetail -> {
                setUserDetail(viewState.user)
                viewModel.setIntention(PostAddEvent.GetPostToEdit)
            }

            is PostAddState.GetPostToEdit -> {
                val isTherePostToEdit = getParcelablePostToEdit()

                if (!isTherePostToEdit) {
                    viewModel.setIntention(PostAddEvent.ReadyToWrite)
                }
            }

            is PostAddState.RenderPostToEditInView -> {
                renderPostToEditInView(viewState.post)
                viewModel.setIntention(PostAddEvent.ReadyToWrite)
            }

            is PostAddState.SaveOrUpdatePost -> {
                if (viewState.isPostToUpdate) {
                    updatePost()
                } else {
                    savePost()
                }
            }

            is PostAddState.GoToPostDetailAndUpdatePost -> {
                goToPostDetailFragment(viewState.post)
            }

            is PostAddState.PostHasBeenSaved -> {
                displayToast(viewState.messageUtil.message)
                goToFeedFragment()
                viewModel.setIntention(PostAddEvent.Idle)
            }

            is PostAddState.ReadyToWrite -> {
                setViewReadyToWrite()
            }

            is PostAddState.NotifyErrorFieldValidation -> {
                displayToast("Fields can't be empty")
                viewModel.setIntention(PostAddEvent.Idle)
            }

            is PostAddState.Error -> {
                displayToast(viewState.exception.message.toString())
            }

            is PostAddState.Idle -> {
                //Nothing
            }
        }
    }

    private fun setUserDetail(user: User) {
        Glide.with(requireView())
            .load(user.profileImageUrl)
            .into(post_add_profile_picture)
    }

    private fun renderPostToEditInView(post: Post) {
        post_add_title.setText(post.title)
        post_add_text.setText(post.text)
    }

    private fun getParcelablePostToEdit(): Boolean {
        val postToEdit = arguments?.getParcelable<Post>("postToEdit")

        postToEdit?.run {
            viewModel.setIntention(PostAddEvent.RenderPostToEditInView(this))
            return true
        }
        return false
    }

    override fun setupListeners() {
        top_app_bar.setNavigationOnClickListener {
            goToFeedFragment()
        }

        top_app_bar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save_post -> {
                    viewModel.setIntention(PostAddEvent.ReadyToSaveOrUpdatePost)
//                    goToFeedFragment()
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    private fun setViewReadyToWrite() {
        post_add_title.requestFocus()
        //displayKeyBoard(true)
    }

    private fun goToPostDetailFragment(updatedPost: Post) {
        displayKeyBoard(false)
        displayActionBar(false)
        val updatedPostBundle = bundleOf("updatedPost" to updatedPost)
        if (findNavController().currentDestination?.id == R.id.postAddFragment) { //Solution conditional to the crash that says that "we are in other fragment"
            findNavController().navigate(R.id.action_postAddFragment_to_postDetailFragment, updatedPostBundle)
        }
    }

    private fun goToFeedFragment() {
        displayKeyBoard(false)
        displayActionBar(false)
        findNavController().popBackStack()
    }

    private fun displayActionBar(display: Boolean) {
        if (display)
            (requireActivity() as AppCompatActivity).supportActionBar?.show()
        else
            (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    private fun updatePost() {
        val title = post_add_title.text.toString()
        val text = post_add_text.text.toString()

        if (isEmpty(title, text)) {
            viewModel.setIntention(PostAddEvent.NotifyErrorFieldValidation)
            return
        }

        viewModel.setIntention(PostAddEvent.UpdatePost(title, text))
    }

    private fun savePost() {
        val title = post_add_title.text.toString()
        val text = post_add_text.text.toString()
        if (isEmpty(title, text)) {
            Toast.makeText(requireContext(), "Fields can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val post = Post(0, "", title, text, "", Instant.now().epochSecond, 0)

        viewModel.setIntention(PostAddEvent.SavePost(post))
    }

    private fun isEmpty(title: String, text: String): Boolean {
        if (title.isEmpty() || text.isEmpty()) {
            return true
        }
        return false
    }

    private fun displayKeyBoard(display: Boolean) {
        val imm: InputMethodManager =
            context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        if (display)
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        else
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}

sealed class PostAddEvent {
    object LoadUserDetails : PostAddEvent()

    object GetPostToEdit : PostAddEvent()
    data class RenderPostToEditInView(val post: Post) : PostAddEvent()
    object ReadyToWrite : PostAddEvent()

    object ReadyToSaveOrUpdatePost : PostAddEvent()
    data class UpdatePost(val title: String, val text: String) : PostAddEvent()
    data class SavePost(val post: Post) : PostAddEvent()

    object NotifyErrorFieldValidation : PostAddEvent()

    object Idle : PostAddEvent()
}