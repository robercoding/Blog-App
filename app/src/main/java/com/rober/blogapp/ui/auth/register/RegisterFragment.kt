package com.rober.blogapp.ui.auth.register

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.rober.blogapp.R
import com.rober.blogapp.ui.auth.AuthViewModel
import com.rober.blogapp.ui.auth.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.fragment_register.progress_bar

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var setErrorFieldsEvent: RegisterFragmentEvent.SetErrorFields
    private var username = ""
    private var email = ""
    private var password = ""


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
        setupListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner, Observer { authState ->
            render(authState)
        })
    }

    private fun render(authState: AuthState) {
        when (authState) {
            is AuthState.Registering -> {
                displayProgressBar(true)
            }
            is AuthState.SuccessRegister -> {
                displayProgressBar(false)
//                Toast.makeText(activity, "Succesfully registered", Toast.LENGTH_SHORT).show()
                viewModel.setRegisterIntention(RegisterFragmentEvent.LogIn(authState.email, authState.password))
            }

            is AuthState.CheckFields -> {
                displayProgressBar(true)
                if (validateFields())
                    viewModel.setRegisterIntention(RegisterFragmentEvent.SignUp(username, email, password))
                else
                    viewModel.setRegisterIntention(setErrorFieldsEvent)

            }

            is AuthState.UserLoggedIn -> {
                Toast.makeText(activity, "LoggedIn", Toast.LENGTH_SHORT).show()
                goToMainFragments()
            }

            is AuthState.SetErrorFields -> {
                displayProgressBar(false)
                cleanErrorFields()
                setErrorFields(
                    authState.usernameError,
                    authState.emailError,
                    authState.passwordLengthError,
                    authState.passwordRepeatError
                )
            }

            is AuthState.Error -> {
                displayProgressBar(false)
                errorMessage(authState.message)
            }
        }
    }


    private fun validateFields(): Boolean {
        username = register_input_text_username.text.toString()
        email = register_input_text_email.text.toString()
        password = register_input_text_password.text.toString()
        val passwordRepeat = register_input_text_password_repeat.text.toString()
        setErrorFieldsEvent = RegisterFragmentEvent.SetErrorFields(true, true, true, true)

        if (username.length < 5 || username.length > 15) {
            setErrorFieldsEvent.isUsernameLengthOk = false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setErrorFieldsEvent.isEmailOk = false
        }

        if (password.length <= 6) {
            setErrorFieldsEvent.isPasswordLengthOk = false
        }
        if (!passwordRepeat.equals(password)) {
            setErrorFieldsEvent.isPasswordRepeatOk = false
        }

        return (setErrorFieldsEvent.isUsernameLengthOk && setErrorFieldsEvent.isEmailOk && setErrorFieldsEvent.isPasswordLengthOk && setErrorFieldsEvent.isPasswordRepeatOk)
    }

    private fun setErrorFields(
        usernameError: String,
        emailError: String,
        passwordLengthError: String,
        passwordRepeatError: String
    ) {
        if (usernameError.isNotEmpty()) {
            register_input_layout_username.helperText = usernameError
        }

        if (emailError.isNotEmpty()) {
            register_input_layout_email.helperText = emailError
        }

        if (passwordLengthError.isNotEmpty()) {
            register_input_layout_password.helperText = passwordLengthError
            register_input_layout_password_repeat.helperText = passwordLengthError
        }

        if (passwordRepeatError.isNotEmpty()) {
            register_input_layout_password.helperText = passwordRepeatError
            register_input_layout_password_repeat.helperText = passwordRepeatError
//                register_input_layout_password_repeat.helperTextCurrentTextColor = colorRed
        }
    }


    private fun cleanErrorFields() {
        register_input_layout_username.helperText = ""
        register_input_layout_email.helperText = ""
        register_input_layout_password.helperText = ""
        register_input_layout_password_repeat.helperText = ""
    }

    private fun cleanErrorField(inputLayout: TextInputLayout) {
        inputLayout.helperText = ""
    }

    private fun goToMainFragments() {
        val navController: NavController = findNavController()
        navController.navigate(R.id.feedFragment)
    }

    fun displayProgressBar(isDisplayed: Boolean) {
        progress_bar.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    fun errorMessage(message: String?) {
        if (message != null)
            Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
        else
            Snackbar.make(requireView(), "There was an error in the server", Snackbar.LENGTH_SHORT).show()
    }

    private fun setupListeners() {
        btnSignUp.setOnClickListener {
            viewModel.setRegisterIntention(RegisterFragmentEvent.CheckFields)
        }

        register_input_text_username.addTextChangedListener {
            if(!register_input_layout_username.helperText.isNullOrEmpty())
                cleanErrorField(register_input_layout_username)
        }
        register_input_text_email.addTextChangedListener {
            if(!register_input_layout_email.helperText.isNullOrEmpty())
                cleanErrorField(register_input_layout_email)
        }

        register_input_text_password.addTextChangedListener {
            if(!register_input_layout_password.helperText.isNullOrEmpty()){
                cleanErrorField(register_input_layout_password)
                cleanErrorField(register_input_layout_password_repeat)
            }
        }
        register_input_text_password_repeat.addTextChangedListener {
            if(!register_input_layout_password_repeat.helperText.isNullOrEmpty()){
                cleanErrorField(register_input_layout_password)
                cleanErrorField(register_input_layout_password_repeat)
            }
        }
    }

    sealed class RegisterFragmentEvent {
        data class SignUp(val username: String, val email: String, val password: String) : RegisterFragmentEvent()
        data class LogIn(val email: String, val password: String) : RegisterFragmentEvent()
        data class SetErrorFields(
            var isUsernameLengthOk: Boolean,
            var isEmailOk: Boolean,
            var isPasswordLengthOk: Boolean,
            var isPasswordRepeatOk: Boolean
        ) :
            RegisterFragmentEvent()

        object CheckFields : RegisterFragmentEvent()
    }
}