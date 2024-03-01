package com.herfy.common.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.herfy.common.databinding.CommonDialogWithButtonsBinding

class CommonCustomDialog(
    context : Context,
    val title : String,
    val message : String,
    val icon : Int,
    val buttonALabel : String = "",
    val buttonBLabel : String = "") : Dialog(context) {

    private lateinit var binding : CommonDialogWithButtonsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = CommonDialogWithButtonsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun initViews() {
        binding.apply {
            tvTitle.text = title
            tvMessage.text = message
            ivIcon.setImageResource(icon)
            setButtonA(buttonALabel)
            setButtonB(buttonBLabel)

        }
    }

    fun setButtonA(label : String){
        binding.apply {
            if(label.isNotEmpty()){
                buttonA.apply {
                    visibility = View.VISIBLE
                    spaceButtonA.visibility = View.VISIBLE
                    buttonA.apply {
                        text = label
                    }
                }
            }else {
                buttonA.visibility = View.GONE
                spaceButtonB.visibility = View.GONE
            }
        }
    }

    fun setButtonB(label: String){
        binding.apply {
            if(label.isNotEmpty()){
                buttonB.apply {
                    visibility = View.VISIBLE
                    spaceButtonB.visibility = View.VISIBLE
                    buttonB.apply {
                        text = label
                    }
                }
            }else {
                buttonB.visibility = View.GONE
                spaceButtonB.visibility = View.GONE
            }
        }
    }

    fun setButtonAListener(listener: OnClickListener?){
        binding.buttonA.setOnClickListener(listener)
    }
    fun setButtonBListener(listener: OnClickListener?){
        binding.buttonB.setOnClickListener(listener)
    }

    override fun onStart() {
        super.onStart()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.WRAP_CONTENT
        window?.setLayout(width, height)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val typedValue = TypedValue()
            window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window?.statusBarColor = typedValue.data
        }
    }
}