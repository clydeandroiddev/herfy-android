package com.herfy.common.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.herfy.common.R
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil


class MainToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var sideMenuButton : ImageButton? = null
    private var cartButton : LinearLayout? = null
    private var cartIcon : ImageView? = null
    private var qtyText : TextView? = null
    private var logo : ImageView? = null
    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        View.inflate(context, R.layout.common_toolbar_main, this)


        sideMenuButton = findViewById(R.id.ib_side_menu)
        cartButton = findViewById(R.id.ln_cart)
        cartIcon = findViewById(R.id.iv_cart)
        qtyText = findViewById(R.id.tv_cart_qty)
        logo = findViewById(R.id.iv_logo)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.MainToolbar)
        try {
            val drawableSideMenu = ta.getResourceId(R.styleable.MainToolbar_leftButton, R.drawable.ic_toolbar_side_menu)
            sideMenuButton?.setImageResource(drawableSideMenu)
            val drawableLogo = ta.getResourceId(R.styleable.MainToolbar_logo, R.drawable.herfy_logo_arabic)
            logo?.setImageResource(drawableLogo)
            val drawableCartLogo = ta.getResourceId(R.styleable.MainToolbar_rightButtonIcon, R.drawable.ic_toolbar_cart)
            cartIcon?.setImageResource(drawableCartLogo)
        } finally {
            ta.recycle()
        }
    }

    fun setCart(quantity : Int = CacheUtil.getInstance().cartQuantity) {
        qtyText?.apply {
            if(quantity > 0) {
                text = quantity.toString()
                visible()
            } else {
                gone()
            }
        }
    }

    fun setSideButtonAction(listener : OnClickListener){
        sideMenuButton?.setOnClickListener(listener)
    }

    fun setCartButtonAction(listener: OnClickListener){
        cartButton?.setOnClickListener(listener)
    }

    fun hide(toolbar : Boolean){
        visibility = if(toolbar) View.GONE else View.VISIBLE
    }
}
