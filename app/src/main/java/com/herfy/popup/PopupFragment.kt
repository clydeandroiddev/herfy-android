package com.herfy.popup

import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.herfy.base.BaseDialogFragment
import com.herfy.databinding.FragmentDialogPopupBinding
import org.json.JSONException
import com.herfy.utils.helper.CacheUtil
import com.herfy.model.Popup
import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.ViewUtil
import com.kochava.tracker.events.EventType

class PopupFragment : BaseDialogFragment() {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_ACTION_TYPE = "action_type"
        const val ARGS_KEY_ACTION_VALUE = "action_value"
    }

    private var _binding: FragmentDialogPopupBinding? = null

    private val binding get() = _binding ?: throw Error("binding should not be null")

    private val popup: Popup = CacheUtil.getInstance().popup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // create ContextThemeWrapper from the original Activity Context with the custom theme
        val mainColorString = CacheUtil.getInstance().colorMain.replace("#", "")
        val contextThemeWrapper: Context = ContextThemeWrapper(
            activity,
            requireContext().resources.getIdentifier(
                "T_${mainColorString}",
                "style",
                requireContext().packageName
            )
        )

        // clone the inflater using the ContextThemeWrapper
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        _binding = FragmentDialogPopupBinding.inflate(localInflater, container, false)

        dialog?.window?.run {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = Color.TRANSPARENT
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ratio = Values.POPUP_ASPECT_RATIO_WIDTH / Values.POPUP_ASPECT_RATIO_HEIGHT
        val popupWidth = Resources.getSystem().displayMetrics.widthPixels * Values.POPUP_WIDTH_PERCENT
        val popupHeight = popupWidth / ratio
        val colorPrimary =  Color.parseColor(CacheUtil.getInstance().colorMain)

        popup.imageUri?.let {
            context?.let { ctx ->
                Glide.with(ctx)
                    .load(it)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(RequestOptions.centerCropTransform())
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.shimmer.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.shimmer.visibility = View.GONE
                            return false
                        }
                    })
                    .into(binding.image)
            }

        }

        binding.card.layoutParams.apply {
            width = popupWidth.toInt()
            height = popupHeight.toInt()
        }

        binding.btnAction.apply {
            isVisible = !popup.buttonLabel.isNullOrBlank() && !popup.actionType.isNullOrBlank() && !popup.actionValue.isNullOrBlank()
            text = popup.buttonLabel
            backgroundTintList = ColorStateList.valueOf(colorPrimary)
            setOnClickListener {
                dismiss()
                setFragmentResult(
                    ARGS_KEY_FRAGMENT_RESULT,
                    bundleOf(
                        ARGS_KEY_ACTION_TYPE to popup.actionType,
                        ARGS_KEY_ACTION_VALUE to popup.actionValue
                    )
                )
            }
        }

        binding.ibClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            setLayout(width, height)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        try {
            CacheUtil.getInstance().setPopup(null)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        findNavController().navigateUp()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}