/*
 * Copyright (C) 2021 Image Picker
 * Author: Nguyen Hoang Lam <hoanglamvn90@gmail.com>
 */

package com.nguyenhoanglam.imagepicker.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.addisonelliott.segmentedbutton.SegmentedButton
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup
import com.google.android.material.button.MaterialButton
import com.nguyenhoanglam.imagepicker.R
import com.nguyenhoanglam.imagepicker.model.ImagePickerConfig

class ImagePickerToolbar : RelativeLayout {

    private lateinit var titleText: TextView
    lateinit var backImage: MaterialButton
    private lateinit var doneText: TextView
    private lateinit var cameraImage: AppCompatImageView
	private lateinit var switchPhotos: SegmentedButtonGroup
	lateinit var switchBtnPhotos: SegmentedButton
	lateinit var switchBtnAlbums: SegmentedButton

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.imagepicker_toolbar, this)
        if (isInEditMode) {
            return
        }
        titleText = findViewById(R.id.text_toolbar_title)
        backImage = findViewById(R.id.image_toolbar_back)
        doneText = findViewById(R.id.text_toolbar_done)
        backImage = findViewById(R.id.image_toolbar_back)
        cameraImage = findViewById(R.id.image_toolbar_camera)
        switchPhotos = findViewById(R.id.switch_photos)
        switchBtnPhotos = findViewById(R.id.switch_btn_photos);
        switchBtnAlbums = findViewById(R.id.switch_btn_albums);
    }

    fun config(config: ImagePickerConfig) {
        setBackgroundColor(Color.parseColor(config.toolbarColor))
        titleText.text = if (config.isFolderMode) config.folderTitle else config.imageTitle
        titleText.setTextColor(Color.parseColor("#000000"))
        backImage.setTextColor(Color.parseColor(config.primaryColor))
        backImage.iconTint = ColorStateList.valueOf(Color.parseColor(config.primaryColor))
        doneText.text = config.doneTitle
        doneText.setTextColor(Color.parseColor(config.toolbarTextColor))
        doneText.visibility = if (config.isAlwaysShowDoneButton) View.VISIBLE else View.GONE
        cameraImage.visibility = if (config.isShowCamera) View.VISIBLE else View.GONE
        cameraImage.setColorFilter(Color.parseColor(config.primaryColor))
        switchPhotos.selectedBackground = ColorDrawable(Color.parseColor(config.primaryColor))
    }

    fun setTitle(title: String?) {
        titleText.text = title
    }

    fun showDoneButton(isShow: Boolean) {
        doneText.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    fun setOnBackClickListener(clickListener: OnClickListener) {
        backImage.setOnClickListener(clickListener)
    }

    fun setOnCameraClickListener(clickListener: OnClickListener) {
        cameraImage.setOnClickListener(clickListener)
    }

    fun setOnDoneClickListener(clickListener: OnClickListener) {
        doneText.setOnClickListener(clickListener)
    }

	fun setOnSwitchPhotosClickListener(clickListener: OnClickListener) {
		switchBtnPhotos.setOnClickListener(clickListener)
	}

	fun setOnSwitchAlbumsClickListener(clickListener: OnClickListener) {
		switchBtnAlbums.setOnClickListener(clickListener)
	}

	fun setSwitchMode(switchMode: Boolean) {
		if (switchMode) {
			titleText.visibility = View.INVISIBLE
			switchPhotos.visibility = View.VISIBLE
		} else {
			titleText.visibility = View.VISIBLE
			switchPhotos.visibility = View.INVISIBLE
		}
	}

	fun isInLanding() : Boolean {
		return switchPhotos.visibility == View.VISIBLE
	}
}