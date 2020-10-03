package com.rober.blogapp.ui.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar

abstract class BaseFragment<STATE, EVENT, VM: BaseViewModel<STATE, EVENT>>(private val fragmentView: Int) : Fragment() {

    abstract val viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.viewStates().observe(this, Observer {
            render(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(requireContext()).inflate(fragmentView, container, false)
//        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    abstract fun render(viewState: STATE)

    abstract fun displayLoadingFragment(display: Boolean)

    fun getEmoji(codePoint: Int): String {
        return String(Character.toChars(codePoint))
    }

    fun hideKeyBoard() {
        val imm: InputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    fun displayToast(message: String){
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun displaySnackbar(message: String){
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }
}