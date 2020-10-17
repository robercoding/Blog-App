package com.rober.blogapp.ui.auth.register

import android.os.Bundle
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.rober.blogapp.R
import com.rober.blogapp.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_register.*

@AndroidEntryPoint
class RegisterFragment :
    BaseFragment<RegisterState, RegisterFragmentEvent, RegisterViewModel>(R.layout.fragment_register) {

    override val viewModel: RegisterViewModel by viewModels()
    private lateinit var setErrorFieldsEvent: RegisterFragmentEvent.SetErrorFields
    private var username = ""
    private var email = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun render(viewState: RegisterState) {
        when (viewState) {
            is RegisterState.Registering -> {
                displayProgressBar(register_progress_bar, true)
            }
            is RegisterState.SuccessRegister -> {
                displayProgressBar(register_progress_bar, false)
//                Toast.makeText(activity, "Succesfully registered", Toast.LENGTH_SHORT).show()
                viewModel.setIntention(RegisterFragmentEvent.Login(viewState.email, viewState.password))
            }

            is RegisterState.CheckFields -> {
                displayProgressBar(register_progress_bar, true)
                if (validateFields())
                    viewModel.setIntention(RegisterFragmentEvent.SignUp(username, email, password))
                else
                    viewModel.setIntention(setErrorFieldsEvent)

            }

            is RegisterState.UserLoggedIn -> {
                goToFeedFragment()
                hideKeyBoard()
            }

            is RegisterState.SetErrorFields -> {
                displayProgressBar(register_progress_bar, false)
                cleanErrorFields()
                setErrorFields(
                    viewState.usernameError,
                    viewState.emailError,
                    viewState.passwordLengthError,
                    viewState.passwordRepeatError
                )
            }

            is RegisterState.Error -> {
                displayProgressBar(register_progress_bar, false)
                displayToast(viewState.message)
            }
        }
    }

    private fun validateFields(): Boolean {
        username = register_input_text_username.text.toString()
        email = register_input_text_email.text.toString()
        password = register_input_text_password.text.toString()
        val passwordRepeat = register_input_text_password_repeat.text.toString()
        setErrorFieldsEvent = RegisterFragmentEvent.SetErrorFields(
            isUsernameLengthOk = true,
            isEmailOk = true,
            isPasswordLengthOk = true,
            isPasswordRepeatOk = true
        )

        if (username.length < 5 || username.length > 15) {
            setErrorFieldsEvent.isUsernameLengthOk = false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setErrorFieldsEvent.isEmailOk = false
        }

        if (password.length <= 6) {
            setErrorFieldsEvent.isPasswordLengthOk = false
        }
        if (passwordRepeat != password) {
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
        }
    }


    private fun cleanErrorFields() {
        register_input_layout_username.helperText = ""
//        register_input_layout_username.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.text_input_box_stroke)
//        register_input_layout_email.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.text_input_box_stroke)
//        register_input_layout_password.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.text_input_box_stroke)
//        register_input_layout_password_repeat.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.text_input_box_stroke)
        register_input_layout_email.helperText = ""
        register_input_layout_password.helperText = ""
        register_input_layout_password_repeat.helperText = ""
    }

    private fun cleanErrorField(inputLayout: TextInputLayout) {
        inputLayout.helperText = ""
    }

    private fun goToFeedFragment() {
        val navController: NavController = findNavController()
        navController.navigate(R.id.action_registerFragment_to_feedFragment)
    }

    override fun setupListeners() {
        btnSignUp.setOnClickListener {
            viewModel.setIntention(RegisterFragmentEvent.CheckFields)
        }

        register_input_text_username.addTextChangedListener {
            if (!register_input_layout_username.helperText.isNullOrEmpty())
                cleanErrorField(register_input_layout_username)
        }
        register_input_text_email.addTextChangedListener {
            if (!register_input_layout_email.helperText.isNullOrEmpty())
                cleanErrorField(register_input_layout_email)
        }

        register_input_text_password.addTextChangedListener {
            if (!register_input_layout_password.helperText.isNullOrEmpty()) {
                cleanErrorField(register_input_layout_password)
                cleanErrorField(register_input_layout_password_repeat)
            }
        }
        register_input_text_password_repeat.addTextChangedListener {
            if (!register_input_layout_password_repeat.helperText.isNullOrEmpty()) {
                cleanErrorField(register_input_layout_password)
                cleanErrorField(register_input_layout_password_repeat)
            }
        }
    }

    override fun setupViewDesign() {
        super.setupViewDesign()
        requireActivity().window.setResize()
    }
}

sealed class RegisterFragmentEvent {
    data class SignUp(val username: String, val email: String, val password: String) : RegisterFragmentEvent()
    data class Login(val email: String, val password: String) : RegisterFragmentEvent()
    data class SetErrorFields(
        var isUsernameLengthOk: Boolean,
        var isEmailOk: Boolean,
        var isPasswordLengthOk: Boolean,
        var isPasswordRepeatOk: Boolean
    ) :
        RegisterFragmentEvent()

    object CheckFields : RegisterFragmentEvent()
}