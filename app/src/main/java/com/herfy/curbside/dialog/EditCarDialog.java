package com.herfy.curbside.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.herfy.R;
import com.herfy.constants.Text;
import com.herfy.constants.Values;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar;
import com.herfy.util.CacheUtil;
import com.herfy.util.ViewUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class EditCarDialog extends DialogFragment {

    @BindView(R.id.plate_number_text)
    EditText plateNumberText;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.plate_number_label)
    TextView plateNumberLabel;
    @BindView(R.id.car_color_label)
    TextView carColorLabel;
    @BindView(R.id.car_maker_label)
    TextView carMakerLabel;
    @BindView(R.id.cancel)
    TextView cancel;
    @BindView(R.id.apply_changes)
    TextView applyChanges;
    @BindView(R.id.car_color_constraint)
    FloatingActionButton carColorConstraint;
    @BindView(R.id.color_image_selected)
    CircleImageView colorImageSelected;
    @BindView(R.id.maker_color_constraint)
    FloatingActionButton makerColorConstraint;
    @BindView(R.id.maker_image)
    CircleImageView makerImage;
    @BindView(R.id.maker_other_text)
    TextView makerOtherText;

    private OnAction onAction;
    private CustomerCar customerCar;
    private List<String> colors;
    private List<CarMaker> makers;
    private ColorListDialog colorListDialog;
    private CarMakerDialog carMakerDialog;

    public static EditCarDialog newInstance(CustomerCar customerCar, List<String> colors, List<CarMaker> makers, OnAction onAction) {
        EditCarDialog fragment = new EditCarDialog();
        fragment.onAction = onAction;
        fragment.colors = colors;
        fragment.makers = makers;
        fragment.customerCar = customerCar;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Objects.requireNonNull(getDialog()).requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_edit_car, container, false);
        ButterKnife.bind(this, view);
        title.setText(CacheUtil.getInstance().getTranslations(Text.EDIT_VEHICLE, "Edit Vehicle"));
        plateNumberLabel.setText(CacheUtil.getInstance().getTranslations(Text.PLATE_NUMBER_OPTIONAL, "Plate Number (Optional)"));
        carMakerLabel.setText(CacheUtil.getInstance().getTranslations(Text.MAKER, "Maker"));
        carColorLabel.setText(CacheUtil.getInstance().getTranslations(Text.COLOR, "Color"));
        cancel.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL").toUpperCase());
        applyChanges.setText(CacheUtil.getInstance().getTranslations(Text.APPLY_CHANGES, "APPLY CHANGES").toUpperCase());

        if (customerCar.attributes.plateNumber != null && !customerCar.attributes.plateNumber.isEmpty()) {
            plateNumberText.setText(customerCar.attributes.plateNumber);
        }

        if (customerCar.attributes.carMakerId == null || customerCar.attributes.carMakerId.isEmpty()) {
            makerImage.setVisibility(View.GONE);
            makerOtherText.setVisibility(View.VISIBLE);
            makerOtherText.setText(customerCar.attributes.carMakerName);
        } else {
            makerImage.setVisibility(View.VISIBLE);
            makerOtherText.setVisibility(View.GONE);
            String imageUrl = customerCar.attributes.carMakerImageUri;
            if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
                imageUrl = Values.DEFAULT_IMAGE_URL;
            }
            Glide.with(this).load(imageUrl).apply(new RequestOptions().override(80, 80)).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            }).into(makerImage);
        }

        colorImageSelected.setImageDrawable(ViewUtil.getColorDrawable("#" + customerCar.attributes.color.replace("#", "")));

        carColorConstraint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorListDialog = ColorListDialog.newInstance(colors, new ColorListDialog.OnAction() {
                    @Override
                    public void onClose() {
                        colorListDialog.dismiss();
                    }

                    @Override
                    public void onAvail(String color) {
                        colorListDialog.dismiss();
                        customerCar.attributes.color = color;
                        colorImageSelected.setVisibility(View.VISIBLE);
                        colorImageSelected.setImageDrawable(ViewUtil.getColorDrawable("#" + color.replace("#", "")));
                    }

                });
                colorListDialog.setCancelable(true);
                colorListDialog.show(getChildFragmentManager(), ColorListDialog.class.toString());
            }
        });

        makerColorConstraint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carMakerDialog = CarMakerDialog.newInstance(makers, new CarMakerDialog.OnActionCarMaker() {
                    @Override
                    public void onClose() {
                        carMakerDialog.dismiss();
                    }

                    @Override
                    public void onAvail(CarMaker color, String other) {
                        carMakerDialog.dismiss();
                        if (other == null) {
                            String imageUrl = color.attributes.imageUri;
                            makerImage.setVisibility(View.VISIBLE);
                            makerOtherText.setVisibility(View.GONE);
                            if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
                                imageUrl = Values.DEFAULT_IMAGE_URL;
                            }
                            Glide.with(EditCarDialog.this).load(imageUrl).apply(new RequestOptions().override(80, 80)).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    return false;
                                }
                            }).into(makerImage);
                            customerCar.attributes.carMakerId = color.id;
                            customerCar.attributes.carMakerName = null;
                        } else {
                            makerImage.setVisibility(View.GONE);
                            makerOtherText.setVisibility(View.VISIBLE);
                            customerCar.attributes.carMakerId = null;
                            customerCar.attributes.carMakerName = other;
                            makerOtherText.setText(other);
                        }
                    }

                });
                carMakerDialog.setCancelable(true);
                carMakerDialog.show(getChildFragmentManager(), CarMakerDialog.class.toString());
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAction.onClose();
            }
        });

        applyChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (plateNumberText.getText().toString().trim().isEmpty()) {
                    customerCar.attributes.plateNumber = null;
                } else {
                    customerCar.attributes.plateNumber = plateNumberText.getText().toString();
                }
                onAction.onEdit(customerCar);
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

        void onEdit(CustomerCar customerCar);
    }

}

