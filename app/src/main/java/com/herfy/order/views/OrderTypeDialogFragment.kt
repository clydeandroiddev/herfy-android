package com.herfy.order.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.herfy.R
import com.herfy.base.BaseDialogFragment
import com.herfy.utils.constants.Text
import com.herfy.databinding.FragmentDialogOrderTypeBinding
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.herfy.utils.helper.*

class OrderTypeDialogFragment: BaseDialogFragment() {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_ORDER_TYPE = "orderType"
    }

    private var _binding: FragmentDialogOrderTypeBinding? = null

    private val binding get() = _binding ?: throw Error("binding should not be null")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // create ContextThemeWrapper from the original Activity Context with the custom theme
        val mainColorString = CacheUtil.getInstance().colorMain.replace("#", "")
        val contextThemeWrapper: Context = ContextThemeWrapper(activity, requireContext().resources.getIdentifier("T_${mainColorString}", "style", requireContext().packageName))

        // clone the inflater using the ContextThemeWrapper
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        _binding = FragmentDialogOrderTypeBinding.inflate(localInflater, container, false)

        dialog?.window?.run {
            isCancelable = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = CacheUtil.getInstance().getTranslations(Text.SELECT_ORDER_TYPE, "SELECT ORDER TYPE")

        val supportedOrderTypes = CacheUtil.getInstance().application.attributes.getOrderTypes()
        supportedOrderTypes.map {
            createOrderType(it)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            setLayout(width, height)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    fun createOrderType(orderType: OrderType) {
        activity?.let {
            val view = LayoutInflater.from(it)
                .inflate(R.layout.dialog_item_order_type, binding.content, false)
            val card = view.findViewById<MaterialCardView>(R.id.card)
            val container = view.findViewById<ConstraintLayout>(R.id.container)
            container.tag = orderType
            container.setOnClickListener {
                card.strokeColor = Color.parseColor(CacheUtil.getInstance().colorMain)
                setFragmentResult(
                    ARGS_KEY_FRAGMENT_RESULT,
                    bundleOf(
                        ARGS_KEY_ORDER_TYPE to (container.tag as OrderType).value
                    )
                )
                findNavController().navigateUp()
            }
            val icon = view.findViewById<ImageView>(R.id.icon)
            val label = view.findViewById<TextView>(R.id.label)
            icon.setColorFilter(Color.parseColor(CacheUtil.getInstance().colorMain))
            if (orderType === OrderType.DELIVERY) {
                icon.setImageResource(R.drawable.ic_order_type_delivery)
                label.text = CacheUtil.getInstance().getTranslations(Text.DELIVERY)
            } else if (orderType === OrderType.CURBSIDE) {
                icon.setImageResource(R.drawable.ic_order_type_curbside)
                label.text = CacheUtil.getInstance()
                    .getTranslations(Text.CURBSIDE_PICKUP_CAPS, "CURBSIDE")
            } else if (orderType === OrderType.PICKUP) {
                icon.setImageResource(R.drawable.ic_order_type_pickup)
                label.text = CacheUtil.getInstance().getTranslations(Text.PICKUP)
            } else if (orderType === OrderType.DINE_IN) {
                icon.setImageResource(R.drawable.ic_order_type_dinein)
                label.text =
                    CacheUtil.getInstance().getTranslations("dine_in_tab_label", "Dine In")
            }
            binding.content.addView(view)
        }
    }

}