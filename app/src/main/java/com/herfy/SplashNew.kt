package com.herfy

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.herfy.databinding.ActivitySampleBinding
import com.herfy.databinding.ActivitySplashBinding
import com.herfy.utils.widgets.otp.OTPListener
import com.herfy.utils.widgets.otp.OTPTextView

class SplashNew : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        binding.otp.setLength(6)
        binding.otp.generate()
        binding.otp.otpListener = object : OTPListener {
            override fun onInteractionListener() {}
            override fun onOTPComplete(otp: String) {
                Toast.makeText(this@SplashNew, "The OTP is $otp", Toast.LENGTH_SHORT).show()
            }
        }
        setContentView(binding.root)
    }
}