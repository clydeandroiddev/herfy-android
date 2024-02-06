package com.herfy.country;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.model.SupportedCountry;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CountriesAdapterNew extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SupportedCountry> countries;
    private SupportedCountry selectedCountry;
    private final int size;

    public CountriesAdapterNew(int size, List<SupportedCountry> countries, SupportedCountry selectedCountry) {
        this.countries = countries;
        this.selectedCountry = selectedCountry;
        this.size = size;
    }

    public static class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.mainContainer)
        ConstraintLayout mainContainer;
        @BindView(R.id.cardContainer)
        MaterialCardView cardContainer;
        @BindView(R.id.countryImage)
        ImageView countryImage;
        @BindView(R.id.countryName)
        TextView countryName;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_country, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return countries.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        Context context = holder.itemView.getContext();
        SupportedCountry country = countries.get(position);

        holder.mainContainer.getLayoutParams().width = size;
        holder.mainContainer.getLayoutParams().height = size;

        boolean isSelected;
        if (selectedCountry != null) {
            isSelected = country.applicationId.equalsIgnoreCase(selectedCountry.applicationId);
        } else {
            isSelected = country.applicationId.equalsIgnoreCase(BuildConfig.APPLICATION_ID);
        }

        int img = R.drawable.orange_check;

        if (isSelected) {
            if (LocaleUtil.getInstance().isArabic()) {
                holder.countryName.setCompoundDrawablesWithIntrinsicBounds(img, 0, 0, 0);
            } else {
                holder.countryName.setCompoundDrawablesWithIntrinsicBounds(0, 0, img, 0);
            }
            holder.cardContainer.setStrokeWidth(2);
        } else {
            holder.countryName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            holder.cardContainer.setStrokeWidth(0);
        }

        holder.mainContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCountry = country;
                notifyDataSetChanged();
            }
        });

        if (country.imageUrl.isEmpty()) {
            holder.countryImage.setImageResource(R.drawable.header);
        } else {
            Glide.with(context).load(country.imageUrl).diskCacheStrategy(DiskCacheStrategy.ALL).apply(RequestOptions.centerInsideTransform()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            }).into(holder.countryImage);
        }

        holder.countryName.setText(CacheUtil.getInstance().getTranslations(country.applicationName,country.applicationName));

    }

    public SupportedCountry getSelectedCountry() {
        return selectedCountry;
    }
}


