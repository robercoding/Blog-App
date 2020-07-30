package com.rober.blogapp.di.modules

import android.app.Application
import com.rober.blogapp.data.room.dao.BlogDao
import com.rober.blogapp.data.room.dao.UserDao
import com.rober.blogapp.data.room.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Singleton
    @Provides
    fun provideDatabase(application: Application): AppDatabase = AppDatabase.getInstance(application)

    @Singleton
    @Provides
    fun provideUserDAO(appDatabase: AppDatabase): UserDao = appDatabase.userDao()

    @Singleton
    @Provides
    fun provideBlogDAO(appDatabase: AppDatabase): BlogDao = appDatabase.blogDao()



}