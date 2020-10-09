package com.rober.blogapp.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job

abstract class BaseViewModel<STATE, EVENT> : ViewModel() {

    val TAG = javaClass.simpleName

    var job: Job? = null

    private val _viewStates: MutableLiveData<STATE> = MutableLiveData()

    fun viewStates(): LiveData<STATE> = _viewStates

    private var _viewState: STATE? = null
    protected var viewState: STATE
        get() = _viewState
            ?: throw UninitializedPropertyAccessException("\"viewState\" was queried before being initialized")
        set(value) {
            _viewState = value
            _viewStates.value = value
        }

    abstract fun setIntention(event: EVENT)

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}