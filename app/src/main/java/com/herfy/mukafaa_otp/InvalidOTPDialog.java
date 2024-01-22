package com.herfy.mukafaa_otp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.herfy.R;
import com.herfy.constants.Text;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InvalidOTPDialog extends DialogFragment {

    private OnAction onAction;
    private String message;

    public static InvalidOTPDialog newInstance(String message, OnAction onAction) {
        InvalidOTPDialog fragment = new InvalidOTPDialog();
        fragment.onAction = onAction;
        fragment.message = message;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_invalid_otp, container, false);
        TextView okay = view.findViewById(R.id.okay);
        TextView labelDesc = view.findViewById(R.id.label_desc);
        TextView title = view.findViewById(R.id.title);
        ImageView icCloseDialog = view.findViewById(R.id.ic_close_dialog);

        labelDesc.setText(message);
        title.setText(CacheUtil.getInstance().getTranslations(Text.ERROR_CAPS, "ERROR"));
        okay.setText(CacheUtil.getInstance().getTranslations(Text.OKAY_CAPS, "OKAY").toUpperCase());
        icCloseDialog.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                onAction.onClose();
            }
        });


        okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAction.onClose();
            }
        });
        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        onAction.onClose();
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }
        }
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        onAction.onClose();
    }

    public interface OnAction {
        void onClose();
    }

}