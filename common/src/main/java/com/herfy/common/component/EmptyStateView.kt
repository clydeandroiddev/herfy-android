package com.herfy.common.component

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.textview.MaterialTextView
import com.herfy.common.R

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attributeSet, defStyle) {

    private lateinit var imageView: ImageView
    private lateinit var txtTitle: MaterialTextView
    private var image: Drawable? = null
    private var title: String? = null

    init {
        val attrs = context.obtainStyledAttributes(attributeSet, R.styleable.EmptyStateView)
        try {
            image = attrs.getDrawable(R.styleable.EmptyStateView_emptyStateDrawable)
            title = attrs.getString(R.styleable.EmptyStateView_emptyStateTitle)
        } finally {
            attrs.recycle()
            initViews()
        }
    }

    private fun initViews() {
        View.inflate(context, R.layout.common_view_empty_state, this)
        imageView = findViewById(R.id.imageEmpty)
        txtTitle = findViewById(R.id.txtEmptyTitle)
    }

    fun setTitle(text: String) {
        title = text
        txtTitle.isVisible = text.isNotEmpty()
        txtTitle.text = text
    }

    fun setImage(drawable: Drawable?) {
        this.image = drawable
        imageView.isVisible = drawable != null
        drawable?.let { imageView.setImageDrawable(it) }
    }
}