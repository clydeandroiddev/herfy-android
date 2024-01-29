package com.herfy.loyalty;

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
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.DialogFragment;

import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EarnablePointsDialog extends DialogFragment {

    @BindView(R.id.close)
    AppCompatImageButton close;

    @BindView(R.id.tvPoints)
    TextView tvPoints;


    public interface OnSubmit {
        void onSubmit(Dialog dialog);
    }

    private OnSubmit onSubmit;

    public static EarnablePointsDialog newInstance(OnSubmit onSubmit) {
        EarnablePointsDialog fragment = new EarnablePointsDialog();
        fragment.onSubmit = onSubmit;
        return fragment;
    }

    public EarnablePointsDialog(){

    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_earnable_points, container, false);
        ButterKnife.bind(this,view);
        return view;
    }


    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String text = CacheUtil.getInstance().getTranslations(Text.EARNABLE_POINTS,"You will earn (x) points once your order is completed.").replace("(x)",String.valueOf(CacheUtil.getInstance().getLoyaltyPointsEarnable("")));

      /*  tvPoints.setText( Html.fromHtml("You just earned <font color=#84C5B2>"+CacheUtil.getInstance().getLoyaltyPointsEarnable() +
              " pts</font> bringing your total to <font color=#84C5B2>"+ (AuthUtil.getInstance().getCustomerLoyalty().getAttributes().getPointsAccumulated()+
              CacheUtil.getInstance().getLoyaltyPointsEarnable()) +" pts</font>"));*/

        tvPoints.setText(text);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit.onSubmit(getDialog());
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TypedValue typedValue = new TypedValue();
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                dialog.getWindow().setStatusBarColor(typedValue.data);
            }

            dialog.setCanceledOnTouchOutside(true);
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        //CacheUtil.getInstance().setLoyaltyPointsEarnable(0);
    }

}

