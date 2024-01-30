package com.herfy.base;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ViewUtil;

import java.util.Locale;

public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null) {

            LocaleUtil.getInstance().setLocale(getActivity());
            Locale locale = LocaleUtil.getInstance().createDefaultLocale();
            Locale.setDefault(locale);
            final Configuration cfg = new Configuration();
            cfg.locale = locale;
            getActivity().getResources().updateConfiguration(cfg, null);

        }

    }

    @Nullable
    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {

        Animation anim = enter ? AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in) : AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(anim);
        return animationSet;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewUtil.overrideFonts(view);
    }

}
