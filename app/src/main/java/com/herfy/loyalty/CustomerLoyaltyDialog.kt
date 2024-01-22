package com.herfy.loyalty

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.*
import androidx.fragment.app.FragmentManager
import com.herfy.base.BaseDialogFragment
import com.herfy.constants.Text
import com.herfy.databinding.DialogLoyaltyQrCodeBinding
import com.herfy.util.CacheUtil
import com.herfy.util.QRHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CustomerLoyaltyDialog(var qrData: String) : BaseDialogFragment() {

    private var _onAction: OnAction? = null
    private val onAction get() = _onAction ?: throw Error("onAction should not be null")
    private var _binding: DialogLoyaltyQrCodeBinding? = null

    private val binding get() = _binding ?: throw Error("binding should not be null")

    companion object {
        fun newInstance(qrData: String): CustomerLoyaltyDialog {
            return CustomerLoyaltyDialog(qrData)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        _binding = DialogLoyaltyQrCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPreferences()
    }

    private fun loadPreferences() {
        setupViews()
        setupTranslations()
    }

    private fun setupViews() {
        binding.apply {
            GlobalScope.launch {
                val qrCode = QRHelper(requireContext()).generateQR(qrData)
                showQRCode(qrCode)
            }

            btnClose.setOnClickListener {
                dismiss()
                onAction.closeQR()
            }
        }
    }

    private fun showQRCode(d: Drawable) {
        Handler(Looper.getMainLooper()).post {
            binding.apply {
                d.let {
                    ivQrCode.setImageDrawable(it)
                }
            }
        }
    }

    private fun setupTranslations() {
        val cacheUtil = CacheUtil.getInstance()
        binding.apply {
            btnClose.text = cacheUtil.getTranslations(Text.CLOSE, "CLOSE").uppercase()
            titleLabel.text = cacheUtil.getTranslations(Text.LOYALTY_QR, "Loyalty QR Code")
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

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            Log.d("ABSDIALOGFRAG", "Exception", e)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _onAction = when {
            context is OnAction -> context
            activity is OnAction -> activity as OnAction
            else -> null
        }
    }

    override fun onDestroy() {
        _binding = null
        _onAction = null
        super.onDestroy()
    }

    interface OnAction {
        fun closeQR()
    }
}