package com.rober.blogapp.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.rober.blogapp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = Navigation.findNavController(this, R.id.container_fragment)
        bottom_navigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener{_, destination, _ ->
            if(destination.id == R.id.loginFragment || destination.id == R.id.registerFragment || destination.id == R.id.postAddFragment){
                displayBottomNavigation(false)
            }else{
                displayBottomNavigation(true)
            }
        }
    }

    fun displayBottomNavigation(display: Boolean){
        if(display) bottom_navigation.visibility = View.VISIBLE else bottom_navigation.visibility
    }
}