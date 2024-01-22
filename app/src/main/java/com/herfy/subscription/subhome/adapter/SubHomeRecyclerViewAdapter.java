package com.herfy.subscription.subhome.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.subscription.subhome.SubHomeContract;
import com.herfy.subscription.subhome.viewholder.SubHomeViewHolder;

import java.util.ArrayList;


public class SubHomeRecyclerViewAdapter extends RecyclerView.Adapter<SubHomeViewHolder> {

    private Context mContext;
    private final SubHomeContract.Presenter menuPresenter;
    private ArrayList<Integer> items;

    public SubHomeRecyclerViewAdapter(Context context, SubHomeContract.Presenter menuPresenter, ArrayList<Integer> items) {
        this.mContext = context;
        this.menuPresenter = menuPresenter;
        this.items = items;
    }

    @NonNull
    @Override
    public SubHomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SubHomeViewHolder(mContext, menuPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_home, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull SubHomeViewHolder holder, int position) {
        if (items.size() != 0) {
            holder.setSubItem(items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

}