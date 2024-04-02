package com.herfy.auth.views

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.utils.helper.AuthUtil
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
        val graph = inflater.inflate(R.navigation.auth_nav)

        val bundle = Bundle()
        graph.setStartDestination(R.id.nav_login_fragment)
        navController.setGraph(graph, bundle)

    }

    override fun onResume() {
        super.onResume()
        if (AuthUtil.getInstance().isSignedIn) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

}