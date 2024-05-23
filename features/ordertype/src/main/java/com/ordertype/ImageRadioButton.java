package com.ordertype;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatRadioButton;

import com.herfy.ordertype.R;

public class ImageRadioButton extends AppCompatRadioButton {

    private ImageSpan imageSpan;
    private Drawable drawable;

    public ImageRadioButton(Context context) {
        super(context);
        init(context, null, -1);
    }

    public ImageRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, -1);
    }

    public ImageRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    public ImageRadioButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context);
        init(context, attrs, defStyleAttr);
    }

    private final void init(Context context, AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ImageRadioButton, defStyleAttr, 0);
            drawable = typedArray.getDrawable(R.styleable.ImageRadioButton_radioTextImage);
            typedArray.recycle();
        }

        if (drawable != null) {
            imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            setText(getText());
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        Spannable newText = new SpannableString("  " + text);
        newText.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        super.setText(newText, type);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (drawable != null) {
            drawable.jumpToCurrentState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (drawable != null) {
            drawable.setState(getDrawableState());

        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        if (drawable != null) {
            return super.verifyDrawable(who) || who == drawable;
        }
        return super.verifyDrawable(who);
    }
}