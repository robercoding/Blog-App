package com.rober.blogapp.ui.main.post

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.util.state.PostAddState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_add.*
import kotlinx.android.synthetic.main.fragment_register.*
import java.util.*


@AndroidEntryPoint
class PostAddFragment : Fragment() {

    val viewModel: PostAddViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

//        val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.TopBar)
//        val localInflater = inflater.cloneInContext(contextThemeWrapper)
//
//
//        return localInflater.inflate(R.layout.fragment_post_add, container, false)
        //requireActivity().actionBar?.hide()

        //See delay of hide
        displayActionBar(false)

        return inflater.inflate(R.layout.fragment_post_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeViewModel()
    }

    fun observeViewModel(){
        viewModel.statePost.observe(viewLifecycleOwner, Observer { state ->
                       handleState(state)
        })
    }

    fun handleState(state : PostAddState){
        when(state){
            is PostAddState.Success<*> ->{
                Toast.makeText(requireContext(), "Post has been succesfully uploaded!", Toast.LENGTH_SHORT).show()
            }

            is PostAddState.Error<*> ->{
                Toast.makeText(requireContext(), state.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupListeners() {
        top_app_bar.setNavigationOnClickListener {

        }

        top_app_bar.setOnMenuItemClickListener {menuItem ->
            when(menuItem.itemId){
                R.id.save_post -> {
                    goToFeedFragment()
                    savePost()
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    private fun goToFeedFragment(){
        displayActionBar(true)
        val navController = findNavController()
        navController.navigate(R.id.feedFragment)
    }

    private fun displayActionBar(display: Boolean){
        if(display)
            (requireActivity() as AppCompatActivity).supportActionBar?.show()
        else
            (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    private fun isEmpty(title: String, text: String): Boolean{
        if(title.isEmpty() || text.isEmpty()){
            return true
        }
        return false
    }

    private fun savePost(){
        val title = post_add_title.text.toString()
        val text = post_add_text.text.toString()
        if(isEmpty(title, text)){
            Toast.makeText(requireContext(), "Fields can't be empty", Toast.LENGTH_SHORT).show()
        }

        val post = Post(0, "", title, text, "", Date(), 0)
        viewModel.setIntention(PostAddEvent.savePost(post))

    }
}

sealed class PostAddEvent(){
    data class savePost(var post:Post): PostAddEvent()

    object None: PostAddEvent()
}