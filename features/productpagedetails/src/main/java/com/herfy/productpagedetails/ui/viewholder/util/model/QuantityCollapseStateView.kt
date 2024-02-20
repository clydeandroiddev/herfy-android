package com.herfy.productpagedetails.ui.viewholder.util.model

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.VISIBLE
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.getSystemService
import androidx.databinding.DataBindingUtil
import com.herfy.productpagedetails.R
import com.herfy.productpagedetails.databinding.ItemModifierQuantityBinding
import com.herfy.utils.extensions.setVisible
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem
import com.skylinedynamics.solosdk.api.models.objects.ModifierItemAttributes
import kotlin.properties.Delegates

//class QuantityCollapseStateView(context: Context,attributeSet: AttributeSet? =null,
//    defStyle:Int = 0): ConstraintLayout(context,attributeSet,defStyle) {
//
//        private var modifierGroup: ModifierGroup? = null
//        private var modifierItemAttributes: ModifierItemAttributes? =null
//        private val binding: ItemModifierQuantityBinding = DataBindingUtil.inflate(context.getSystemService()!!,
//            R.layout.item_modifier_quantity,
//            this,
//            true
//        )
//    private var clickState by Delegates.observable(ModifierItemState()) { _, _, _ ->
//        // Your observer logic here
//    }
//    fun setInitialState(isShouldEnablePlusButton:Boolean,modifierGroup:ModifierGroup,modifierItem: ModifierItem,
//        modifierGroupPosition:Int,
//        modifierItemPosition:Int
//    ) {
//        clickState = ModifierItemState(modifierGroupPosition = modifierGroupPosition, modifierGroup = modifierGroup,
//            modifierItemPosition = modifierItemPosition,
//            modifierItem =modifierItem)
//    }
//
//    init {
//        var isExpanded = false // Track the expanded state
//
//        binding.quantityUp.setOnClickListener {
//            if (!isExpanded) { // Expand only if not already expanded
//                binding.quantityDownLayout.setVisible(modifierGroup!!.attributes.maximum > 1 &&
//                        modifierItemAttributes?.isSelected == true && modifierItemAttributes?.maximum == 1)
//                expandView(binding.quantityDownLayout, duration = 200, targetWidth = 150)
//                isExpanded = true
//                binding.quantity.setText()
//            } else {
//
//            }
//        }
//    }
//    }
//
//    fun expandView(v: View, duration: Int, targetWidth: Int) {
//        val prevHeight = v.width
//        v.visibility = VISIBLE
//        val valueAnimator = ValueAnimator.ofInt(prevHeight, targetWidth)
//        valueAnimator.addUpdateListener { animation ->
//            val animatedValue = animation.animatedValue as Int
//            val layoutParams = v.layoutParams
//            layoutParams.width = animatedValue
//            v.layoutParams = layoutParams
//        }
//        valueAnimator.interpolator = DecelerateInterpolator()
//        valueAnimator.setDuration(duration.toLong())
//        valueAnimator.start()
//    }
//
//    fun collapseView(v: View, duration: Int, targetHeight: Int) {
//        val prevHeight = v.width
//        val valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight)
//        valueAnimator.interpolator = DecelerateInterpolator()
//        valueAnimator.addUpdateListener { animation ->
//            val animatedValue = animation.animatedValue as Int
//            val layoutParams = v.layoutParams
//            layoutParams.width = animatedValue
//            v.layoutParams = layoutParams
//        }
//        valueAnimator.setDuration(duration.toLong())
//        valueAnimator.start()
//    }
//}
//        data class ModifierItemState(val isAdd:Boolean = false,
//                                     val modifierItemPosition:Int? = null,
//                                     val modifierGroupPosition:Int? = null,
//            val modifierItem: ModifierItem? = null,
//            val modifierGroup: ModifierGroup? = null
//        )
