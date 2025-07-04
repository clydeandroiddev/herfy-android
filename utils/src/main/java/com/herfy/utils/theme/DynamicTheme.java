package com.herfy.utils.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.herfy.utils.R;

public class DynamicTheme {

    private static final String NAME = "DynamicTheme";
    private static final String KEY = "color";
    public static final String DEFAULT_PRIMARY_COLOR = "0064AC";

    @ColorInt
    public int color;

    public DynamicTheme(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        String stringColor = sharedPreferences.getString(KEY, DEFAULT_PRIMARY_COLOR);
        color = Color.parseColor("#" + stringColor);

        if (isLightActionBar()) context.setTheme(R.style.AppTheme);
        context.setTheme(context.getResources().getIdentifier("T_" + stringColor, "style", context.getPackageName()));
    }

    public static void setNewThemeColor(Activity activity, int red, int green, int blue) {
        int colorStep = 15;
        red = Math.round(red / colorStep) * colorStep;
        green = Math.round(green / colorStep) * colorStep;
        blue = Math.round(blue / colorStep) * colorStep;

        String stringColor = Integer.toHexString(Color.rgb(red, green, blue)).substring(2);
        SharedPreferences.Editor editor = activity.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY, stringColor);
        editor.apply();

    }

    public static String getStringColor(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        String stringColor = sharedPreferences.getString(KEY, DEFAULT_PRIMARY_COLOR);
        return "#" + stringColor;
    }

    private boolean isLightActionBar() {
        int rgb = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
        return rgb > 210;
    }
}