package com.rober.blogapp.ui.main.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rober.blogapp.R
import dagger.hilt.android.AndroidEntryPoint

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


//        viewModel.login()
//        viewModel.saveUser(User(1, "Rober", "Valencia"))
//        viewModel.printSomething()
    }
}