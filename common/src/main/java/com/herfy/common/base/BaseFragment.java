package com.herfy.common.base;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;


import com.herfy.common.utils.LocaleUtil;
import com.herfy.common.utils.ViewUtil;

import java.util.Locale;
import java.util.Objects;

public class BaseFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleUtil.getInstance().setLocale(getActivity());

        Locale locale = LocaleUtil.getInstance().createDefaultLocale();

        Locale.setDefault(locale);
        final Configuration cfg = new Configuration();
        cfg.locale = locale;
        getActivity().getResources().updateConfiguration(cfg, null);
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

    protected void showKeyboard(AppCompatEditText text) {
        ((InputMethodManager) Objects.requireNonNull(requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE))).showSoftInput(text, InputMethodManager.SHOW_FORCED);
    }

    protected void closeKeyboard(AppCompatEditText text) {
        ((InputMethodManager) Objects.requireNonNull(requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE))).hideSoftInputFromWindow(text.getWindowToken(), 0);
    }

    public void update() {

    }

}
