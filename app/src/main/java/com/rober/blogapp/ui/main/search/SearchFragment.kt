package com.rober.blogapp.ui.main.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.rober.blogapp.R
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.search.adapter.UserSearchAdapter
import com.rober.blogapp.util.state.DataState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_search.*


@AndroidEntryPoint
class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()

    lateinit var userSearchAdapter: UserSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        userSearchAdapter = UserSearchAdapter(requireView(), R.layout.adapter_search_viewholder_user)
        subscribeObservers()
    }

    private fun subscribeObservers(){
        viewModel.userList.observe(viewLifecycleOwner, Observer {dataState ->
            when(dataState){
                is DataState.Success -> {
                    if(dataState.data.isEmpty()){
                        userSearchAdapter.setUsers(mutableListOf())
                        recyclerAdapterApply()
                    }else{
                        userSearchAdapter.setUsers(dataState.data.toMutableList())
                        recyclerAdapterApply()

                    }
                }
            }
        })
    }

    private fun recyclerAdapterApply(){
        recycler_user_search.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userSearchAdapter
        }
    }

    private fun setupListeners(){
        search_user_text.addTextChangedListener {
            val textToSearch = search_user_text.text.toString()
            if(!textToSearch.isEmpty())
                viewModel.setIntention(SearchFragmentEvent.RetrieveUserByUsername(textToSearch))
        }
    }
}

sealed class SearchFragmentEvent(){
    data class RetrieveUserByUsername(val searchUsername: String) : SearchFragmentEvent()
}