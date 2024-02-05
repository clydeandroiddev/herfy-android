package com.herfy.country

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.databinding.ActivityCountriesBinding
import com.herfy.utils.helper.CacheUtil

class CountriesActivity : BaseActivity() {

    private lateinit var binding: ActivityCountriesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainToolbar.back.visibility = View.GONE
        binding.mainToolbar.cart.visibility = View.GONE

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            binding.mainToolbar.title.text = CacheUtil.getInstance().getTranslations(destination.label.toString())
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

}