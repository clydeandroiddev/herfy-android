package com.herfy.menu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.SizeViewHolder;

public class SizesRecyclerViewAdapter extends RecyclerView.Adapter<SizeViewHolder> {

    private Context context;

    private final MenuContract.Presenter menuPresenter;

    private SizeViewHolder.OnAction onAction;

    public void setOnAction(SizeViewHolder.OnAction onAction) {
        this.onAction = onAction;
    }

    public SizesRecyclerViewAdapter(Context context, MenuContract.Presenter menuPresenter) {
        this.context = context;
        this.menuPresenter = menuPresenter;
    }

    @NonNull
    @Override
    public SizeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        SizeViewHolder sizeViewHolder = new SizeViewHolder(context, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_size, parent, false));
        sizeViewHolder.setOnAction(onAction);
        return sizeViewHolder;

    }

    @Override
    public void onBindViewHolder(@NonNull SizeViewHolder holder, int position) {
         menuPresenter.onBindSizeViewAtPosition(position, holder);
    }

    @Override
    public int getItemCount() {
        return menuPresenter.getSizesCount();
    }
}
