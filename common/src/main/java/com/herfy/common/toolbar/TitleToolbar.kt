package com.herfy.common.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.herfy.common.R

class TitleToolbar  @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var navButton: ImageButton? = null
    private var rightButton: MaterialButton? = null
    private var tvTitle: TextView? = null

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        View.inflate(context, R.layout.common_toolbar_with_title, this)


        navButton = findViewById(R.id.ib_navigation)
        rightButton = findViewById(R.id.btn_right)
        tvTitle = findViewById(R.id.tv_title)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.TitleToolbar)
        try {
            val drawableSideMenu =
                ta.getResourceId(R.styleable.TitleToolbar_navButton, R.drawable.ic_navigate_back)
            navButton?.setImageResource(drawableSideMenu)
            val colorId = ta.getResourceId(R.styleable.TitleToolbar_rightButtonColor, android.R.attr.colorPrimary)
            val color = AppCompatResources.getColorStateList(context, colorId)
            rightButton?.apply {
                backgroundTintList = color
                text = ta.getString(R.styleable.TitleToolbar_rightButtonLabel)
            }
            tvTitle?.text = ta.getString(R.styleable.TitleToolbar_title)
        } finally {
            ta.recycle()
        }
    }

    fun setTitle(title: String){
        this.tvTitle?.text = title
    }

    fun setNavButtonAction(listener: OnClickListener){
        navButton?.setOnClickListener(listener)
    }

    fun setRightButtonAction(listener: OnClickListener){
        rightButton?.setOnClickListener(listener)
    }

    fun setRightButtonLabel(label : String){
        rightButton?.text = label
    }

    fun hide(toolbar: Boolean) {
        visibility = if (toolbar) View.GONE else View.VISIBLE
    }
}