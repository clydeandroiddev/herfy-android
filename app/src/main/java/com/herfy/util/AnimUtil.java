package com.herfy.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.herfy.R;
import com.herfy.data.constants.Values;

/**
 * Animation Util
 * for proper page transition in arabic
 * and english
 */
public class AnimUtil {

    public static Context context;
    public static int ANIM_DURATION = 1000;

    public AnimUtil(Context context) {
        this.context = context;
    }

    /**
     * 1st anim for going from left to right
     *
     * @return
     */
    public static int inF() {
        if (!LocaleUtil.getInstance().isEnglish() || !LocaleUtil.getInstance().isFrench()) {
            return R.anim.slide_out_right;
        } else {
            return R.anim.slide_in_left;
        }
    }

    /**
     * 2nd anim for going from left to right
     *
     * @return
     */
    public static int inS() {
        if (!LocaleUtil.getInstance().isEnglish() || !LocaleUtil.getInstance().isFrench()) {
            return R.anim.slide_in_right;
        } else {
            return R.anim.slide_out_left;
        }
    }

    /**
     * 1st anim for going from left to right
     *
     * @return
     */
    public static int outF() {
        if (!LocaleUtil.getInstance().isEnglish() || !LocaleUtil.getInstance().isFrench()) {
            return android.R.anim.slide_in_left;
        } else {
            return android.R.anim.slide_out_right;
        }
    }

    /**
     * 2nd anim for going from left to right
     *
     * @return
     */
    public static int outS() {
        if (!LocaleUtil.getInstance().isEnglish() || !LocaleUtil.getInstance().isFrench()) {
            return R.anim.slide_out_left;
        } else {
            return R.anim.slide_in_right;
        }
    }

    /**
     * @param context
     * @param itemView
     * @param imageUrl
     * @param toLoc
     * @param dummyImage
     */
    public static void animateViewToCart(Context context, View itemView, String imageUrl, int[] toLoc, ImageView dummyImage) {
        try {

            int actionBarHeight = 0;
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
            }

            if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
                imageUrl = Values.DEFAULT_IMAGE_URL;
            }

            dummyImage.getLayoutParams().width = itemView.getWidth();
            dummyImage.getLayoutParams().height = itemView.getHeight();

            Glide.with(context).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerCropTransform()).into(dummyImage);
            int[] fromLoc = new int[2];
            itemView.getLocationOnScreen(fromLoc);
            float startX = fromLoc[0];
            float startY = fromLoc[1];

            float destX = toLoc[0];
            float destY = toLoc[1];

            AnimatorSet addToCartAnimation = new AnimatorSet();
            ObjectAnimator y = ObjectAnimator.ofFloat(dummyImage, "translationY", startY, destY - actionBarHeight);
            ObjectAnimator x = ObjectAnimator.ofFloat(dummyImage, "translationX", startX, destX);
            ObjectAnimator sy = ObjectAnimator.ofFloat(dummyImage, "scaleY", 1.0f, 0.1f);
            ObjectAnimator sx = ObjectAnimator.ofFloat(dummyImage, "scaleX", 1.0f, 0.1f);
            addToCartAnimation.playTogether(x, y, sx, sy);
            addToCartAnimation.setDuration(ANIM_DURATION);
            addToCartAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    dummyImage.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    dummyImage.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    dummyImage.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            addToCartAnimation.start();
        } catch (Exception e) {
            dummyImage.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }
}