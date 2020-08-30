package com.rober.blogapp.di.modules

import com.rober.blogapp.data.network.firebase.*
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.data.network.util.FirebaseErrors
import com.rober.blogapp.data.network.util.FirebasePath
//import com.rober.blogapp.data.room.dao.BlogDao
//import com.rober.blogapp.data.room.dao.UserDao
//import com.rober.blogapp.data.room.repository.RoomRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

//    @Singleton
//    @Provides
//    fun provideRoomRepository(userDao: UserDao, blogDao: BlogDao): RoomRepository = RoomRepository(userDao, blogDao)

    @Singleton
    @Provides
    fun provideFirebaseSource(): FirebaseSource = FirebaseSource()

    @Singleton
    @Provides
    fun provideFirebaseErrors(): FirebaseErrors = FirebaseErrors()

    @Singleton
    @Provides
    fun provideFirebasePath(): FirebasePath = FirebasePath()

    @Singleton
    @Provides
    fun provideFirebaseAuthManager(firebaseSource: FirebaseSource, firebaseErrors: FirebaseErrors): FirebaseAuthManager =
        FirebaseAuthManager(firebaseSource, firebaseErrors)

    @Singleton
    @Provides
    fun provideFirebaseFeedManager(firebaseSource: FirebaseSource): FirebaseFeedManager = FirebaseFeedManager(firebaseSource)

    @Singleton
    @Provides
    fun provideFirebasePostAddManager(firebaseSource: FirebaseSource, firebasePath: FirebasePath): FirebasePostAddManager =
        FirebasePostAddManager(firebaseSource, firebasePath)

    @Singleton
    @Provides
    fun provideFirebaseProfileManager(
        firebaseSource: FirebaseSource,
        firebasePath: FirebasePath
    ): FirebaseProfileDetailManager = FirebaseProfileDetailManager(firebaseSource, firebasePath)

    @Singleton
    @Provides
    fun provideFirebaseSearchManager(firebaseSource: FirebaseSource): FirebaseSearchManager =
        FirebaseSearchManager(firebaseSource)

    @Singleton
    @Provides
    fun provideFirebaseRepository(
        firebaseAuthManager: FirebaseAuthManager,
        firebaseFeedManager: FirebaseFeedManager,
        firebasePostAddManager: FirebasePostAddManager,
        firebaseSearchManager: FirebaseSearchManager,
        firebaseProfileDetailManager: FirebaseProfileDetailManager,
        firebaseProfileEditManager: FirebaseProfileEditManager

    ): FirebaseRepository = FirebaseRepository(
        firebaseAuthManager,
        firebaseFeedManager,
        firebasePostAddManager,
        firebaseSearchManager,
        firebaseProfileDetailManager,
        firebaseProfileEditManager
    )


}