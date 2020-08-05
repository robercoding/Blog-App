package com.rober.blogapp.ui.main.feed

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.util.state.FeedState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_feed.*

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels()
    private val TAG: String = "FeedFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        //private val listPosts = viewModel.getPosts()

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
        viewModel.feedState.observe(viewLifecycleOwner, Observer {dataState ->
            Log.i(TAG, "$dataState")

            when(dataState){

                is FeedState.SuccessListPostState -> {
                    displayProgressBar(false)
                    recycler_feed.apply {
                       layoutManager = LinearLayoutManager(requireContext())
                        adapter = PostAdapter(requireView(), dataState.data)
                        setHasFixedSize(true)
                        addOnScrollListener(object: RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                super.onScrolled(recyclerView, dx, dy)

                            }

                            override fun onScrollStateChanged(
                                recyclerView: RecyclerView,
                                newState: Int
                            ) {
                                super.onScrollStateChanged(recyclerView, newState)
                                if(!recyclerView.canScrollVertically(1)){
                                    viewModel.setIntention(FeedFragmentEvent.RetrievePosts(true))
                                }
                            }
                        })
                    }
                }
                is FeedState.Error -> {
                    Toast.makeText(requireContext(), dataState.message, Toast.LENGTH_SHORT).show()
                }


                is FeedState.GettingPostState -> {
                    displayProgressBar(true)

                }
            }
        })
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

    private fun displayProgressBar(isDisplayed: Boolean){
        progressbar.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

    private fun setupListeners(){
        fab_to_post_add.setOnClickListener {
            goToPostAdd()
        }

    //Example of sending a variable
//            val navController = findNavController()
//            val variable = "a variable has been sended"
//            val bundle = bundleOf("variable" to variable)
//            navController.navigate(R.id.profileFragment, bundle)

    }

    private fun goToPostAdd(){
        val navController = findNavController()
        navController.navigate(R.id.postAddFragment)
    }
}
sealed class FeedFragmentEvent{
    data class RetrievePosts(val morePosts: Boolean) : FeedFragmentEvent()
}