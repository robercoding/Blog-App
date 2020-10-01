package com.rober.blogapp.di.modules

import android.app.Application
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
    fun provideFirebaseSource(firebasePath: FirebasePath): FirebaseSource = FirebaseSource(firebasePath)

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
    fun provideFirebaseFeedManager(firebaseSource: FirebaseSource, firebasePath: FirebasePath): FirebaseFeedManager =
        FirebaseFeedManager(firebaseSource, firebasePath)

    @Singleton
    @Provides
    fun provideFirebasePostAddManager(firebaseSource: FirebaseSource): FirebasePostAddManager =
        FirebasePostAddManager(firebaseSource)

    @Singleton
    @Provides
    fun provideFirebasePostDetailManager(
        firebaseSource: FirebaseSource,
        firebasePath: FirebasePath
    ): FirebasePostDetailManager =
        FirebasePostDetailManager(firebaseSource, firebasePath)

    @Singleton
    @Provides
    fun provideFirebaseProfileDetailManager(
        firebaseSource: FirebaseSource,
        firebasePath: FirebasePath
    ): FirebaseProfileDetailManager = FirebaseProfileDetailManager(firebaseSource, firebasePath)

    @Singleton
    @Provides
    fun provideFirebaseSearchManager(firebaseSource: FirebaseSource): FirebaseSearchManager =
        FirebaseSearchManager(firebaseSource)

    @Singleton
    @Provides
    fun provideFirebaseSettingsManager(firebaseSource: FirebaseSource): FirebaseSettingsManager =
        FirebaseSettingsManager(firebaseSource)

    @Singleton
    @Provides
    fun provideFirebaseRepository(
        firebaseSource: FirebaseSource,
        firebaseAuthManager: FirebaseAuthManager,
        firebaseFeedManager: FirebaseFeedManager,
        firebasePostAddManager: FirebasePostAddManager,
        firebasePostDetailManager: FirebasePostDetailManager,
        firebaseSearchManager: FirebaseSearchManager,
        firebaseProfileDetailManager: FirebaseProfileDetailManager,
        firebaseProfileEditManager: FirebaseProfileEditManager,
        firebaseSettingsManager: FirebaseSettingsManager

    ): FirebaseRepository = FirebaseRepository(
        firebaseSource,
        firebaseAuthManager,
        firebaseFeedManager,
        firebasePostAddManager,
        firebasePostDetailManager,
        firebaseSearchManager,
        firebaseProfileDetailManager,
        firebaseProfileEditManager,
        firebaseSettingsManager
    )


}