package com.rober.blogapp.ui.auth.register

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.rober.blogapp.R
import com.rober.blogapp.ui.auth.AuthViewModel
import com.rober.blogapp.ui.auth.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.fragment_register.etEmail
import kotlinx.android.synthetic.main.fragment_register.progress_bar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activateListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeViewModel()

    }

    private fun activateListeners(){
        btnSignUp.setOnClickListener {
            signUp()
        }
    }

    private fun signUp(){
        val name = etName.text.toString()
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        val passwordRepeat = etPasswordRepeat.text.toString()

        if(name.isEmpty()){
            Toast.makeText(activity, "Name can't be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(activity, "Email must be valid", Toast.LENGTH_SHORT).show()
            return
        }

        if(password.length <= 6){
            Toast.makeText(activity, "Password must contain at least 7 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if(!passwordRepeat.equals(password)){
            Toast.makeText(activity, "Passwords must be equals", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.setRegisterIntetion(RegisterFragmentEvent.SignUp(email, password, name))
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner, Observer {authState ->
            render(authState)
        })
    }

    private fun render(authState: AuthState) {
        when(authState){
            is AuthState.Registering -> {
                displayProgressBar(true)
            }
            is AuthState.SuccessRegister -> {
                displayProgressBar(false)
//                Toast.makeText(activity, "Succesfully registered", Toast.LENGTH_SHORT).show()
                viewModel.setRegisterIntetion(RegisterFragmentEvent.LogIn(authState.email, authState.password))
            }

            is AuthState.UserLoggedIn -> {
                Toast.makeText(activity, "LoggedIn", Toast.LENGTH_SHORT).show()
                goToMainFragments()
            }

            is AuthState.Error -> {
                displayProgressBar(false)
                errorMessage(authState.message)
            }
        }
    }

    private fun goToMainFragments(){
        val navController: NavController = findNavController()
        navController.navigate(R.id.feedFragment)
    }

    fun displayProgressBar(isDisplayed: Boolean){
        progress_bar.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

    fun errorMessage(message: String?){
        if(message != null)
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
        else
            Snackbar.make(requireView(), "There was an error in the server", Snackbar.LENGTH_SHORT).show()
    }
}

sealed class RegisterFragmentEvent{
    data class SignUp(val email: String, val password: String, val name:String): RegisterFragmentEvent()
    data class LogIn(val email: String, val password: String): RegisterFragmentEvent()
}