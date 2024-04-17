package com.herfy.payment.savedcards;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.DialogFragment;

import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SaveDialogAlert extends DialogFragment {

    OnSavedAction onAction;

    public static SaveDialogAlert newInstance(OnSavedAction onAction) {
        SaveDialogAlert fragment = new SaveDialogAlert();
        fragment.onAction = onAction;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_saved_card_alert, container, false);
        TextView title = view.findViewById(R.id.title);
        TextView labelDesc = view.findViewById(R.id.label_desc);
        TextView noThanks = view.findViewById(R.id.no_thanks);
        TextView save = view.findViewById(R.id.save);
        AppCompatImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAction.onClose();
            }
        });

        int defaultInterval = 1000;
        save.setOnClickListener(new View.OnClickListener() {
            Long lastTimeClicked = 0L;
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
                    return;
                }
                onAction.onProceed();
                lastTimeClicked = SystemClock.elapsedRealtime();
            }
        });
        noThanks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAction.onClose();
            }
        });
        title.setText(CacheUtil.getInstance().getTranslations(Text.SAVE_THIS_CARD, "Save this card?"));
        save.setText(CacheUtil.getInstance().getTranslations(Text.SAVE, "SAVE").toUpperCase());
        noThanks.setText(CacheUtil.getInstance().getTranslations(Text.NO_THANKS_CAPS, "NO THANKS").toUpperCase());
        labelDesc.setText(CacheUtil.getInstance().getTranslations(Text.SAVE_CARD_DESC, "Would you like us to save the card you used for future orders?"));
        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            TypedValue typedValue = new TypedValue();
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setStatusBarColor(typedValue.data);
        }
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        onAction.onClose();
    }

    public interface OnSavedAction {
        void onClose();

        void onProceed();
    }

}

