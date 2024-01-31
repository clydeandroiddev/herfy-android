package com.herfy.curbside.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.skylinedynamics.solosdk.api.models.objects.curbside.CarMaker;
import com.herfy.util.CacheUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class CarMakerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface CarMakerListener {
        void selectCar(CarMaker maker, int position);

        void selectOther();
    }

    private CarMakerListener listener;

    private List<CarMaker> makers;
    private Context context;

    public CarMakerListAdapter(Context context, List<CarMaker> makers, CarMakerListener listener) {
        this.makers = makers;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.item_maker_constraint)
        FloatingActionButton itemContainer;
        @BindView(R.id.other_layout)
        LinearLayout otherLayout;
        @BindView(R.id.other_label)
        TextView otherLabel;
        @BindView(R.id.color_image)
        CircleImageView colorImage;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_car_maker, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return makers.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        final CarMaker maker = makers.get(position);
        holder.itemContainer.setOnClickListener(v -> {
            if (position == (makers.size() - 1)) {
                listener.selectOther();
            } else {
                listener.selectCar(maker, position);
            }
        });

        holder.otherLabel.setText(CacheUtil.getInstance().getTranslations(Text.OTHER, "Other"));

        if (position == (makers.size() - 1)) {
            holder.otherLayout.setVisibility(View.VISIBLE);
            holder.colorImage.setVisibility(View.INVISIBLE);
        } else {
            String imageUrl = maker.attributes.imageUri;
            holder.colorImage.setVisibility(View.VISIBLE);
            if (imageUrl == null || imageUrl.equalsIgnoreCase("null") || imageUrl.isEmpty() || imageUrl.toLowerCase().contains("default-image.png")) {
                imageUrl = Values.DEFAULT_IMAGE_URL;
            }
            Glide.with(context).load(imageUrl).apply(new RequestOptions().override(80, 80)).skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            }).into(holder.colorImage);
            holder.otherLayout.setVisibility(View.GONE);

        }


    }
}

