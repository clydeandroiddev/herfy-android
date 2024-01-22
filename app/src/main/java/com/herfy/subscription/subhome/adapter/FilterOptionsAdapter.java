package com.herfy.subscription.subhome.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.subscription.subhome.StubFilterModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class FilterOptionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    public interface FilterOptionsListener {
        void select(StubFilterModel menu, int position);
    }

    private FilterOptionsListener listener;

    private List<StubFilterModel> menus;
    private Context context;

    public FilterOptionsAdapter(Context context, List<StubFilterModel> menus, FilterOptionsListener listener) {
        this.menus = menus;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.item_check)
        ImageButton checkBox;
        @BindView(R.id.filter_name)
        AppCompatTextView filterName;
        @BindView(R.id.item_container)
        ConstraintLayout containerItem;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_filter_option, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return menus.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {

        final StubFilterModel menu = menus.get(position);

        holder.filterName.setText(menu.name);

        if (menu.selected) {
            holder.checkBox.setImageResource(R.drawable.ic_checked_checkbox);
        } else {
            holder.checkBox.setImageResource(R.drawable.ic_unchecked_checkbox_svg);
        }

        holder.containerItem.setOnClickListener(v -> {
            listener.select(menu, position);
        });

    }
}
