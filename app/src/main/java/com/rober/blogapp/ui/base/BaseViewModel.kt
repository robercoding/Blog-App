package com.rober.blogapp.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel<State, Event> : ViewModel(){

    private val _viewStates: MutableLiveData<State> = MutableLiveData()
    fun viewStates() : LiveData<State> = _viewStates

    private var _viewState : State? = null
    protected var viewState : State
        get() = _viewState ?: throw UninitializedPropertyAccessException("\"viewState\" was queried before being initialized")

        set(value) {
            _viewState = value
            _viewStates.value = value
        }

        abstract fun setIntention(event: Event)


}