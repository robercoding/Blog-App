package com.rober.blogapp.ui.auth.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rober.blogapp.R
import com.rober.blogapp.ui.auth.AuthViewModel
import com.rober.blogapp.ui.auth.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_login.*

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


    private fun activateListeners() {
        btnLogin.setOnClickListener {
            login()
        }
        btnRegisterEmail.setOnClickListener {
            goToRegisterFragment()
        }
    }

    private fun login(){
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()

        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(activity, "Email must be valid", Toast.LENGTH_SHORT).show()
            return
        }

        if(password.isEmpty()){
           return
        }

        viewModel.setLoginIntention(LoginFragmentEvent.Login(email, password))
    }


    private fun observeViewModel(){
        viewModel.authState.observe(viewLifecycleOwner, Observer {loginAuthState ->
            render(loginAuthState)
        })
    }

    private fun render(state: AuthState){
        when(state) {
            is AuthState.CheckingUserLoggedIn -> {
                displayProgressBar(true)
            }
            is AuthState.Logging -> {
                displayProgressBar(true)
            }
            is AuthState.UserLoggedIn -> {
                displayProgressBar(false)
                goToMainFragments()
            }
            is AuthState.UserLogout -> {
                displayProgressBar(false)
                Snackbar.make(requireView(), "Logout", Snackbar.LENGTH_SHORT).show()
            }
            is AuthState.Error -> {
                displayProgressBar(false)
                errorMessage(state.message)
            }
            is AuthState.Idle -> {

                displayProgressBar(false)
//                Snackbar.make(requireView(), "Idle", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMainFragments(){
        val navController: NavController = findNavController()

        navController.navigate(R.id.feedFragment)
    }

    private fun goToRegisterFragment(){
        val navController: NavController = findNavController()

        navController.navigate(R.id.action_loginFragment_to_registerFragment)
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

sealed class LoginFragmentEvent{
    data class Login(val email: String, val password:String) : LoginFragmentEvent()
}