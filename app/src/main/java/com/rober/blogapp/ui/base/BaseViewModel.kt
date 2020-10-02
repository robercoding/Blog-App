package com.rober.blogapp.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel<ViewState, BaseEvent> : ViewModel(){

    private val _viewState : MutableLiveData<ViewState> = MutableLiveData<ViewState>()

    val viewState : LiveData<ViewState>
        get() = _viewState

    open fun setIntention(event: BaseEvent) {}
}