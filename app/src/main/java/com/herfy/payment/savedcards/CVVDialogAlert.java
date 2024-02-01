package com.herfy.payment.savedcards;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.DialogFragment;

import com.herfy.R;
import com.herfy.data.constants.Text;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CVVDialogAlert extends DialogFragment {

    OnCVVAction onAction;
    CustomerCard card;

    public static CVVDialogAlert newInstance(OnCVVAction onAction, CustomerCard card) {
        CVVDialogAlert fragment = new CVVDialogAlert();
        fragment.onAction = onAction;
        fragment.card = card;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_enter_cvv, container, false);
        TextView title = view.findViewById(R.id.title);
        EditText cvvText = view.findViewById(R.id.cvv_text);
        TextView tvNumber = view.findViewById(R.id.tv_number);
        TextView applyChanges = view.findViewById(R.id.apply_changes);
        AppCompatImageView closeButton = view.findViewById(R.id.closeButton);

        tvNumber.setText(card.attributes.last4Digits);
        title.setText(CacheUtil.getInstance().getTranslations(Text.ENTER_CVV, "Enter CVV"));
        applyChanges.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAction.onClose();
            }
        });

        applyChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!cvvText.getText().toString().isEmpty()) {
                    onAction.onProceed(cvvText.getText().toString());
                }
            }
        });

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

    public interface OnCVVAction {
        void onClose();

        void onProceed(String cvv);
    }

}

