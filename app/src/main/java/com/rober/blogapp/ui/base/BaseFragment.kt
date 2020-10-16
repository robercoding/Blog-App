package com.rober.blogapp.ui.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.rober.blogapp.ui.MainActivity

abstract class BaseFragment<STATE, EVENT, VM : BaseViewModel<STATE, EVENT>>(
    private val fragmentView: Int
) :
    Fragment() {

    abstract val viewModel: VM

    val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.viewStates().observe(this, Observer {
            render(it)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return LayoutInflater.from(requireContext()).inflate(fragmentView, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupViewDesign()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupObjects()
    }

    abstract fun render(viewState: STATE)

    abstract fun setupListeners()

    open fun setupObjects() {}

    open fun setupViewDesign() {}

    open fun customActionOnBackPressed(action: Int = 0) {}

    open fun displayLoadingFragment(display: Boolean) {}

    open fun displayLoadingView(display: Boolean) {}

    fun getColor(color: Int): Int {
        return ContextCompat.getColor(requireContext(), color)
    }

    fun hideKeyBoard() {
        val imm: InputMethodManager =
            context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    fun displayToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun displaySnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    fun restoreDefaultOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {}
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    fun displayOpaqueBackground(display: Boolean) {
        (requireActivity() as MainActivity).displayOpaqueBackground(display)
    }

    fun displayCenterProgressBar(display: Boolean) {
        (requireActivity() as MainActivity).displayCenterProgressBar(display)
    }

    open fun displayProgressBar(view: View, display: Boolean) {
        if (display)
            view.show()
        else
            view.hide()
    }

    fun getEmoji(codePoint: Int): String {
        return String(Character.toChars(codePoint))
    }

    private fun View.hide() {
        this.visibility = View.GONE
    }

    private fun View.show() {
        this.visibility = View.VISIBLE
    }
}