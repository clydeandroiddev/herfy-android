package com.herfy.curbside.dialog;

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
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.curbside.adapter.ColorListAdapter;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ColorListDialog extends DialogFragment implements ColorListAdapter.ColorListListener {

    private ColorListAdapter adapter;
    private List<String> colors;
    private OnAction onAction;

    public static ColorListDialog newInstance(List<String> colors, OnAction onAction) {
        ColorListDialog fragment = new ColorListDialog();
        fragment.colors = colors;
        fragment.onAction = onAction;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.fragment_dialog_car_color, container, false);
        RecyclerView colorList = view.findViewById(R.id.color_list);
        TextView colorLabel = view.findViewById(R.id.label);
        colorLabel.setText(CacheUtil.getInstance().getTranslations(Text.CHOOSE_A_COLOR, "Choose a color"));
        if (adapter == null) {
            adapter = new ColorListAdapter(getActivity(), colors, this);
        }
        colorList.setAdapter(adapter);
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

    @Override
    public void select(String color, int position) {
        onAction.onAvail(color);
    }

    public interface OnAction {
        void onClose();

        void onAvail(String color);
    }

}

