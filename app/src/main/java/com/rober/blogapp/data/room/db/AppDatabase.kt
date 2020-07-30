package com.rober.blogapp.data.room.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rober.blogapp.data.room.dao.BlogDao
import com.rober.blogapp.data.room.dao.UserDao
import com.rober.blogapp.entity.Blog
import com.rober.blogapp.entity.User

@Database(entities = arrayOf(User::class, Blog::class), version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){

    abstract fun userDao(): UserDao
    abstract fun blogDao(): BlogDao


    companion object{
        const val DB_NAME = "app_database"

        @Volatile
        private var INSTANCE : AppDatabase? = null

        fun getInstance(context: Context): AppDatabase{
            val tempInstance = INSTANCE

            if(tempInstance!= null){
                return tempInstance
            }else{
                synchronized(this){
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DB_NAME
                    ).build()

                    INSTANCE = instance
                    return instance
                }
            }
        }
    }
}