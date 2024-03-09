package com.herfy.common.toolbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.herfy.common.R

class CustomButtonsToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var navButton: ImageButton? = null
    private var rightButton: ImageButton? = null
    private var tvTitle: TextView? = null
    var title: String = ""
        set(value) {
            field = value
            tvTitle?.text = value
        }

    init {
        init(attrs)
    }

    @SuppressLint("ResourceAsColor")
    private fun init(attrs: AttributeSet?) {
        View.inflate(context, R.layout.common_toolbar_with_custom_buttons, this)


        navButton = findViewById(R.id.ib_navigation)
        rightButton = findViewById(R.id.ib_right)
        tvTitle = findViewById(R.id.tv_title)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.CustomButtonsToolbar)
        try {
            ta.getResourceId(
                R.styleable.CustomButtonsToolbar_navButton,
                R.drawable.ic_navigate_back
            ).let {
                navButton?.setImageResource(it)
            }

            ta.getResourceId(
                R.styleable.CustomButtonsToolbar_rightButtonIcon,
                R.drawable.ic_history_circle_white
            ).let {
                rightButton?.setImageResource(it)
            }
            ta.getColor(R.styleable.CustomButtonsToolbar_titleTextColor, Color.WHITE)?.let {
                tvTitle?.setTextColor(it)
            }
        } finally {
            ta.recycle()
        }
    }

    fun setNavButtonAction(listener: OnClickListener) {
        navButton?.setOnClickListener(listener)
    }

    fun setRightButtonAction(listener: OnClickListener) {
        rightButton?.setOnClickListener(listener)
    }

    fun hide(toolbar: Boolean) {
        visibility = if (toolbar) View.GONE else View.VISIBLE
    }


}