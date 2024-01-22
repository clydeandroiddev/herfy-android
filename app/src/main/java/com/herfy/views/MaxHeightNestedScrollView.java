package com.herfy.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.core.widget.NestedScrollView;

import com.herfy.R;

public class MaxHeightNestedScrollView extends NestedScrollView {
    private int maxHeight;

    public MaxHeightNestedScrollView(Context context) {
        this(context, null);
    }

    public MaxHeightNestedScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxHeightNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray styledAttrs =
                context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView);
        try {
            maxHeight = styledAttrs.getDimensionPixelSize(R.styleable.MaxHeightScrollView_mhs_maxHeight, 0);
        } finally {
            styledAttrs.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeight > 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}