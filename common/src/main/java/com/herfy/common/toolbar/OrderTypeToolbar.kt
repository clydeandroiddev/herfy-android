package com.herfy.common.toolbar

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.card.MaterialCardView
import com.herfy.common.R
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.CacheUtil

class OrderTypeToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var navButton: ImageButton? = null
    private var rightButton: MaterialCardView? = null
    private var orderTypeButton: ConstraintLayout? = null
    private var tvOrderType : TextView? = null
    private var tvOrderTypeAddress : TextView? = null
    private var ivOrderType : ImageView? = null
    private var rightButtonIcon : ImageView? = null
    private var qtyText : TextView? = null

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        View.inflate(context, R.layout.common_toolbar_order_type, this)


        navButton = findViewById(R.id.ib_navigation)
        rightButton = findViewById(R.id.cv_cart)
        orderTypeButton = findViewById(R.id.cl_order_type)
        tvOrderType = findViewById(R.id.tv_order_type)
        tvOrderTypeAddress = findViewById(R.id.tv_order_type_address)
        ivOrderType = findViewById(R.id.iv_order_type)
        qtyText = findViewById(R.id.tv_cart_qty)
        rightButtonIcon = findViewById(R.id.iv_cart)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.OrderTypeToolbar)
        try {
            val drawableSideMenu =
                ta.getResourceId(R.styleable.OrderTypeToolbar_navButton, R.drawable.ic_navigate_back)
            navButton?.setImageResource(drawableSideMenu)
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

    fun setNavButton(listener : OnClickListener){
        navButton?.setOnClickListener(listener)
    }

    fun setCartButtonAction(listener: OnClickListener){
        rightButton?.setOnClickListener(listener)
    }

    fun setOrderTypeAction(listener: OnClickListener){
        orderTypeButton?.setOnClickListener(listener)
    }

    fun setOrderType(type : String, address : String, icon : Drawable?) {
        tvOrderType?.text = type
        tvOrderTypeAddress?.text = address
        ivOrderType?.apply {
            if (icon != null) {
                setImageDrawable(icon)
                visible()
            } else {
                gone()
            }
        }
    }

    fun hide(toolbar : Boolean){
        visibility = if(toolbar) View.GONE else View.VISIBLE
    }



}