package com.herfy.base;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.main.MainActivity;
import com.herfy.util.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.ViewUtil;

import java.util.Locale;

public class BaseDialogFragment extends DialogFragment {

    private AlertDialog loadingDialog;
    private AlertDialog alertDialog;

    private AlertDialog promoDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleUtil.getInstance().setLocale(getActivity());

        Locale locale = LocaleUtil.getInstance().createDefaultLocale();

        Locale.setDefault(locale);
        final Configuration cfg = new Configuration();
        cfg.locale = locale;
        getActivity().getResources().updateConfiguration(cfg, null);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).observeNetworkConnection();
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

    public void loadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setView(getLayoutInflater().inflate(R.layout.loading, null));
        loadingDialog = builder.create();
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        loadingDialog.show();
    }

    public void dismissDialogs() {
        try {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showAlertDialog(String title, String message) {
        dismissDialogs();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        alertDialog = builder.create();
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(getActivity()));
                } else {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                }

            }
        });

        alertDialog.show();

    }

    public void dismissPromptDialog() {
        try {
            if (promoDialog != null && promoDialog.isShowing()) {
                promoDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void showPromptAlertDialog(String title, String message, String positive, DialogInterface.OnClickListener positiveOnClickListener, String negative, DialogInterface.OnClickListener negativeOnClickListener) {
        dismissPromptDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setTitle(title);
        builder.setMessage(message);
        promoDialog = builder.create();
        if (!positive.equals("")) {
            promoDialog.setButton(AlertDialog.BUTTON_POSITIVE, positive, positiveOnClickListener);
        }

        if (!negative.equals("")) {
            promoDialog.setButton(AlertDialog.BUTTON_NEGATIVE, negative, negativeOnClickListener);
        }

        promoDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (!positive.equals("")) {
                    promoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        promoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(getActivity()));
                    } else {
                        promoDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getActivity(), R.color.primary_text));
                    }
                }

                if (!negative.equals("")) {
                    promoDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
                    promoDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getActivity(), R.color.secondary_text));
                }
            }
        });
        promoDialog.show();
    }
}
