package com.herfy.common.base

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.herfy.common.R
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
    protected open fun shouldShouldInputMode() = false

    protected open fun peekHeight(): Int = BottomSheetBehavior.PEEK_HEIGHT_AUTO

    open fun onStateExpanded() {}

    open fun onStateCollapsed() {}

    open fun onStateHidden() {}

    lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

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
            requireContext(),
            R.style.BottomSheetDialogStyle
        )
        val dialogView =
            View.inflate(
                requireContext(),
                getLayoutId(),
                null
            )
        if (shouldShouldInputMode()) {
            dialog?.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
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
        } else {
            frameLayout.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            bottomSheetBehavior.isFitToContents = false
        }

        bottomSheet.setOnShowListener {
            if (shouldExpandOnOpen()) {
                bottomSheetBehavior?.skipCollapsed = true
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

    inner class EdgeToEdgeBottomSheetDialog(context: Context,theme:Int) :
        BottomSheetDialog(context,theme) {

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            window?.let { window ->
                if (shouldShouldInputMode()) {
                    window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    )
                }
            }


            val container = findViewById<View>(com.google.android.material.R.id.container)

            val controller = WindowCompat.getInsetsController(window!!, container!!)

            controller.isAppearanceLightNavigationBars = true

            container.apply {
                fitsSystemWindows = false
                applyInsetter {
                    containerInsets()
//                }
                }
                findViewById<View>(com.google.android.material.R.id.coordinator)?.fitsSystemWindows =
                    false
            }
        }
    }

    private var loadingDialog: AlertDialog? = null
    open fun showLoadingDialog() {
        try {
            dismissLoadingDialog()
            context?.let {
                val builder = AlertDialog.Builder(it)
                builder.setCancelable(false)
                builder.setView(layoutInflater.inflate(R.layout.loading, null))
                loadingDialog = builder.create()
                loadingDialog?.apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    show()
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    open fun dismissLoadingDialog() {
        try {
           loadingDialog?.let { dialog ->
               if(dialog.isShowing){
                   dialog.dismiss()
               }
           }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}


