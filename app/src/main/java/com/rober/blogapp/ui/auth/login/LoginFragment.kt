package com.rober.blogapp.ui.auth.login

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
                displayProgressBar(login_progress_bar, false)
            }

            is LoginState.EnablingAccount -> {
                displayOpaqueBackground(true)
                displayCenterProgressBar(true)
                enableTouchView(login_scroll_view, false)
            }

            is LoginState.EnabledAccount -> {
                displayOpaqueBackground(false)
                displayCenterProgressBar(false)
                displayToast(viewState.message)
                enableTouchView(login_scroll_view, true)
                login()
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

    private fun enableTouchView(view: View, enableTouch: Boolean) {
        view.isEnabled = enableTouch
        if (view is ViewGroup) {
            val viewgroup = view as ViewGroup
            for (index in 0 until viewgroup.childCount) {
                val child = viewgroup.getChildAt(index)
                enableTouchView(child, enableTouch)
            }
        }
    }

    override fun setupListeners() {
        btnLogin.setOnClickListener {
            login()
        }
        btnRegisterEmail.setOnClickListener {
            goToRegisterFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setIntention(LoginFragmentEvent.CheckIfAlreadyLogin)
    }
}

sealed class LoginFragmentEvent {
    data class LoginByEmail(val email: String, val password: String) : LoginFragmentEvent()
    data class LoginByUsername(val username: String, val password: String) : LoginFragmentEvent()

    object CheckIfAlreadyLogin : LoginFragmentEvent()


    object EnableAccount : LoginFragmentEvent()
}