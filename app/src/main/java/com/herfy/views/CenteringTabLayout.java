package com.herfy.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

public class CenteringTabLayout extends TabLayout {

    public CenteringTabLayout(Context context) {
        super(context);
    }

    public CenteringTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CenteringTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!changed) return;
        int totalTabsWidth = 0;
        for (int i = 0; i < getTabCount(); i++)
            totalTabsWidth += ((ViewGroup) getChildAt(0)).getChildAt(i).getWidth();
        int padding = (getWidth() - totalTabsWidth) / 2;
        if (padding < 0) padding = 0;
        View firstTab = ((ViewGroup) getChildAt(0)).getChildAt(0);
        if (firstTab != null) {
            getChildAt(0).setPaddingRelative(padding, 0, padding, 0);
        }
    }

}
