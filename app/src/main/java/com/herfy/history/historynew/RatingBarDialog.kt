package com.herfy.history.historynew

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.common.formatOrderTime
import com.herfy.common.todayOrYesterday
import com.herfy.history.databinding.DialogRatingBarBinding
import com.herfy.history.historynew.adapter.RatingBarAdapter
import com.herfy.ordertype.R
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.models.objects.Rating

class RatingBarDialog : BottomSheetDialogFragment() {

    private var onSubmit: OnRatingListener? = null

    private var maxListHeight: Int? = null

    private var locationName: String = ""

    private var time: String = ""

    private var orderType: String = ""

    private var ratings: List<Rating> = listOf()

    private lateinit var binding: DialogRatingBarBinding

    private lateinit var ratingBarAdapter: RatingBarAdapter

    interface OnRatingListener {
        fun orderRatings(ratings: List<Rating>, feedbackOther: String)
        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogRatingBarBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        dialog?.window?.decorView?.layoutDirection = if (LocaleUtil.getInstance().isArabic) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    private fun setupDialog() {
        val bottomSheet = (requireView().parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        bottomSheet.elevation = 0f
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupDialog()
        setupViews()
    }

    private fun setupList() {
        maxListHeight?.let {
            val layoutParams = binding.ratingsList.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.matchConstraintMaxHeight = it
            binding.ratingsList.layoutParams = layoutParams
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        binding.apply {
            ratingBarAdapter = RatingBarAdapter(ratings)
            ratingsList.adapter = ratingBarAdapter
            otherFeedbackEdit.hint = CacheUtil.getInstance().getTranslations(
                Text.OTHER_FEEDBACK_PLACEHOLDER,
                "e.g: Loved the food and service!"
            )
            ratingName.text = CacheUtil.getInstance()
                .getTranslations(Text.DO_HAVE_OTHER_FEEDBACK, "Do you have any other feedback?")
            soloType.text = CacheUtil.getInstance().getTranslations(Text.FEEDBACK, "Feedback")
            back.setOnClickListener {
                onSubmit?.exit()
            }
            orderTypeLabel.text = orderType
            orderAddress.text = "- $locationName"
            todayLabel.text = time.todayOrYesterday()
            if (todayLabel.text.toString().isNotEmpty()) {
                timeLabel.text = time.formatOrderTime().replace(todayLabel.text.toString(), "")
            } else {
                timeLabel.text = time.formatOrderTime()
            }
            howWasYourOrderLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.HOW_WAS_YOUR_ORDER, "How was your order?")
            submitButton.text = CacheUtil.getInstance().getTranslations(Text.SUBMIT, "Submit")
            submitButton.setOnClickListener {
                onSubmit?.orderRatings(ratingBarAdapter.getRatings(), otherFeedbackEdit.text.toString())
            }
        }
    }

    companion object {
        fun newInstance(
            time: String,
            orderType: String,
            locationName: String,
            maxListHeight: Int,
            ratings: List<Rating>,
            listener: OnRatingListener
        ): RatingBarDialog {
            val ratingBarDialogDialogFragment = RatingBarDialog()
            ratingBarDialogDialogFragment.onSubmit = listener
            ratingBarDialogDialogFragment.ratings = ratings
            ratingBarDialogDialogFragment.maxListHeight = maxListHeight
            ratingBarDialogDialogFragment.time = time
            ratingBarDialogDialogFragment.orderType = orderType
            ratingBarDialogDialogFragment.locationName = locationName
            return ratingBarDialogDialogFragment
        }
    }
}
