package com.rober.blogapp.di.modules

import com.rober.blogapp.data.network.firebase.FirebaseAuthManager
import com.rober.blogapp.data.network.firebase.FirebaseSource
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.data.room.dao.BlogDao
import com.rober.blogapp.data.room.dao.UserDao
import com.rober.blogapp.data.room.repository.RoomRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideRoomRepository(userDao: UserDao, blogDao: BlogDao): RoomRepository = RoomRepository(userDao, blogDao)

    @Singleton
    @Provides
    fun provideFirebaseSource(): FirebaseSource = FirebaseSource()

    @Singleton
    @Provides
    fun provideFirebaseAuthManager(firebaseSource: FirebaseSource): FirebaseAuthManager = FirebaseAuthManager(firebaseSource)

    @Singleton
    @Provides
    fun provideFirebaseRepository(firebaseAuthManager: FirebaseAuthManager): FirebaseRepository = FirebaseRepository(firebaseAuthManager)


}