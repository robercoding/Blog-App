package com.rober.blogapp.ui.auth.login

import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.rober.blogapp.R
import com.rober.blogapp.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_login.*

@AndroidEntryPoint
class LoginFragment : BaseFragment<LoginState, LoginFragmentEvent, LoginViewModel>(R.layout.fragment_login) {

    override val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun render(viewState: LoginState) {
        when (viewState) {
            is LoginState.CheckingUserLoggedIn -> {
                displayProgressBar(login_progress_bar, true)
            }
            is LoginState.Logging -> {
                displayProgressBar(login_progress_bar, true)
            }
            is LoginState.UserLoggedIn -> {
                displayProgressBar(login_progress_bar, false)
                hideKeyBoard()
                goToMainFragments()
            }
            is LoginState.UserLogout -> {
                displayProgressBar(login_progress_bar, false)
                Snackbar.make(requireView(), "Logout", Snackbar.LENGTH_SHORT).show()
            }

            is LoginState.OfferEnableAccount -> {
                showMaterialDialogEnableAccount(viewState.message)
            }

            is LoginState.Error -> {
                displayProgressBar(login_progress_bar, false)
                displayToast(viewState.message)
            }
            is LoginState.Idle -> {

                displayProgressBar(login_progress_bar, false)
//                Snackbar.make(requireView(), "Idle", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun login() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()

        if (password.isEmpty()) {
            return
        }

        if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModel.setIntention(LoginFragmentEvent.LoginByEmail(email, password))
        } else {
            viewModel.setIntention(LoginFragmentEvent.LoginByUsername(email, password))
        }
    }

    private fun goToMainFragments() {
        val navController: NavController = findNavController()
        navController.navigate(R.id.action_loginFragment_to_feedFragment)
    }

    private fun goToRegisterFragment() {
        val navController: NavController = findNavController()
        navController.navigate(R.id.action_loginFragment_to_registerFragment)
    }

    private fun showMaterialDialogEnableAccount(message: String) {
        val materialDialog =
            MaterialAlertDialogBuilder(requireContext(), R.style.Settings_MaterialDialogTheme)

        materialDialog
            .setTitle("Enable account")
            .setMessage(message)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.color.primaryBackground))
            .setPositiveButton("Yes") { dialog, which ->
                viewModel.setIntention(LoginFragmentEvent.EnableAccount)
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }.show()
    }

    override fun setupListeners() {
        btnLogin.setOnClickListener {
            login()
        }
        btnRegisterEmail.setOnClickListener {
            goToRegisterFragment()
        }
    }
}

sealed class LoginFragmentEvent {
    data class LoginByEmail(val email: String, val password: String) : LoginFragmentEvent()
    data class LoginByUsername(val username: String, val password: String) : LoginFragmentEvent()

    object EnableAccount : LoginFragmentEvent()
}