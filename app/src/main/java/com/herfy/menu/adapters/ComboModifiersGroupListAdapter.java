package com.herfy.menu.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.utils.constants.Text;
import com.herfy.menu.MenuContract;
import com.herfy.menu.viewholders.ModifierItemViewHolderNew;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.VisibilityConfig;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ComboModifiersGroupListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int promoWidth = 200;
    private List<ModifierGroup> modifierGroups;
    private Activity context;
    private MenuContract.Presenter presenter;
    public String sizeId;
    private int itemModGroupPosition;
    private int comboPosition;
    private ComboModifierGroupActionListener listener;
    private ComboModifiersGroupListAdapter comboModifierGroupListAdapter;
    private String optionsName;

    public ComboModifiersGroupListAdapter(Activity context, List<ModifierGroup> modifierGroups, MenuContract.Presenter presenter, int itemModGroupPosition, int comboPosition, ComboModifierGroupActionListener listener) {
        this.modifierGroups = modifierGroups;
        this.context = context;
        this.itemModGroupPosition = itemModGroupPosition;
        this.comboPosition = comboPosition;
        this.presenter = presenter;
        this.listener = listener;
        this.optionsName = null;
        comboModifierGroupListAdapter = this;
    }

    public ComboModifiersGroupListAdapter(Activity context, List<ModifierGroup> menuItemComponents, MenuContract.Presenter presenter, int itemModGroupPosition, int comboPosition, ComboModifierGroupActionListener listener, String optionsName) {
        this.modifierGroups = menuItemComponents;
        this.context = context;
        this.itemModGroupPosition = itemModGroupPosition;
        this.comboPosition = comboPosition;
        this.presenter = presenter;
        this.listener = listener;
        this.optionsName = optionsName;
        comboModifierGroupListAdapter = this;
    }

    @Override
    public int getItemCount() {
        return modifierGroups.size();
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.minimum_maximum)
        TextView minimumMaximum;
        @BindView(R.id.select)
        TextView select;
        @BindView(R.id.recycler_view)
        RecyclerView recyclerView;

        public ViewHolderData(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        final ModifierGroup modifierGroup = modifierGroups.get(position);
        boolean visible = false;
        VisibilityConfig visibilityConfig = modifierGroup.getAttributes().getVisibilityConfig();
        if (visibilityConfig != null) {
            if (visibilityConfig.getInitialValue() == null) {
                visible = true;
                holder.name.setVisibility(View.VISIBLE);
                holder.recyclerView.setVisibility(View.VISIBLE);
            } else {
                if (visibilityConfig.getInitialValue() != null && !visibilityConfig.getInitialValue().equalsIgnoreCase("hidden")) {
                    visible = true;
                    holder.name.setVisibility(View.VISIBLE);
                    holder.recyclerView.setVisibility(View.VISIBLE);
                } else {
                    holder.name.setVisibility(View.GONE);
                    holder.recyclerView.setVisibility(View.GONE);
                }
            }
        } else {
            visible = true;
            holder.name.setVisibility(View.VISIBLE);
            holder.recyclerView.setVisibility(View.VISIBLE);
        }

        holder.name.setText(modifierGroup.getAttributes().getName());

        if (visible) {
            int minimum = modifierGroup.getAttributes().getMinimum();
            int maximum = modifierGroup.getAttributes().getMaximum();
            if (minimum > 0) {
                if(LocaleUtil.getInstance().isArabic()){
                    holder.minimumMaximum.setText("(x)\\(y)".replace("(x)", StringUtil.localize(String.valueOf(presenter.getModifierGroupSelectedModifierItemsCount(modifierGroup)))).replace("(y)", StringUtil.localize(String.valueOf(maximum))));
                }else {
                    holder.minimumMaximum.setText("(x)/(y)".replace("(x)", StringUtil.localize(String.valueOf(presenter.getModifierGroupSelectedModifierItemsCount(modifierGroup)))).replace("(y)", StringUtil.localize(String.valueOf(maximum))));
                }
                holder.minimumMaximum.setVisibility(View.VISIBLE);
                if (maximum > minimum) {
                    holder.select.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_ATLEAST, "(Select at least (x))").replace("(x)", StringUtil.localize(String.valueOf(minimum))));
                    holder.select.setVisibility(View.VISIBLE);
                } else if (maximum == minimum) {
                    holder.select.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_X, "(Select (x))").replace("(x)", StringUtil.localize(String.valueOf(maximum))));
                    holder.select.setVisibility(View.VISIBLE);
                } else {
                    holder.select.setVisibility(View.INVISIBLE);
                }
            } else if (minimum == 0) {
                holder.minimumMaximum.setVisibility(View.GONE);
                if (maximum > 0) {
                    holder.select.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_UP_TO_X, "(Select up to (x))").replace("(x)", StringUtil.localize(String.valueOf(maximum))));
                    holder.select.setVisibility(View.VISIBLE);
                } else {
                    holder.select.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.select.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.minimumMaximum.setVisibility(View.GONE);
            holder.select.setVisibility(View.GONE);
        }

        holder.recyclerView.setLayoutManager(new LinearLayoutManager(context));

        ModifierItemsRecyclerViewAdapterNew modifierItemsRecyclerViewAdapter = new ModifierItemsRecyclerViewAdapterNew(context, presenter, modifierGroup);
        modifierItemsRecyclerViewAdapter.setOnAction(new ModifierItemViewHolderNew.OnAction() {
            @Override
            public void onCheckedChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem, int posTrue) {
                listener.onCheckChanged(isSelected, modifierGroup, modifierItem, position, comboModifierGroupListAdapter);
            }

            @Override
            public void onQuantityChanged(boolean add, ModifierGroup modifierGroup, ModifierItem modifierItem, int posTrue) {
                listener.onQuantityChanged(add, modifierGroup, modifierItem, position, comboModifierGroupListAdapter);

            }
        });
        holder.recyclerView.setAdapter(modifierItemsRecyclerViewAdapter);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_new_modifier_items, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView((ViewHolderData) holder, position);
    }

    public interface ComboModifierGroupActionListener {
        void onCheckChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem, int getIndex, ComboModifiersGroupListAdapter adapter);

        void onQuantityChanged(boolean add, ModifierGroup modifierGroup, ModifierItem modifierItem, int getIndex, ComboModifiersGroupListAdapter adapter);
    }

    public void updateModifierGroups(String optionsName, int itemModGroupPosition, List<ModifierGroup> groups) {
        this.optionsName = optionsName;
        this.itemModGroupPosition = itemModGroupPosition;
        this.modifierGroups = groups;
        notifyDataSetChanged();
    }

}