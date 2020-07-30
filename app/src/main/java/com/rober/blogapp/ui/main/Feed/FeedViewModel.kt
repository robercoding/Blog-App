package com.rober.blogapp.ui.main.Feed

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.data.room.repository.RoomRepository

class FeedViewModel
@ViewModelInject
constructor(
    private val firebaseRepository: FirebaseRepository,
    private val roomRepository: RoomRepository,
    @Assisted savedStateHandle: SavedStateHandle
): ViewModel(){
    private val TAG = "FeedViewModel"

    //private val _userSta
}
