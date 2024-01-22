package com.herfy.payment.savedcards;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.constants.Text;
import com.herfy.payment.savedcards.adapter.SavedCardsAdapter;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.herfy.util.CacheUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SavedEditCardsDialog extends DialogFragment implements SavedCardsAdapter.SavedCardsListener {

    SavedCardsAdapter adapter;
    private List<CustomerCard> cards = new ArrayList<>();
    OnSavedAction onAction;
    private CustomerCard customerCard;

    public static SavedEditCardsDialog newInstance(List<CustomerCard> cards, OnSavedAction onAction) {
        SavedEditCardsDialog fragment = new SavedEditCardsDialog();
        fragment.onAction = onAction;
        fragment.cards = cards;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_saved_cards, container, false);
        TextView useNewCards = view.findViewById(R.id.useNewCards);
        RecyclerView listCards = view.findViewById(R.id.list_cards);
        TextView title = view.findViewById(R.id.title);
        ImageView close = view.findViewById(R.id.iv_close);
        TextView savedCards = view.findViewById(R.id.saved_cards);
        TextView editCards = view.findViewById(R.id.cancel);
        TextView confirm = view.findViewById(R.id.apply_changes);
        ConstraintLayout mainLayout = view.findViewById(R.id.main_layout);
        mainLayout.setBackgroundResource(R.color.page_background_color);
        savedCards.setText(CacheUtil.getInstance().getTranslations(Text.SAVED_CARDS, "Saved Cards"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.PAY_BY_CARD, "Pay by Card"));
        useNewCards.setText(CacheUtil.getInstance().getTranslations(Text.USE_NEW_CARDS_CAPS, "USE NEW CARD"));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM"));
        editCards.setText(CacheUtil.getInstance().getTranslations(Text.EDIT_CARDS_CAPS, "EDIT CARDS"));

        confirm.setOnClickListener(view13 -> {
            if (customerCard != null) {
                onAction.onAvail(customerCard);
            }
        });

        close.setOnClickListener(view14 -> onDismiss(requireDialog()));
        editCards.setOnClickListener(view12 -> onAction.onEditCards());
        useNewCards.setOnClickListener(view1 -> onAction.onUseNewCard());

        if (adapter == null) {
            adapter = new SavedCardsAdapter(getActivity(), cards, this);
        }
        listCards.setAdapter(adapter);

        return view;
    }

    public void updateSaveCards() {
        customerCard = null;
        cards.clear();
        cards.addAll(CacheUtil.getInstance().getCustomerCards());
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
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
            TypedValue typedValue = new TypedValue();
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog.getWindow().setStatusBarColor(typedValue.data);
        }
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        onAction.onClose();
    }

    @Override
    public void selectCard(CustomerCard maker, int position, boolean isSelected) {
        adapter.selectedCardId = maker.id;
        adapter.notifyDataSetChanged();
        customerCard = maker;
    }

    @Override
    public void deleteCard(CustomerCard maker, int position) {

    }

    public interface OnSavedAction {
        void onClose();

        void onEditCards();

        void onUseNewCard();

        void onAvail(CustomerCard card);
    }

}

