package com.rober.blogapp.ui.main.search

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rober.blogapp.R
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.search.adapter.UserSearchAdapter
import com.rober.blogapp.util.RecyclerViewActionInterface
import com.rober.blogapp.util.state.DataState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_search.*


@AndroidEntryPoint
class SearchFragment : Fragment(), RecyclerViewActionInterface {
    private val TAG = "SearchFragment"

    private val viewModel: SearchViewModel by viewModels()
    lateinit var userSearchAdapter: UserSearchAdapter
    private var textSearch = ""
    private var alreadyFocusOnSearchText = false
    private var didUserJustEnterInFragment = true

    private var listUsers = mutableListOf<User>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListeners()

        userSearchAdapter = UserSearchAdapter(requireView(), R.layout.adapter_search_viewholder_user, this)
        subscribeObservers()
    }

    private fun subscribeObservers(){
        viewModel.searchState.observe(viewLifecycleOwner, Observer { searchState ->
            render(searchState)
        })
    }

    private fun render(searchState : SearchState){
        when(searchState){
            is SearchState.ReadySearchUser -> {
                ReadySearchUser()
            }

            is SearchState.StopSearchUser -> {
                StopSearchUser()
            }

            is SearchState.ShowResultSearch -> {
                search_text_cant_find_user.visibility = View.GONE
                search_text_cant_find_user.text = ""

                recycler_user_search.visibility = View.VISIBLE

                listUsers = searchState.listUsers.toMutableList()
                userSearchAdapter.setUsers(listUsers)
                recyclerAdapterApply()
            }

            is SearchState.EmptyResultsSearch -> {
                recycler_user_search.visibility = View.INVISIBLE
                listUsers = mutableListOf()

                search_text_cant_find_user.text = "@${searchState.searchUsername}"
                search_text_cant_find_user.visibility = View.VISIBLE
            }

            is SearchState.Loading -> {
                //Load
            }
        }
    }

    private fun recyclerAdapterApply(){
        recycler_user_search.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userSearchAdapter
        }
    }

    private fun setupListeners(){
        search_user_text.setOnClickListener {
            if(!alreadyFocusOnSearchText && !didUserJustEnterInFragment)
                viewModel.setIntention(SearchFragmentEvent.ReadySearchUser)
        }

        search_user_text.setOnFocusChangeListener(object: View.OnFocusChangeListener {
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if(hasFocus){
                    Log.i(TAG, "Focused")
                    viewModel.setIntention(SearchFragmentEvent.ReadySearchUser)
                    alreadyFocusOnSearchText = true
                }
            }
        })

        search_user_text.addTextChangedListener {
            val textToSearch = search_user_text.text.toString()
            didUserJustEnterInFragment = false
            viewModel.setIntention(SearchFragmentEvent.RetrieveUserByUsername(textToSearch))
        }

        search_arrow_back.setOnClickListener {

            viewModel.setIntention(SearchFragmentEvent.StopSearchUser)
        }

        search_top_app_bar.setOnMenuItemClickListener {menuItem ->
            when(menuItem.itemId){
                R.id.icon_settings -> {
                    findNavController().popBackStack()
                    true
                }

                else -> true
            }

        }
    }

    private fun ReadySearchUser(){
        Log.i(TAG, "Recover text= ${textSearch}")
        search_toolbar_profile_image.visibility = View.GONE
        search_arrow_back.visibility = View.VISIBLE
        search_top_app_bar.menu.findItem(R.id.icon_settings).isVisible = false

        enableSearchUserEditText(true)

        displayBottomNavigation(false)
        recycler_user_search.visibility = View.VISIBLE
    }

    private fun StopSearchUser(){
        alreadyFocusOnSearchText = false
        textSearch = search_user_text.text.toString()

        search_arrow_back.visibility = View.GONE
        search_toolbar_profile_image.visibility = View.VISIBLE
        search_top_app_bar.menu.findItem(R.id.icon_settings).isVisible = true

        enableSearchUserEditText(false)
        displayBottomNavigation(true)
        hideKeyBoard()
        recycler_user_search.visibility = View.GONE
    }

    private fun enableSearchUserEditText(enabled: Boolean){
        if (enabled){
            search_user_text.setText(textSearch)
            search_user_text.setSelection(textSearch.length)
        }else{
            search_user_text.setText("")
            search_user_text.clearFocus()
        }
    }

    private fun displayBottomNavigation(display: Boolean){
        val navController = activity?.bottom_navigation ?: return
        if(display) navController.visibility = View.VISIBLE else navController.visibility = View.GONE
    }

    private fun hideKeyBoard(){
        val imm: InputMethodManager =  context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun clickListenerOnPost(positionAdapter: Int) {}

    override fun clickListenerOnUser(positionAdapter: Int) {
        val user_id = listUsers[positionAdapter].username

        val navController = findNavController()
        val bundle_user_id = bundleOf("user_id" to user_id)
        hideKeyBoard()
        navController.navigate(R.id.action_searchFragment_to_profileFragment, bundle_user_id)
    }

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {}

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Resume")
        didUserJustEnterInFragment = true
    }


}

sealed class SearchFragmentEvent(){
    data class RetrieveUserByUsername(val searchUsername: String) : SearchFragmentEvent()
    object ReadySearchUser : SearchFragmentEvent()
    object StopSearchUser : SearchFragmentEvent()
}