package com.herfy.utils.helper;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;


import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.herfy.utils.R;

import java.text.Normalizer;
import java.util.Locale;

import kotlin.Suppress;

public class ViewUtil {

    public static int dpToPx(Context context, int dp) {
        return dp * (context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static int pxToDp(Context context, int px) {
        return px / (context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

  /*  public static int getImageWidthHalfDevice(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return ((displayMetrics.widthPixels - context.getResources().getDimensionPixelSize(R.dimen.spacing_88)) / 2);
    }
*/
   /* public static int getSideWidth(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        try {
            if(context != null) {
                context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((displayMetrics.widthPixels - context.getResources().getDimensionPixelSize(R.dimen.pixel_40)) / 3);
    }*/


    public static int getA3rdWidth(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels - (displayMetrics.widthPixels / 3);
    }

   /* public static int getScreenWidth(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        try {
            if(context != null) {
                context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return displayMetrics.widthPixels;
    }*/

    public static int getScreenWidth(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            WindowMetrics metrics = activity.getSystemService(WindowManager.class).getCurrentWindowMetrics();
            return metrics.getBounds().width();
        }else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }

    public static int getScreenWidthHalf(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        try {
            if(context != null) {
                context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return displayMetrics.widthPixels / 2;
    }

    /*public static int getScreenHeight(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        try {
            if(context != null) {
                context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return displayMetrics.heightPixels;
    }*/

    public static int getScreenHeight(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            WindowMetrics metrics = activity.getSystemService(WindowManager.class).getCurrentWindowMetrics();
            return metrics.getBounds().height();
        }else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
           return displayMetrics.heightPixels;
        }
    }

   /* public static int getColorMain(Context context) {

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return typedValue.data;

    }*//*

    public static int getColorAccent(Context context){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
        return typedValue.data;
    }*/

    public static ColorDrawable getColorDrawable(String colorString) {
        int c = Color.parseColor(colorString);
        ColorDrawable cd = new ColorDrawable();
        cd.setColor(c);
        return cd;
    }

  /*  public static int getColorDark(Context context) {

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        return typedValue.data;

    }*/

    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        if (darkness < 0.5) {
            return false;
        } else {
            return true;
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static BitmapDescriptor vectorDrawableToBitmapDescriptor(Context context, @DrawableRes int id) {
        try {
            Drawable vectorDrawable = ContextCompat.getDrawable(context, id);
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap vectorDrawableToBitmap(Context context, @DrawableRes int id) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, id);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }


    /*public static Bitmap writeTextOnDrawable(Context context, int drawableId, String text) {

        Bitmap bitmap = null;

        try {

            bitmap = vectorDrawableToBitmap(context, drawableId).copy(Bitmap.Config.ARGB_8888, true);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ViewUtil.getColorMain(context));

            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(dpToPx(context, 12));

            Rect textRect = new Rect();
            paint.getTextBounds(text, 0, text.length(), textRect);

            Canvas canvas = new Canvas(bitmap);

            if (textRect.width() >= (canvas.getWidth() - 10)) {
                paint.setTextSize(dpToPx(context, 10));
            }

            int xPos = (canvas.getWidth() / 2);

            int yPos = (int) ((canvas.getHeight() / 2.4) - ((paint.descent() + paint.ascent()) / 2.4));

            canvas.drawText(text, xPos, yPos, paint);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        return bitmap;
    }*/

    public static Bitmap createDrawableFromView(Context context, View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay()
                .getMetrics(displayMetrics);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels,
                displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
                view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;

    }


    /*public static CharSequence highlightQuery(Context context, String search, String originalText) {

        String normalizedText = Normalizer
                .normalize(originalText, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ENGLISH);

        int start = normalizedText.indexOf(search.toLowerCase(Locale.ENGLISH));
        if (start < 0) {
            return originalText;
        } else {
            Spannable highlighted = new SpannableString(originalText);
            while (start >= 0) {
                int spanStart = Math.min(start, originalText.length());
                int spanEnd = Math.min(start + search.length(),
                        originalText.length());

                highlighted.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary_text)),
                        spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                start = normalizedText.indexOf(search, spanEnd);
            }

            return highlighted;
        }
    }*/

    /*public static void overrideFonts(View view) {
        try {
            if (view instanceof ViewGroup) {
                for (int x = 0; x < ((ViewGroup) view).getChildCount(); x++) {
                    overrideFonts(((ViewGroup) view).getChildAt(x));
                }
            } else if (view instanceof TextView && ((TextView) view).getTypeface() == null) {
                ((TextView) view).setTypeface(FontHandler.getRegularTypeface());
            } else if (view instanceof EditText && ((EditText) view).getTypeface() == null) {
                ((EditText) view).setTypeface(FontHandler.getRegularTypeface());
            } else if (view instanceof TextInputEditText && ((TextInputEditText) view).getTypeface() == null) {
                ((TextInputEditText) view).setTypeface(FontHandler.getRegularTypeface());
            } else if (view instanceof Button && ((Button) view).getTypeface() == null) {
                ((Button) view).setTypeface(FontHandler.getRegularTypeface());
            } else if (view instanceof MaterialButton && ((MaterialButton) view).getTypeface() == null) {
                ((MaterialButton) view).setTypeface(FontHandler.getRegularTypeface());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }*/

    /*public static int getImageWidthHalfDeviceNew(Activity context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        try {
            if(context != null) {
                context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ((displayMetrics.widthPixels - context.getResources().getDimensionPixelSize(R.dimen.spacing_18)) / 2);
    }*/

    public static int getColorMain(Context context) {

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        return typedValue.data;

    }

}
