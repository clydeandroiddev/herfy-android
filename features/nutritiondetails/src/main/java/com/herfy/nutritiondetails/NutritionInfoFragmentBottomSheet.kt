package com.herfy.nutritiondetails

import android.os.Bundle
import com.herfy.common.base.BaseBottomSheetDialog
import com.herfy.nutritiondetails.databinding.DialogNutritionInfoBottomSheetBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.getParcelableValue
import com.herfy.utils.extensions.loadImage
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.viewLifecycleScope

class NutritionInfoFragmentBottomSheet: BaseBottomSheetDialog<DialogNutritionInfoBottomSheetBinding>() {


    companion object {
        const val TAG = "com.herfy.nutritiondetails.NutritionInfoFragmentBottomSheet"
        const val NAV_ARGS_INFO_KEY = "NAV_ARGS_INFO_KEY"

        fun newInstance(bundle: Bundle): NutritionInfoFragmentBottomSheet {
            val fragment = NutritionInfoFragmentBottomSheet()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun isFullScreen(): Boolean = false
    override fun shouldExpandOnOpen(): Boolean = false

    override fun isDraggable(): Boolean = false

    override fun isDismissible(): Boolean = true

    override fun shouldSkipCollapsedState(): Boolean = false

    private val cacheUtilHelper by lazy {
        CacheUtilHelper(requireContext())
    }

    override fun getLayoutId(): Int = R.layout.dialog_nutrition_info_bottom_sheet


    override fun setupViews(binding: DialogNutritionInfoBottomSheetBinding) {
        binding.icClose.ninjaTap(viewLifecycleScope) {
            dismiss()
        }

        val nutritionInfoParams = requireArguments().getParcelableValue<NutritionInfoParams>(NAV_ARGS_INFO_KEY)
        nutritionInfoParams?.let { params ->
            with(binding) {
                txtNutritionInfo.text = params.menuName
                storyLayout.setVisible(params.nutritionStory.isNotEmpty())
                loadImage(nutritionImage,params.nutritionImageUrl)
                txtStoryDescription.text = params.nutritionStory
                cacheUtilHelper.let { cacheHelper ->
                    binding.txtNutritionLabel.text = cacheHelper.getTranslations(Text.NUTRITION_INFO,"Nutrition info")
                    txtStory.text = cacheHelper.getTranslations(Text.STORY, "Story")
                }
            }
        }
    }

}
