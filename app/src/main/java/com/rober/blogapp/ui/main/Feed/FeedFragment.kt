package com.rober.blogapp.ui.main.Feed

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rober.blogapp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels()
    private val TAG: String = "FeedFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        val user = User(1, "Rober", "Valencia")
//        insertUser(user)
//        val user2 = User(2, "Ferran", "Valencia")
//        insertUser(user2)
//        val user3 = User(3, "Mew", "Valencia")
//        insertUser(user3)
//        for(i in 1..15){
//            insertBlog(i)
//        }

        subscribeObservers()
        //viewModel.setStateEvent(FeedStateEvent.GetUserWithBlogs)
        //viewModel.deleteUser(user)
    }

    private fun subscribeObservers(){
//        viewModel.usersWithBlogsState.observe(viewLifecycleOwner, Observer {dataState ->
//            Log.i(TAG, "Data: $dataState")
//            when(dataState){
//                is DataState.Success<List<UserWithBlogs>> -> {
//                    progressbar.visibility = ProgressBar.GONE
//                    Log.i(TAG, "List1: ${dataState}")
//                }
//
//                is DataState.Loading -> {
//                    progressbar.visibility = ProgressBar.VISIBLE
//                    Log.i(TAG, "Loading")
//                }
//                is DataState.Error -> {
//                    Log.i(TAG, "Error ${dataState.exception}")
//                }
//            }
//
//        })
    }
//
//    private fun insertUser(user: User){
//        viewModel.insertUser(user)
//    }
//    private fun insertBlog(i: Int){
//        viewModel.insertBlog(i)
//    }

}