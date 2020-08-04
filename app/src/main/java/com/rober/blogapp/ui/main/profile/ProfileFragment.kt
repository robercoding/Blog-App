package com.rober.blogapp.ui.main.profile

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.rober.blogapp.R
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.util.state.DataState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_profile.*

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        subscribeObservers()

        var userName = arguments?.getString("userName")
        if(userName.isNullOrBlank()){
            Log.i("ProfileFragment", "Let's load our user")
            viewModel.setIntention(ProfileFragmentEvent.loadUserDetails(null))

        }else{
            Log.i("ProfileFragment", "Let's load other user")
        }





//        viewModel.login()
//        viewModel.saveUser(User(1, "Rober", "Valencia"))
//        viewModel.printSomething()
    }

    private fun subscribeObservers(){
        viewModel.profileState.observe(viewLifecycleOwner, Observer {dataState->
            when(dataState){
                is DataState.Success -> {
                    val user = dataState.data
                    uid_name.text = user.username
                    uid_biography.text = user.biography
                    uid_followers.text = "20 following"
                    uid_following.text = "30 followers"
                }
            }

        })
    }
}

sealed class ProfileFragmentEvent{
    data class loadUserDetails(val name: String? = null): ProfileFragmentEvent()
}