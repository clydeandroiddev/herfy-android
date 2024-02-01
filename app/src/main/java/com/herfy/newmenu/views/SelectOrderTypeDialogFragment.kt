package com.herfy.newmenu.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.herfy.R
import com.herfy.base.BaseDialogFragment
import com.herfy.data.constants.Text
import com.herfy.data.constants.Text.SELECT_ORDER_TYPE
import com.herfy.databinding.FragmentDialogSelectOrderTypeBinding
import com.skylinedynamics.solosdk.api.models.objects.OrderType
import com.herfy.util.CacheUtil

class SelectOrderTypeDialogFragment(
    private val onOrderTypeSelected: (orderType: OrderType) -> Unit
) : BaseDialogFragment() {

    private lateinit var binding: FragmentDialogSelectOrderTypeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogSelectOrderTypeBinding.inflate(inflater,container,false)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.title.text = CacheUtil.getInstance().getTranslations(SELECT_ORDER_TYPE,"Select Order Type").lowercase().replaceFirstChar { it.uppercase() }

        val availableMenus = CacheUtil.getInstance().application.attributes.orderTypeMenus
        if (availableMenus.isNotEmpty()) {
            availableMenus.forEachIndexed { index, orderTypeMenus ->
                if (orderTypeMenus.type == "pickup"){
                    binding.firstOrderTypeContainer.visibility = View.VISIBLE
                    binding.firstTypeLabel.text = orderTypeMenus.type?.let { getMenuType(it) }
                    binding.ivFirstOrderType.setImageDrawable(orderTypeMenus.type?.let { getImage(it) }?.let {
                        ContextCompat.getDrawable(requireContext(),
                            it
                        )
                    })
                    binding.firstOrderTypeContainer.setOnClickListener {
                        dismiss()
                        onOrderTypeSelected.invoke(OrderType.fromString(orderTypeMenus.type))
                    }
                }

                if (orderTypeMenus.type == "deliver") {
                    binding.secondOrderTypeContainer.visibility = View.VISIBLE
                    binding.secondTypeLabel.text = getMenuType(orderTypeMenus.type)
                    binding.ivSecondOrderType.setImageDrawable(getImage(orderTypeMenus.type).let {
                        ContextCompat.getDrawable(requireContext(),
                            it
                        )
                    })
                    binding.secondOrderTypeContainer.setOnClickListener {
                        dismiss()
                        onOrderTypeSelected.invoke(OrderType.fromString(orderTypeMenus.type))
                    }
                }


/*              for dynamic view
                if (index == 0) {
                    binding.firstOrderTypeContainer.visibility = View.VISIBLE
                    binding.firstTypeLabel.text = orderTypeMenus.type?.let { getMenuType(it) }
                    binding.ivFirstOrderType.setImageDrawable(orderTypeMenus.type?.let { getImage(it) }?.let {
                        ContextCompat.getDrawable(requireContext(),
                            it
                        )
                    })
                    binding.firstOrderTypeContainer.setOnClickListener {
                        dismiss()
                        onOrderTypeSelected.invoke(OrderType.fromString(orderTypeMenus.type))
                    }
                }
                if (index == 1) {
                    binding.secondOrderTypeContainer.visibility = View.VISIBLE
                    binding.secondTypeLabel.text = getMenuType(orderTypeMenus.type)
                    binding.ivSecondOrderType.setImageDrawable(getImage(orderTypeMenus.type).let {
                        ContextCompat.getDrawable(requireContext(),
                            it
                        )
                    })
                    binding.secondOrderTypeContainer.setOnClickListener {
                        dismiss()
                        onOrderTypeSelected.invoke(OrderType.fromString(orderTypeMenus.type))
                    }
                }
                if (index == 2) {
                    binding.thirdOrderTypeContainer.visibility = View.VISIBLE
                    binding.thirdTypeLabel.text = getMenuType(orderTypeMenus.type)
                    binding.ivThirdOrderType.setImageDrawable(getImage(orderTypeMenus.type).let {
                        ContextCompat.getDrawable(requireContext(),
                            it
                        )
                    })
                    binding.thirdOrderTypeContainer.setOnClickListener {
                        dismiss()
                        onOrderTypeSelected.invoke(OrderType.fromString(orderTypeMenus.type))
                    }
                }
                if (index == 3) {
                    binding.fourthOrderTypeContainer.visibility = View.VISIBLE
                    binding.fourthTypeLabel.text = getMenuType(orderTypeMenus.type)
                    binding.ivFourthOrderType.setImageDrawable(getImage(orderTypeMenus.type).let {
                        ContextCompat.getDrawable(requireContext(),
                            it
                        )
                    })
                    binding.fourthOrderTypeContainer.setOnClickListener {
                        dismiss()
                        onOrderTypeSelected.invoke(OrderType.fromString(orderTypeMenus.type))
                    }
                }*/
            }
        }
    }

    private fun getImage(type: String) : Int {
        return when(type) {
            "pickup" -> R.drawable.ic_pickup_menu
            "deliver" -> R.drawable.ic_delivery_v2
            "dinein" -> R.drawable.ic_dinein
            else -> R.drawable.ic_curbside

        }
    }

    private fun getMenuType(type: String) : String {
        return when(type) {
            "pickup" -> CacheUtil.getInstance().getTranslations(Text.STORE_PICKUP,"STORE PICKUP").uppercase()
            "deliver" -> CacheUtil.getInstance().getTranslations(Text.DELIVERY, "DELIVERY").uppercase()
            "dinein" -> CacheUtil.getInstance().getTranslations(Text.DINE_IN, "DINE IN").uppercase()
            else -> CacheUtil.getInstance().getTranslations(Text.CURBSIDE_PICKUP, "CURBSIDE").uppercase()

        }
    }



    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            val typedValue = TypedValue()
            dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window!!.statusBarColor = typedValue.data
        }
    }
}