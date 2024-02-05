package com.herfy.common.base

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.common.utils.autoCleared
import dev.chrisbanes.insetter.InsetterDsl
import dev.chrisbanes.insetter.applyInsetter

abstract class BaseBottomSheetDialog<B: ViewDataBinding>: BottomSheetDialogFragment() {
    protected var binding: B by autoCleared(::onBindingCleanup)
        private set

    @CallSuper
    protected open fun onBindingCleanup(binding: B) {
        binding.unbind()
    }

    @LayoutRes
    abstract fun getLayoutId(): Int

    /**
     * @return true if dialog is cancellable via outside interactions
     */
    protected open fun cancellableWithOutsideTouch(): Boolean {
        return true
    }

    protected open fun isFullScreen(): Boolean = false

    protected open fun shouldExpandOnOpen(): Boolean = false

    protected open fun shouldSkipCollapsedState(): Boolean = false

    protected open fun isDismissible(): Boolean = true

    protected open fun isDraggable(): Boolean = true

    protected open fun isTransparent(): Boolean = false

    protected open fun peekHeight(): Int = BottomSheetBehavior.PEEK_HEIGHT_AUTO

    open fun onStateExpanded() {}

    open fun onStateCollapsed() {}

    open fun onStateHidden() {}

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    open fun InsetterDsl.mainContentInset() {
        type(navigationBars = true) {
            margin()
        }
    }

    open fun InsetterDsl.containerInsets() {
        type(statusBars = true) {
            margin()
        }
    }

    @CallSuper
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheet = EdgeToEdgeBottomSheetDialog(
            requireContext()
        )
        val dialogView =
            View.inflate(
                requireContext(),
                getLayoutId(),
                null
            )

        dialogView.applyInsetter {
            mainContentInset()
        }

        val dataBinding = DataBindingUtil.bind<B>(dialogView)
        this.binding = dataBinding!!
        this.binding.lifecycleOwner = this

        bottomSheet.setContentView(dialogView)

        val frameLayout = bottomSheet
            .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)!!

        bottomSheetBehavior = BottomSheetBehavior.from(dialogView.parent as View)
        bottomSheetBehavior.peekHeight = peekHeight()
        bottomSheetBehavior.skipCollapsed = shouldSkipCollapsedState()
        bottomSheetBehavior.isFitToContents = isFullScreen()
        bottomSheetBehavior.isDraggable = isDraggable()

        bottomSheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

                @SuppressLint("SwitchIntDef")
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            onStateExpanded()
                        }

                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            onStateCollapsed()
                        }

                        BottomSheetBehavior.STATE_HIDDEN -> {
                            onStateHidden()
                            if (isDismissible()) {
                                dismiss()
                            }
                        }

                        BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                            if (shouldSkipCollapsedState() && isDismissible()) {
                                dismiss()
                            }
                        }
                    }
                }
            }
        )

        if (isFullScreen()) {
            frameLayout.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

            bottomSheetBehavior.isFitToContents = false
        }else {
            frameLayout.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            bottomSheetBehavior.isFitToContents = false
        }

        bottomSheet.setOnShowListener {
            if (shouldExpandOnOpen()) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        setupViews(binding)

        return bottomSheet
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun getViewLifecycleOwner(): LifecycleOwner = this

    abstract fun setupViews(binding: B)

    inner class EdgeToEdgeBottomSheetDialog(context: Context) :
        BottomSheetDialog(context) {

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()

            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
            }

            val container = findViewById<View>(com.google.android.material.R.id.container)

            val controller = WindowCompat.getInsetsController(window!!, container!!)

            controller.isAppearanceLightNavigationBars = true

            container.apply {
                fitsSystemWindows = false
                applyInsetter {
                    containerInsets()
                }
            }
            findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows =
                false
        }
    }
}
