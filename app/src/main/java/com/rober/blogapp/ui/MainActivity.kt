package com.rober.blogapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.rober.blogapp.R
import com.rober.blogapp.util.Destinations
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    @Inject
    lateinit var destinations: Destinations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        navController = Navigation.findNavController(this, R.id.container_fragment)
        bottom_navigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            if (destination.id in destinations.fragmentsWithoutBottomNavigationList) {
                displayBottomNavigation(false)
            } else {
                displayBottomNavigation(true)
            }
        }
    }

    fun displayBottomNavigation(display: Boolean) {
        if (display) {
            bottom_navigation.visibility = View.VISIBLE
            view_top_border_bottom_navigation_view.visibility = View.VISIBLE
        } else {
            bottom_navigation.visibility = View.GONE
            view_top_border_bottom_navigation_view.visibility = View.GONE
        }

    }

//    override fun onBackPressed() {
//        Log.i("SeeBack", "Detected Back")
//        when (navController.currentDestination?.id) {
//            destinations.POST_DETAIL_FRAGMENT -> {
//                Log.i("SeeBack", "PreviousBackStrack = ${navController.previousBackStackEntry?.destination?.id.toString()}")
//                if(navController.previousBackStackEntry?.destination?.id == destinations.POST_ADD_FRAGMENT && navController.currentDestination?.id == destinations.POST_DETAIL_FRAGMENT){
//                    Log.i("SeeBack", "True")
////                    navController.popBackStack(R.id.profileDetailFragment, false)
//                    navController.navigate(R.id.profileDetailFragment)
//                    navController.popBackStack(R.id.postAddFragment, true)
//                    Log.i("SeeBack", "Go")
//                }
////                Log.i("SeeBack", "Detected Back and see PostDetail")
//            }
//        }
//        super.onBackPressed()
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}