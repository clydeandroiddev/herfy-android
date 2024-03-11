package com.herfy.utils.base.mvvm

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import com.herfy.utils.R


abstract class BaseDialogFragmentV2<B : ViewDataBinding> : DialogFragment() {

    private var _binding: B? = null
    val binding: B get() = _binding ?: throw NullPointerException("Binding not yet set nor created")
    open fun attachToParent(): Boolean = false

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DataBindingUtil.inflate(
            inflater,
            getLayoutId(),
            container,
            attachToParent()
        )
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(binding)
        setupListeners()
        initData()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            val width = if (shouldWidthMatchParent()) {
                ConstraintLayout.LayoutParams.MATCH_PARENT
            } else {
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            }

            val layoutParams = attributes
            val height = if (shouldHeightMatchParent()) {
                ConstraintLayout.LayoutParams.MATCH_PARENT
            } else {
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            }

            // Adjust width by deducting horizontal margin
            if (shouldDialogHaveRedius()) {
                layoutParams.width -= 16.dpToPx() // Deduct horizontal margin
            }

            setLayout(width, height)

            if (shouldBackgroundTransparent()) {
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            if (shouldDialogHaveRedius()) {
                setBackgroundDrawableResource(R.drawable.bg_dialog_insets)
                layoutParams.horizontalMargin = 16.dpToPx().toFloat()
            }

            // Apply the modified layout params
            attributes = layoutParams
        }

        dialog?.setCancelable(shouldCancelable())
    }
    @LayoutRes
    abstract fun getLayoutId(): Int

    protected open fun setupViews(view: B) = Unit

    protected open fun setupListeners() = Unit

    protected open fun initData() = Unit

    protected open fun shouldWidthMatchParent(): Boolean {
        return true
    }

    protected open fun shouldHeightMatchParent(): Boolean = false

    protected open fun shouldBackgroundTransparent(): Boolean {
        return true
    }

    protected open fun shouldCancelable(): Boolean {
        return false
    }

    protected open fun shouldDialogHaveRedius()  = false


    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}