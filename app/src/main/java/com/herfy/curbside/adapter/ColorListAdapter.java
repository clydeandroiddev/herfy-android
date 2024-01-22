package com.herfy.curbside.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.herfy.R;
import com.herfy.util.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

public class ColorListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface ColorListListener {
        void select(String color, int position);
    }

    private ColorListListener listener;

    private List<String> colors;
    private Context context;

    public ColorListAdapter(Context context, List<String> colors, ColorListListener listener) {
        this.colors = colors;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.item_color_constraint)
        FloatingActionButton itemColor;
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
                R.layout.item_car_color, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        final String color = colors.get(position);
        holder.colorImage.setImageDrawable(ViewUtil.getColorDrawable("#" + color.replace("#", "")));
        holder.itemColor.setOnClickListener(v -> listener.select(color, position));
    }
}

