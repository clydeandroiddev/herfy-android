package com.herfy.curbside.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.utils.constants.Values;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CustomerCar;
import com.herfy.util.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerCarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface CustomerCarListener {
        void selectCar(CustomerCar maker, int position, boolean isSelected);

        void editCar(CustomerCar maker, int position);

        void deleteCar(CustomerCar maker, int position);
    }

    private CustomerCarListener listener;

    private List<CustomerCar> cars;
    private Context context;
    public String selectedCarId = "";
    public boolean isEditMode;

    public CustomerCarAdapter(Context context, List<CustomerCar> cars, CustomerCarListener listener) {
        this.cars = cars;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {


        @BindView(R.id.item_car_selected)
        ImageView itemCarSelected;
        @BindView(R.id.item_car_name)
        TextView itemCarName;
        @BindView(R.id.main_layout)
        MaterialCardView mainLayout;
        @BindView(R.id.main_constraint)
        ConstraintLayout mainConstraint;
        @BindView(R.id.car_color_label)
        TextView carColorLabel;
        @BindView(R.id.item_car_maker_label)
        TextView itemCarMakerLabel;
        @BindView(R.id.color_image)
        CircleImageView colorImage;
        @BindView(R.id.maker_image)
        CircleImageView makerImage;
        @BindView(R.id.maker_other_text)
        TextView makerOtherText;
        @BindView(R.id.editable_layout)
        LinearLayout editableLayout;
        @BindView(R.id.edit_vehicle)
        ImageView editVehicle;
        @BindView(R.id.delete_vehicle)
        ImageView deleteVehicle;
        @BindView(R.id.edit_vehicle_constraint)
        ConstraintLayout editVehicleConstraint;
        @BindView(R.id.delete_vehicle_constraint)
        ConstraintLayout deleteVehicleConstraint;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_cars, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return cars.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        CustomerCar customerCar = cars.get(position);

        holder.carColorLabel.setText(CacheUtil.getInstance().getTranslations(Text.COLOR, "Color"));
        holder.itemCarMakerLabel.setText(CacheUtil.getInstance().getTranslations(Text.MAKER, "Maker"));

        String carName;
        if (customerCar.attributes.plateNumber != null && !customerCar.attributes.plateNumber.isEmpty()) {
            carName = customerCar.attributes.plateNumber;
        } else {
            carName = customerCar.attributes.carMakerName;
        }

        if (customerCar.attributes.carMakerId == null || customerCar.attributes.carMakerId.isEmpty()) {
            holder.makerImage.setVisibility(View.GONE);
            holder.makerOtherText.setVisibility(View.VISIBLE);
            holder.makerOtherText.setText(customerCar.attributes.carMakerName);
        } else {
            holder.makerImage.setVisibility(View.VISIBLE);
            holder.makerOtherText.setVisibility(View.GONE);
            String imageUrl = customerCar.attributes.carMakerImageUri;
            if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
                imageUrl = Values.DEFAULT_IMAGE_URL;
            }
            Glide.with(context).load(imageUrl).apply(new RequestOptions().override(80, 80)).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            }).into(holder.makerImage);
        }

        holder.colorImage.setImageDrawable(ViewUtil.getColorDrawable("#" + customerCar.attributes.color.replace("#", "")));

        holder.itemCarName.setText(carName);

        holder.mainConstraint.setOnClickListener(v -> {
            if (!isEditMode)
                listener.selectCar(customerCar, position, customerCar.id.equals(selectedCarId));
        });

        if (isEditMode) {
            holder.mainLayout.setStrokeWidth(0);
            holder.editableLayout.setVisibility(View.VISIBLE);
            holder.itemCarSelected.setVisibility(View.GONE);

            holder.editVehicleConstraint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.editCar(customerCar, position);
                }
            });

            holder.deleteVehicleConstraint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.deleteCar(customerCar, position);
                }
            });

        } else {
            if (customerCar.id.equals(selectedCarId)) {
                holder.mainLayout.setStrokeWidth(2);
                holder.itemCarSelected.setImageResource(R.drawable.language_selected);
            } else {
                holder.itemCarSelected.setImageResource(R.drawable.ic_rectangle_47);
                holder.mainLayout.setStrokeWidth(0);
            }
            holder.itemCarSelected.setVisibility(View.VISIBLE);
            holder.editableLayout.setVisibility(View.GONE);
        }
    }
}

