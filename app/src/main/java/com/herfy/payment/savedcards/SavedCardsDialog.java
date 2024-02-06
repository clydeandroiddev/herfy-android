package com.herfy.payment.savedcards;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.payment.savedcards.adapter.SavedCardsAdapter;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.objects.CustomerCard;
import com.herfy.util.AppLogger;
import com.herfy.util.StringUtil;
import com.herfy.util.ViewUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SavedCardsDialog extends DialogFragment implements SavedCardsAdapter.SavedCardsListener {

    SavedCardsAdapter adapter;
    OnSavedAction onAction;
    private List<CustomerCard> cards = new ArrayList<>();
    private CustomerCard customerCard;
    private TextView title;
    private TextView useNewCards;
    private TextView editCards;
    private DeleteCardDialog deleteCardDialog;
    private AlertDialog loadingDialog;
    private AlertDialog alertDialog;
    private boolean isEditMode;
    private TextView confirm;
    private MaterialButton close;

    public static SavedCardsDialog newInstance(List<CustomerCard> cards, OnSavedAction onAction) {
        SavedCardsDialog fragment = new SavedCardsDialog();
        fragment.onAction = onAction;
        fragment.cards = cards;
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = inflater.inflate(R.layout.dialog_saved_cards, container, false);
        useNewCards = view.findViewById(R.id.useNewCards);
        RecyclerView listCards = view.findViewById(R.id.list_cards);
        title = view.findViewById(R.id.title);
        close = view.findViewById(R.id.back);
        TextView savedCards = view.findViewById(R.id.saved_cards);
        editCards = view.findViewById(R.id.editOrDone);
        confirm = view.findViewById(R.id.apply_changes);
        ConstraintLayout mainLayout = view.findViewById(R.id.main_layout);
        mainLayout.setBackgroundResource(R.color.page_background_color);
        savedCards.setText(CacheUtil.getInstance().getTranslations(Text.SAVED_CARDS, "Saved Cards"));
        title.setText(CacheUtil.getInstance().getTranslations(Text.CHOOSE_CARD_CAPS, "CHOOSE CARD"));
        useNewCards.setText(CacheUtil.getInstance().getTranslations(Text.USE_NEW_CARDS_CAPS, "USE NEW CARD"));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.PROCEED_WITH_THIS_CARD_CAPS, "PROCEED WITH THIS CARD"));
        editCards.setText(CacheUtil.getInstance().getTranslations(Text.EDIT_CAPS, "EDIT"));
        close.setText(CacheUtil.getInstance().getTranslations(Text.CLOSE, "Close"));

        confirm.setOnClickListener(view13 -> {
            if (!isEditMode) {
                if (customerCard != null) {
                    onAction.onAvail(customerCard);
                }
            } else {
                switchToNormal();
            }
        });

        close.setOnClickListener(view14 -> onAction.onClose());

        editCards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isEditMode) {
                    onAction.onEditCards();
                } else {
                    switchToNormal();
                }
            }
        });

        useNewCards.setOnClickListener(view1 -> onAction.onUseNewCard());

        if (adapter == null) {
            adapter = new SavedCardsAdapter(getActivity(), cards, this);
        }
        listCards.setAdapter(adapter);

        return view;
    }

    private void switchToNormal() {
        adapter.isEditMode = false;
        isEditMode = false;
        close.setVisibility(View.VISIBLE);
        if (adapter != null)
            adapter.notifyDataSetChanged();
        useNewCards.setVisibility(View.VISIBLE);
        title.setText(CacheUtil.getInstance().getTranslations(Text.CHOOSE_CARD_CAPS, "CHOOSE CARD"));
        editCards.setText(CacheUtil.getInstance().getTranslations(Text.EDIT_CAPS, "EDIT"));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.PROCEED_WITH_THIS_CARD_CAPS, "PROCEED WITH THIS CARD"));
        String primary = StringUtil.colorIntToString(ViewUtil.getColorMain(requireDialog().getContext()));
        if (customerCard != null) {
            confirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primary)));
        } else {
            confirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireDialog().getContext(), R.color.secondary_text)));
        }
    }

    public void switchToEditMode() {
        adapter.isEditMode = true;
        isEditMode = true;
        if (adapter != null)
            adapter.notifyDataSetChanged();
        close.setVisibility(View.GONE);
        useNewCards.setVisibility(View.GONE);
        title.setText(CacheUtil.getInstance().getTranslations(Text.EDIT_CARDS_CAPS, "EDIT CARDS"));
        String primary = StringUtil.colorIntToString(ViewUtil.getColorMain(requireDialog().getContext()));
        confirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primary)));
        confirm.setText(CacheUtil.getInstance().getTranslations(Text.SAVE_CHANGES, "SAVE CHANGES").toUpperCase());
        editCards.setText(CacheUtil.getInstance().getTranslations(Text.DONE_CAPS, "DONE"));
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
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
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
        String primary = StringUtil.colorIntToString(ViewUtil.getColorMain(requireDialog().getContext()));
        confirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primary)));
    }

    @Override
    public void deleteCard(CustomerCard maker, int position) {
        if (deleteCardDialog != null) {
            deleteCardDialog.dismiss();
        }

        deleteCardDialog = DeleteCardDialog.newInstance(new DeleteCardDialog.OnDeleteAction() {
            @Override
            public void onClose() {
                deleteCardDialog.dismiss();
            }

            @Override
            public void onProceed() {
                deleteCardDialog.dismiss();
                showLoadingDialog();
                CustomersApiCall.getInstance().deleteCard(AuthUtil.getInstance().getCustomer().getId(), maker.getId() + "",
                        new ApiRequestListener() {
                            @Override
                            public void onSuccess(Object o) {
                                dismissDialogs();
                                CustomerCard card = cards.get(position);
                                if (card.id.equalsIgnoreCase(adapter.selectedCardId)) {
                                    adapter.selectedCardId = "";
                                    customerCard = null;
                                    confirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireDialog().getContext(), R.color.secondary_text)));
                                }
                                cards.remove(position);
                                adapter.notifyDataSetChanged();
                                if (cards.isEmpty()) {
                                    onAction.onUseNewCard();
                                    CacheUtil.getInstance().setCustomerCards(null);
                                } else {
                                    CacheUtil.getInstance().setCustomerCards(new Gson().toJson(cards));
                                }
                            }

                            @Override
                            public void onError(Object o) {
                                dismissDialogs();
                                AppLogger.printRetrofitError(o);
                                if (o instanceof JSONObject) {
                                    try {
                                        JSONObject object = (JSONObject) o;
                                        Object error = object.get("error");
                                        String detail = CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT);
                                        if (error instanceof JSONObject) {
                                            detail = ((JSONObject) error).getString("detail");
                                        } else if (error instanceof JSONArray) {
                                            JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                                            detail = errorObject.getString("detail");
                                        }
                                        showAlertDialog("", detail);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                                    }
                                } else {
                                    showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.SORRY_ERROR_PAYMENT));
                                }
                            }
                        });
            }
        });
        deleteCardDialog.setCancelable(false);
        deleteCardDialog.show(requireActivity().getSupportFragmentManager(), DeleteCardDialog.class.toString());
    }

    private void dismissDialogs() {
        try {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showLoadingDialog() {
        try {
            dismissDialogs();
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setCancelable(false);

            builder.setView(getLayoutInflater().inflate(R.layout.loading, null));

            loadingDialog = builder.create();

            loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            loadingDialog.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showAlertDialog(String title, String message) {

        try {
            dismissDialogs();
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setCancelable(true);
            builder.setTitle(title);
            builder.setMessage(message);
            alertDialog = builder.create();
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                    if (!CacheUtil.getInstance().getColorMain().equalsIgnoreCase("#FFFFFF")) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ViewUtil.getColorMain(requireActivity()));
                    } else {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireActivity(), R.color.primary_text));
                    }

                }
            });
            alertDialog.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public interface OnSavedAction {
        void onClose();

        void onEditCards();

        void onUseNewCard();

        void onAvail(CustomerCard card);
    }

}
