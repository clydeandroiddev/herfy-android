package com.herfy.payment.savedcards.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SavedEditCardsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface SavedEditCardsListener {
        void selectCard(CustomerCard maker, int position, boolean isSelected);
    }

    private SavedEditCardsListener listener;

    private List<CustomerCard> cards;
    private Context context;
    public String selectedCardId = "";

    public SavedEditCardsAdapter(Context context, List<CustomerCard> cards, SavedEditCardsListener listener) {
        this.cards = cards;
        this.context = context;
        this.listener = listener;
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_card)
        ImageView ivCard;
        @BindView(R.id.tv_number)
        TextView number;
        @BindView(R.id.iv_selected)
        ImageView ivSelected;
        @BindView(R.id.main)
        ConstraintLayout main;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_payment_card, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView(((ViewHolderData) holder), position);
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        CustomerCard customerCard = cards.get(position);

        holder.main.setOnClickListener(view -> listener.selectCard(customerCard, position, (customerCard.id.equalsIgnoreCase(selectedCardId))));

        if (customerCard.id.equalsIgnoreCase(selectedCardId)) {
            holder.ivSelected.setVisibility(View.VISIBLE);
        } else {
            holder.ivSelected.setVisibility(View.GONE);
        }

        holder.number.setText(customerCard.getAttributes().getLast4Digits());

        int res = R.drawable.ic_saved_card;

        if ("visa".equalsIgnoreCase(customerCard.getAttributes().getBrand()))
            res = R.drawable.ic_card_visa_common;
        else if ("mastercard".equalsIgnoreCase(customerCard.getAttributes().getBrand()))
            res = R.drawable.ic_card_mastercard;

        holder.ivCard.setImageResource(res);

    }
}

