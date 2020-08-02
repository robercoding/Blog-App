package com.rober.blogapp.ui.auth.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rober.blogapp.R
import com.rober.blogapp.ui.auth.AuthViewModel
import com.rober.blogapp.util.state.AuthStateEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        activateListeners()

    }

    private fun observeViewModel(){
        viewModel.loginAuthState.onEach {
                state -> handleAuthState(state)
        }.launchIn(lifecycleScope)
    }

    private fun activateListeners(){
        btnLogin.setOnClickListener {
            viewModel.login(etEmail.text.toString(), etPassword.text.toString())
        }
        btnRegisterEmail.setOnClickListener {
            goToRegisterFragment()
        }
    }

    private fun handleAuthState(state: AuthStateEvent){
        when(state) {
            is AuthStateEvent.CheckingUserLoggedIn -> {
                displayProgressBar(true)
            }
            is AuthStateEvent.Logging -> {
                displayProgressBar(true)
            }
            is AuthStateEvent.UserLoggedIn -> {
                displayProgressBar(false)
                viewModel.getAndSetCurrentUser()
                goToMainFragments()
            }
            is AuthStateEvent.UserLogout -> {
                displayProgressBar(false)
                Snackbar.make(requireView(), "Logout", Snackbar.LENGTH_SHORT).show()
            }
            is AuthStateEvent.Error -> {
                displayProgressBar(false)
                errorMessage(state.message)
            }
            is AuthStateEvent.Idle -> {
                displayProgressBar(false)
                Snackbar.make(requireView(), "Idle", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMainFragments(){
        val navController: NavController = findNavController()

        navController.navigate(R.id.feedFragment)
    }

    private fun goToRegisterFragment(){
        val navController: NavController = findNavController()

        navController.navigate(R.id.registerFragment)
    }


    fun errorMessage(message: String?){
        if(message != null)
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
        else
            Snackbar.make(requireView(), "There was an error in the server", Snackbar.LENGTH_SHORT).show()
    }


    fun displayProgressBar(isDisplayed: Boolean){
        progress_bar.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

}