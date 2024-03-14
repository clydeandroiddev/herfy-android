package com.herfy.nutritiondetails

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.common.base.BaseBottomSheetDialog
import com.herfy.nutritiondetails.databinding.DialogNutritionInfoBottomSheetBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.getParcelableValue
import com.herfy.utils.extensions.loadImage
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.viewLifecycleScope

class NutritionInfoFragmentBottomSheet: BottomSheetDialogFragment() {


    private lateinit var binding: DialogNutritionInfoBottomSheetBinding
    companion object {
        const val TAG = "com.herfy.nutritiondetails.NutritionInfoFragmentBottomSheet"
        const val NAV_ARGS_INFO_KEY = "NAV_ARGS_INFO_KEY"

        fun newInstance(bundle: Bundle): NutritionInfoFragmentBottomSheet {
            val fragment = NutritionInfoFragmentBottomSheet()
            fragment.arguments = bundle
            return fragment
        }
    }


    private val cacheUtilHelper by lazy {
        CacheUtilHelper(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogNutritionInfoBottomSheetBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDialog()
        setupViews()
    }
    private fun setupDialog() {
        val bottomSheet = (requireView().parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        bottomSheet.elevation = 0f
    }

    private fun setupViews() {
        binding.apply {
           icClose.ninjaTap(viewLifecycleScope) {
                dismiss()
            }

            val nutritionInfoParams =
                requireArguments().getParcelableValue<NutritionInfoParams>(NAV_ARGS_INFO_KEY)
            nutritionInfoParams?.let { params ->
                with(binding) {
                    txtNutritionInfo.text = params.menuName
                    loadImage(nutritionImage, params.nutritionImageUrl)
                    storyLayout.setVisible(params.nutritionStory.isNotEmpty())
                    txtStoryDescription.text = params.nutritionStory
                    cacheUtilHelper.let { cacheHelper ->
                        binding.txtNutritionLabel.text =
                            cacheHelper.getTranslations(Text.NUTRITION_INFO, "Nutrition info")
                        txtStory.text = cacheHelper.getTranslations(Text.STORY, "Story")
                    }
                }
            }
        }
    }

}
