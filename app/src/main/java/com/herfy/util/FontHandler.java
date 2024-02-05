package com.herfy.util;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import com.herfy.R;

public class FontHandler {
    private Typeface lightFont;
    private Typeface regularFont;
    private Typeface mediumFont;
    private Typeface semiboldFont;
    private Typeface extraboldFont;
    private static FontHandler instance;

    private FontHandler(Context context) {
        /*lightFont = ResourcesCompat.getFont(context, R.font.poppins_light);
        regularFont = ResourcesCompat.getFont(context, R.font.poppins_regular);
        mediumFont = ResourcesCompat.getFont(context, R.font.poppins_medium);
        semiboldFont = ResourcesCompat.getFont(context, R.font.poppins_semibold);
        extraboldFont = ResourcesCompat.getFont(context, R.font.poppins_extrabold);*/
        lightFont = ResourcesCompat.getFont(context, R.font.cairo_light);
        regularFont = ResourcesCompat.getFont(context, R.font.cairo_regular);
        mediumFont = ResourcesCompat.getFont(context, R.font.cairo_bold);
        semiboldFont = ResourcesCompat.getFont(context, R.font.cairo_semibold);
        extraboldFont = ResourcesCompat.getFont(context, R.font.cairo_black);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new FontHandler(context);
        }
    }

    public static FontHandler getInstance() {
        return instance;
    }

    public static Typeface getLightTypeface() {
        //return LocaleUtil.getInstance().isEnglish() ? getInstance().lightFont : getInstance().arLightFont;
        return getInstance().lightFont;
    }

    public static Typeface getRegularTypeface() {
        //return LocaleUtil.getInstance().isEnglish() ? getInstance().regularFont : getInstance().arMediumFont;
        return getInstance().regularFont;
    }

    public static Typeface getMediumTypeface() {
        //return LocaleUtil.getInstance().isEnglish() ? getInstance().mediumFont : getInstance().arMediumFont;
        return getInstance().mediumFont;
    }

    public static Typeface getSemiboldTypeface() {
        //return LocaleUtil.getInstance().isEnglish() ? getInstance().semiboldFont : getInstance().arBoldFont;
        return getInstance().semiboldFont;
    }

    public static Typeface getExtraboldTypeface() {
        //return LocaleUtil.getInstance().isEnglish() ? getInstance().extraboldFont : getInstance().arBoldFont;
        return getInstance().extraboldFont;
    }

}
