package com.herfy.common.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.Window
import com.herfy.common.databinding.CommonDialogWithButtonsBinding

class CommonCustomDialog(
    context : Context,
    val title : String,
    val message : String,
    val icon : Int) : Dialog(context) {

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
        }
    }

    fun setButtonA(label : String, listener : OnClickListener){
        binding.apply {
            if(label.isNotEmpty()){
                buttonA.apply {
                    visibility = View.VISIBLE
                    spaceButtonA.visibility = View.VISIBLE
                    buttonA.setOnClickListener(listener)
                }
            }else {
                buttonA.visibility = View.GONE
                spaceButtonB.visibility = View.GONE
            }
        }
    }

    fun setButtonB(label : String, listener: OnClickListener){
        binding.apply {
            if(label.isNotEmpty()){
                buttonB.apply {
                    visibility = View.VISIBLE
                    spaceButtonB.visibility = View.VISIBLE
                    buttonB.setOnClickListener(listener)
                }
            }else {
                buttonB.visibility = View.GONE
                spaceButtonB.visibility = View.GONE
            }
        }
    }

}