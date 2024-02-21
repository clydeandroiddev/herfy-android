package com.herfy.order.views.ordertype.addaddress

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.maps.model.LatLng
import com.herfy.common.utils.staticMap
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.DialogAddAddressBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil

class AddAddressDialog : DialogFragment() {

    private var onSubmit: OnSubmit? = null

    private var address: Address? = null

    private lateinit var binding: DialogAddAddressBinding

    interface OnSubmit {
        fun onSuccess()
        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAddAddressBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme_White)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
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
            dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog.window!!.statusBarColor = typedValue.data
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            saveAddressLabel.text =
                CacheUtil.getInstance().getTranslations(Text.SAVE_ADDRESS, "Save Address")
            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.SAVE_AND_CONTINUE, "Save & Continue")
            back.setOnClickListener {
                onSubmit?.exit()
                dismiss()
            }

            address?.let {
                Glide.with(requireActivity()).load(
                    LatLng(
                        it.latitude, it.longitude
                    ).staticMap(requireActivity().resources.getString(com.herfy.R.string.google_maps_api_key))
                ).apply(RequestOptions.centerInsideTransform())
                    .placeholder(com.herfy.R.drawable.imagepicker_image_placeholder)
                    .diskCacheStrategy(
                        DiskCacheStrategy.ALL
                    ).into(staticMap)
            }

        }
    }

    companion object {
        fun newInstance(address: Address?, listener: OnSubmit): AddAddressDialog {
            val addressDialogFragment = AddAddressDialog()
            addressDialogFragment.onSubmit = listener
            addressDialogFragment.address = address
            return addressDialogFragment
        }
    }
}
