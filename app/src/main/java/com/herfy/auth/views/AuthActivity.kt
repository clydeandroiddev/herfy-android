package com.herfy.auth.views

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.utils.helper.CacheUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : BaseActivity() {

    var guest = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        CacheUtil.getInstance().signinUsername = ""

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController

        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_auth)

        val bundle = Bundle()
//        intent.extras?.let {
//            bundle.putBoolean("sign_in", it.getBoolean("sign_in"))
//            bundle.putBoolean("guest", it.getBoolean("guest"))
//            guest = it.getBoolean("guest")
//        }
//        bundle.putBoolean("curbside", false)
//        bundle.putBoolean("checkout", false)
//
//        if (!guest) {
            graph.setStartDestination(R.id.nav_login_fragment)
//        } else {
//            graph.setStartDestination(R.id.nav_guest)
//        }

        navController.setGraph(graph, bundle)

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

}