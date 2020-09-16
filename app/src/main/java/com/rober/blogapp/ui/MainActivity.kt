package com.rober.blogapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.setupWithNavController
import com.rober.blogapp.R
import com.rober.blogapp.util.Destinations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    @Inject lateinit var destinations: Destinations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        navController = Navigation.findNavController(this, R.id.container_fragment)
        bottom_navigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener{_, destination, _ ->

            if (destination.id in destinations.fragmentsWithoutBottomNavigationList){
                displayBottomNavigation(false)
            }else{
                displayBottomNavigation(true)
            }

        }
    }

    fun displayBottomNavigation(display: Boolean){
        if(display){
            bottom_navigation.visibility = View.VISIBLE
            view_top_border_bottom_navigation_view.visibility = View.VISIBLE
        } else{
            bottom_navigation.visibility = View.GONE
            view_top_border_bottom_navigation_view.visibility = View.GONE
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}