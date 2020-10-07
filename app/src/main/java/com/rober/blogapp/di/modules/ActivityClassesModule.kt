package com.rober.blogapp.di.modules

import android.app.Application
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class)
object ActivityClassesModule {

    @ActivityScoped
    @Provides
    fun provideSharedPreferences(application: Application): SharedPreferences = application.applicationContext.getSharedPreferences("AppSettingsPrefs", 0)

    @ActivityScoped
    @Provides
    fun provideSharedPreferencesEdit(sharedPreferences: SharedPreferences): SharedPreferences.Editor = sharedPreferences.edit()
}