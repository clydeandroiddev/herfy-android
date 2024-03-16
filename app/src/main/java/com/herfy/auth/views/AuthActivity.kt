package com.herfy.auth.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        setupBroadcastReceiver()
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(actReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val actReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun setupBroadcastReceiver() {
        try {
            registerReceiver(
                actReceiver, IntentFilter(getString(com.herfy.authentication.R.string.exit_to_home))
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

}