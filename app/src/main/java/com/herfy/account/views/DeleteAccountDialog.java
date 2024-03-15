package com.herfy.account.views;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeleteAccountDialog extends DialogFragment {

    private OnAction onAction;

    public static DeleteAccountDialog newInstance(OnAction onAction) {
        DeleteAccountDialog fragment = new DeleteAccountDialog();
        fragment.onAction = onAction;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_delete_account_new, container, false);
        MaterialButton delete = view.findViewById(R.id.delete);
        MaterialButton cancel = view.findViewById(R.id.cancel);
        MaterialCardView card = view.findViewById(R.id.card);
        TextView labelDesc = view.findViewById(R.id.label_desc);
        TextView title = view.findViewById(R.id.title);
        title.setText(CacheUtil.getInstance().getTranslations(Text.DELETE_ACCOUNT_ASK, "Delete account?"));
        labelDesc.setText(CacheUtil.getInstance().getTranslations(Text.DELETE_ACCOUNT_CANNOT_UNDONE, "Are you sure you want to delete your account? This cannot be undone."));

        delete.setText(CacheUtil.getInstance().getTranslations(Text.DELETE, "DELETE").toUpperCase());
        cancel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL").toUpperCase());

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                onAction.onDelete();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
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

        void onDelete();
    }

}

