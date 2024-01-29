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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.curbside.adapter.CarMakerListAdapter;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CarMakerDialog extends DialogFragment implements CarMakerListAdapter.CarMakerListener {

    private CarMakerListAdapter adapter;
    private List<CarMaker> carMakers;
    private OnActionCarMaker onAction;

    private TextView labelDesc;
    private ConstraintLayout checkManualColor;
    private EditText carMakerText;
    private CardView checkLayout;

    private NestedScrollView nestedMain;

    public static CarMakerDialog newInstance(List<CarMaker> colors, OnActionCarMaker onAction) {
        CarMakerDialog fragment = new CarMakerDialog();
        fragment.carMakers = colors;
        fragment.onAction = onAction;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.fragment_dialog_car_maker, container, false);
        RecyclerView colorList = view.findViewById(R.id.maker_list);
        TextView colorLabel = view.findViewById(R.id.label);
        nestedMain = view.findViewById(R.id.nested_main);
        carMakerText = view.findViewById(R.id.car_maker_text);
        labelDesc = view.findViewById(R.id.label_desc);
        checkLayout = view.findViewById(R.id.check_layout);
        checkManualColor = view.findViewById(R.id.check_manual_color);
        labelDesc.setText(CacheUtil.getInstance().getTranslations(Text.YOUR_CAR_MAKER_IS_ON_THE_LIST, "Your car maker is not on the list?"));
        colorLabel.setText(CacheUtil.getInstance().getTranslations(Text.CAR_MANUFACTURER, "Car Manufacturer"));
        if (adapter == null) {
            adapter = new CarMakerListAdapter(getActivity(), carMakers, this);
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
    public void selectCar(CarMaker maker, int position) {
        onAction.onAvail(maker, null);
    }

    @Override
    public void selectOther() {
        labelDesc.setVisibility(View.VISIBLE);
        carMakerText.setVisibility(View.VISIBLE);
        checkLayout.setVisibility(View.VISIBLE);
        nestedMain.post(() -> nestedMain.fullScroll(View.FOCUS_DOWN));
        checkManualColor.setOnClickListener(v -> {
            if (!carMakerText.getText().toString().trim().isEmpty())
                onAction.onAvail(null, carMakerText.getText().toString());
        });
    }

    public interface OnActionCarMaker {
        void onClose();

        void onAvail(@Nullable CarMaker maker, @Nullable String other);
    }

}
