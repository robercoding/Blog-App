package com.rober.blogapp.ui.main.search

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.search.adapter.UserSearchAdapter
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_search.*

@AndroidEntryPoint
class SearchFragment :
    BaseFragment<SearchState, SearchFragmentEvent, SearchViewModel>(R.layout.fragment_search),
    RecyclerViewActionInterface {

    override val viewModel: SearchViewModel by viewModels()
    lateinit var userSearchAdapter: UserSearchAdapter
    private var textSearch = ""
    private var alreadyFocusOnSearchText = false

    private var listUsers = mutableListOf<User>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.setIntention(SearchFragmentEvent.LoadUserDetails)
    }

    override fun render(viewState: SearchState) {
        when (viewState) {
            is SearchState.SetUserDetails -> {
                setUserDetails(viewState.user)
                stopSearchUser()
            }

            is SearchState.ReadySearchUser -> {
                readySearchUser()
            }

            is SearchState.StopSearchUser -> {
                stopSearchUser()
            }

            is SearchState.ShowResultSearch -> {
                search_text_cant_find_user.visibility = View.GONE
                search_text_cant_find_user.text = ""

                recycler_user_search.visibility = View.VISIBLE

                listUsers = viewState.listUsers.toMutableList()
                userSearchAdapter.setUsers(listUsers)
                recyclerAdapterApply()
            }

            is SearchState.EmptyResultsSearch -> {
                recycler_user_search.visibility = View.INVISIBLE
                listUsers = mutableListOf()

                search_text_cant_find_user.text = "@${viewState.searchUsername}"
                search_text_cant_find_user.visibility = View.VISIBLE
            }

            is SearchState.GoToProfileFragment -> {
                textSearch = search_user_text.text.toString()
                goToProfileFragment(viewState.user)
            }

            is SearchState.GoToSettingsFragment -> {
                findNavController().navigate(R.id.action_searchFragment_to_settingsFragment)
            }

            is SearchState.Loading -> {
                //Load
            }
        }
    }

    private fun setUserDetails(user: User) {
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(requireView())
                .load(user.profileImageUrl)
                .into(search_toolbar_profile_image)
        }
    }

    private fun recyclerAdapterApply() {
        recycler_user_search.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userSearchAdapter
        }
    }

    override fun setupListeners() {
        search_user_text.setOnClickListener {
            if (!alreadyFocusOnSearchText)
                viewModel.setIntention(SearchFragmentEvent.ReadySearchUser)
        }

        search_user_text.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                viewModel.setIntention(SearchFragmentEvent.ReadySearchUser)
                alreadyFocusOnSearchText = true
            }
        }

        search_user_text.addTextChangedListener {
            val textToSearch = search_user_text.text.toString()
            viewModel.setIntention(SearchFragmentEvent.RetrieveUserByUsername(textToSearch))
        }

        search_arrow_back.setOnClickListener {
            viewModel.setIntention(SearchFragmentEvent.StopSearchUser)
        }

        search_top_app_bar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.icon_settings -> {
                    viewModel.setIntention(SearchFragmentEvent.GoToSettingsFragment)
                    true
                }

                else -> true
            }
        }
    }

    override fun setupObjects() {
        super.setupObjects()
        userSearchAdapter = UserSearchAdapter(requireView(), R.layout.adapter_search_viewholder_user, this)
    }

    private fun readySearchUser() {
        search_toolbar_profile_image.visibility = View.GONE
        search_top_app_bar.menu.findItem(R.id.icon_settings).isVisible = false

        search_arrow_back.visibility = View.VISIBLE
        recycler_user_search.visibility = View.VISIBLE

        enableSearchUserEditText(true)
        displayBottomNavigation(false)
    }

    private fun stopSearchUser() {
        textSearch = search_user_text.text.toString()
        alreadyFocusOnSearchText = false

        displayBottomNavigation(true)
        search_toolbar_profile_image.visibility = View.VISIBLE
        search_top_app_bar.menu.findItem(R.id.icon_settings).isVisible = true


        hideKeyBoard()
        enableSearchUserEditText(false)
        recycler_user_search.visibility = View.GONE
        search_arrow_back.visibility = View.GONE
    }

    private fun enableSearchUserEditText(enabled: Boolean) {
        if (enabled) {
            search_user_text.requestFocus()
            search_user_text.setText(textSearch)
            search_user_text.setSelection(textSearch.length)
        } else {
            search_user_text.setText("")
            search_user_text.clearFocus()
        }
    }

    private fun goToProfileFragment(user: User) {
//        val userId = listUsers[positionAdapter].userId

        val navController = findNavController()
        val bundleUserObject = bundleOf("userObject" to user)
        hideKeyBoard()
        navController.navigate(R.id.action_searchFragment_to_profileFragment, bundleUserObject)
    }

    private fun displayBottomNavigation(display: Boolean) {
        val navController = activity?.bottom_navigation ?: return
        if (display) navController.visibility = View.VISIBLE else navController.visibility = View.GONE
    }

    override fun clickListenerOnPost(positionAdapter: Int) {}

    override fun clickListenerOnUser(positionAdapter: Int) {
        viewModel.setIntention(SearchFragmentEvent.GoToProfileFragment(positionAdapter))
    }

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {}

    override fun clickListenerOnItem(positionAdapter: Int) {}

    override fun onResume() {
        super.onResume()
        viewModel.setIntention(SearchFragmentEvent.LoadUserDetails)
    }
}

sealed class SearchFragmentEvent {
    object LoadUserDetails : SearchFragmentEvent()
    data class RetrieveUserByUsername(val searchUsername: String) : SearchFragmentEvent()

    object ReadySearchUser : SearchFragmentEvent()
    object StopSearchUser : SearchFragmentEvent()

    data class GoToProfileFragment(val positionAdapter: Int) : SearchFragmentEvent()
    object GoToSettingsFragment : SearchFragmentEvent()
}