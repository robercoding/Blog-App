package com.rober.blogapp.ui.base

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel

abstract class BaseFragment<ViewState, BaseEvent, VM: BaseViewModel<ViewState, BaseEvent>> : Fragment() {

    abstract val viewModel: VM

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.viewState.observe(this, Observer {
            render(it)
        })
    }

    abstract fun render(viewState: ViewState)

    fun getEmoji(codePoint: Int): String {
        return String(Character.toChars(codePoint))
    }

    fun hideKeyBoard() {
        val imm: InputMethodManager = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}