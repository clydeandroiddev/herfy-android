package com.herfy.checkout

import android.os.Bundle
import com.herfy.checkout.databinding.BottomSheetAlrhajiBinding
import com.herfy.common.base.BaseBottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
@AndroidEntryPoint
class BottomSheetAlrhaji : BaseBottomSheetDialog<BottomSheetAlrhajiBinding>() {

    val viewModel: AlrhajiOTPViewModel by viewModels
    override fun getLayoutId(): Int = R.layout.bottom_sheet_alrhaji
    override fun isFullScreen(): Boolean = true
    override fun isDraggable(): Boolean = false
    override fun isDismissible(): Boolean = false
    override fun shouldExpandOnOpen(): Boolean = true
    override fun setupViews(binding: BottomSheetAlrhajiBinding) {

    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BottomSheetAlrhaji().apply {
                arguments = Bundle().apply {

                }
            }
    }
}