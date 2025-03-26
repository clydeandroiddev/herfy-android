package com.herfy.feedback

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.card.MaterialCardView
import com.herfy.R
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil

class PickerDialog : DialogFragment() {
    private var onAction: OnAction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view: View = inflater.inflate(R.layout.dialog_photo_selection, container, false)
        val cameraLabel = view.findViewById<TextView>(R.id.tvCameraLabel)
        val close = view.findViewById<ImageButton>(R.id.close)
        val cameraContainer = view.findViewById<MaterialCardView>(R.id.camera_mc)
        val galleryContainer = view.findViewById<MaterialCardView>(R.id.gallery_mc)
        val galleryLabel = view.findViewById<TextView>(R.id.tvGalleryLabel)
        val title = view.findViewById<TextView>(R.id.title)
        title.text = CacheUtil.getInstance()
            .getTranslations(Text.CHOOSE_LABEL, "Choose")
        cameraLabel.text =
            CacheUtil.getInstance().getTranslations(Text.CAMERA, "Camera")
        galleryLabel.text =
            CacheUtil.getInstance().getTranslations(Text.GALLERY, "Gallery")
        close.setOnClickListener { onAction!!.onClose() }
        cameraContainer.setOnClickListener { onAction!!.onCamera() }
        galleryContainer.setOnClickListener { onAction!!.onGallery() }
        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onAction!!.onClose()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
            val typedValue = TypedValue()
            dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog.window!!.statusBarColor = typedValue.data
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        onAction!!.onClose()
    }

    interface OnAction {
        fun onClose()

        fun onCamera()

        fun onGallery()
    }

    companion object {
        fun newInstance(onAction: OnAction?): PickerDialog {
            val fragment = PickerDialog()
            fragment.onAction = onAction
            return fragment
        }
    }
}

