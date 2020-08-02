package com.rober.blogapp.ui.main.post

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.rober.blogapp.R
import com.rober.blogapp.util.state.DataState
import com.rober.blogapp.util.state.PostAddState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_add.*


@AndroidEntryPoint
class PostAddFragment : Fragment() {

    val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_add, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeViewModel()
    }

    fun observeViewModel(){
        viewModel._statePost.observe(viewLifecycleOwner, Observer {state ->
                       handleState(state)
        })
    }

    fun handleState(state : PostAddState){
        when(state){
            is PostAddState.Success<*> ->{
                if(state.data!= null){
                    Log.i("PostAddFragment", state.data.toString())
                    post_add_username.text = state.data.toString()
                }
            }

            is PostAddState.Error<*> ->{
                Toast.makeText(requireContext(), state.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }

    }






}