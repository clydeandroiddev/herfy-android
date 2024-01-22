package com.herfy.concepts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.concepts.views.ConceptViewHolder;

public class ConceptsRecyclerViewAdapter extends RecyclerView.Adapter<ConceptViewHolder> {

    private final ConceptsContract.Presenter conceptsPresenter;

    private Context context;

    private int selected = -1;

    public ConceptsRecyclerViewAdapter(Context context, ConceptsContract.Presenter changeStorePresenter) {
        this.context = context;
        this.conceptsPresenter = changeStorePresenter;
    }

    @NonNull
    @Override
    public ConceptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConceptViewHolder(context, conceptsPresenter, LayoutInflater.from(parent.getContext()).inflate(R.layout.item_concept, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ConceptViewHolder holder, int position) {
        conceptsPresenter.onBindConceptViewAtPosition(holder, position, selected == position);
    }

    @Override
    public int getItemCount() {
        return conceptsPresenter.getConceptsCount();
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public int getSelected() {
        return selected;
    }

}