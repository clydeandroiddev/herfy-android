package com.herfy.menu.adapters;

import android.app.Activity;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.menu.MenuContract;
import com.skylinedynamics.solosdk.api.models.objects.ComponentModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.MenuItemComponents;
import com.skylinedynamics.solosdk.api.models.objects.ModifierGroup;
import com.skylinedynamics.solosdk.api.models.objects.ModifierItem;
import com.skylinedynamics.solosdk.api.models.objects.Translated;
import com.herfy.util.CacheUtil;
import com.herfy.util.LocaleUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CombosListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int promoWidth = 200;
    private ArrayList<MenuItemComponents> menuItemComponents;
    private Activity context;
    private MenuContract.Presenter presenter;
    public String sizeId;
    public MenuItem menuItemSelected;
    public HashMap<Integer, ComboItemListAdapter> componentsAdapters = new HashMap<>();
    public HashMap<Integer, ComboModifiersGroupListAdapter> componentModifierGroupAdapters = new HashMap<>();
    int selectedModifierIndex = 0;

    public CombosListAdapter(Activity context, ArrayList<MenuItemComponents> menuItemComponents, MenuContract.Presenter presenter, MenuItem menuItemSelected) {
        this.menuItemComponents = menuItemComponents;
        this.context = context;
        this.presenter = presenter;
        this.menuItemSelected = menuItemSelected;
        this.componentsAdapters = new HashMap<>();
    }

    @Override
    public int getItemCount() {
        return menuItemComponents.size();
    }

    public class ViewHolderData extends RecyclerView.ViewHolder {

        LinearLayout mainComboContainer;
        TextView sideLabel;
        RecyclerView itemsSideList;
        RecyclerView yourWayList;

        public ViewHolderData(View itemView) {
            super(itemView);
            mainComboContainer = itemView.findViewById(R.id.mainComboContainer);
            sideLabel = itemView.findViewById(R.id.sideLabel);
            itemsSideList = itemView.findViewById(R.id.itemsSideList);
            yourWayList = itemView.findViewById(R.id.yourWayList);
        }

    }

    private void setupDataView(final ViewHolderData holder, final int position) {
        final MenuItemComponents menuItem = menuItemComponents.get(position);
        if (!menuItem.items.isEmpty()) {
            holder.mainComboContainer.setVisibility(View.VISIBLE);
            holder.sideLabel.setText(translated(LocaleUtil.getInstance().getLanguage(), menuItem.name));
            ComboItemListAdapter comboItemListAdapter = new ComboItemListAdapter(context, menuItem.items, presenter, position);
            componentsAdapters.put(position, comboItemListAdapter);
            RecyclerView.ItemAnimator itemAnimator = holder.itemsSideList.getItemAnimator();
            if(itemAnimator != null) {
                ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
            }
            holder.itemsSideList.setAdapter(comboItemListAdapter);
            comboItemListAdapter.notifyDataSetChanged();
            setupYourWay(holder, menuItem, position);
        } else {
            holder.mainComboContainer.setVisibility(View.GONE);
        }
    }

    public void setupYourWay(final ViewHolderData holder, MenuItemComponents itemComponent, int position) {
        selectedModifierIndex = ifHasSelected(itemComponent.items);
        if (selectedModifierIndex != -1) {
            if (!itemComponent.items.get(selectedModifierIndex).modifierGroups.isEmpty()) {
                String optionsName = itemComponent.items.get(selectedModifierIndex).name.getValue(LocaleUtil.getInstance().getLanguage());
                ComboModifiersGroupListAdapter comboModifierGroupListAdapter = new ComboModifiersGroupListAdapter(context, itemComponent.items.get(selectedModifierIndex).modifierGroups, presenter, selectedModifierIndex, position, new ComboModifiersGroupListAdapter.ComboModifierGroupActionListener() {
                    @Override
                    public void onCheckChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem, int getIndex, ComboModifiersGroupListAdapter adapter) {
                        boolean proceed = true;
                        if (modifierGroup.getAttributes().getMaximum() == 1) {
                            if (modifierGroup.getAttributes().getMinimum() == 1) {
                                isSelected = true;
                            }
                            for (ModifierItem item : modifierGroup.getModifierItems()) {
                                item.getAttributes().setSelected(isSelected && item.getId().equals(modifierItem.getId()));
                            }
                        } else {
                            ModifierItem selectedModifierItem = null;
                            int selectedItems = 0;
                            for (ModifierItem item : modifierGroup.getModifierItems()) {
                                if (item.getAttributes().isSelected()) {
                                    selectedItems += item.getAttributes().getQuantity();
                                }
                                if (item.getId().equals(modifierItem.getId())) {
                                    selectedModifierItem = item;
                                }
                            }
                            if (isSelected && selectedItems >= modifierGroup.getAttributes().getMaximum()) {
                                proceed = false;
                                Toast.makeText(context, modifierGroup.getAttributes().getName() + " " + CacheUtil.getInstance().getTranslations(Text.MODIFIER_LIMIT_REACHED, "Options limit reached"), Toast.LENGTH_SHORT).show();
                            } else if(selectedModifierItem != null && selectedModifierItem.getId().equals(modifierItem.getId())) {
                                selectedModifierItem.getAttributes().setSelected(isSelected);
                                if (!isSelected) {
                                    selectedModifierItem.getAttributes().setQuantity(1);
                                }
                            }
                        }

                        if (proceed) {
                            presenter.updateComponentsFromYourWayQuantityCheck(modifierGroup, modifierItem, position, ifHasSelected(itemComponent.items), getIndex, isSelected);
                            //presenter.resetMenuItem();
                        }
                    }

                    @Override
                    public void onQuantityChanged(boolean add, ModifierGroup modifierGroup, ModifierItem modifierItem, int getIndex, ComboModifiersGroupListAdapter adapter) {
                        boolean proceed = true;
                        int quantity = modifierItem.getAttributes().getQuantity();
                        if (add) {
                            quantity = quantity + 1;
                            if (presenter.isAboveMax(modifierGroup, (presenter.modifierItemQuantityCount(modifierGroup.getModifierItems())))) {
                                proceed = false;
                                Toast.makeText(context, modifierGroup.getAttributes().getName() + " " + CacheUtil.getInstance().getTranslations(Text.MODIFIER_LIMIT_REACHED, "Options limit reached"), Toast.LENGTH_SHORT).show();
                            } else {
                                if (modifierItem.getAttributes().getMaximum() < (modifierItem.getAttributes().getQuantity() + 1)) {
                                    proceed = false;
                                    Toast.makeText(context, modifierGroup.getAttributes().getName() + " " + CacheUtil.getInstance().getTranslations(Text.MODIFIER_LIMIT_REACHED, "Options limit reached"), Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            quantity = quantity - 1;
                        }
                        if (proceed) {
                            presenter.updateComponentsFromYourWayQuantity(modifierItem, position, ifHasSelected(itemComponent.items), getIndex, add, quantity);
                            //presenter.resetMenuItem();
                        }
                    }
                }, optionsName);
                componentModifierGroupAdapters.put(position, comboModifierGroupListAdapter);
                RecyclerView.ItemAnimator itemAnimator = holder.yourWayList.getItemAnimator();
                if(itemAnimator != null) {
                    ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
                }
                holder.yourWayList.setAdapter(comboModifierGroupListAdapter);
                holder.yourWayList.setVisibility(View.VISIBLE);
                try {
                    Parcelable state = holder.yourWayList.getLayoutManager().onSaveInstanceState();
                    comboModifierGroupListAdapter.notifyDataSetChanged();
                    holder.yourWayList.getLayoutManager().onRestoreInstanceState(state);
                } catch (Exception e) {
                    comboModifierGroupListAdapter.notifyDataSetChanged();
                    e.printStackTrace();
                }
            } else {
                holder.yourWayList.setVisibility(View.GONE);
            }
        }

        /*ArrayList<MenuItemComponents> components1 = presenter.filterComponents(components);
        ComboMainYourWayListAdapter comboMainYourWayListAdapter = new ComboMainYourWayListAdapter(context, components1, presenter, menuItemSelected);
        if (!components1.isEmpty()) {
            holder.yourWayList.setAdapter(comboMainYourWayListAdapter);
            holder.yourWayList.setVisibility(View.VISIBLE);
            try {
                Parcelable state = holder.yourWayList.getLayoutManager().onSaveInstanceState();
                comboMainYourWayListAdapter.notifyDataSetChanged();
                holder.yourWayList.getLayoutManager().onRestoreInstanceState(state);
            } catch (Exception e) {
                comboMainYourWayListAdapter.notifyDataSetChanged();
                e.printStackTrace();
            }
        } else {
            holder.yourWayList.setVisibility(View.GONE);
            comboMainYourWayListAdapter.notifyDataSetChanged();
        }*/
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_combos, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        setupDataView((ViewHolderData) holder, position);
    }

    public String translated(String language, Translated name) {
        if (language.equalsIgnoreCase("en-us")) {
            return name.getEnglish();
        } else {
            return language.equalsIgnoreCase("fr") ? name.getFrench() : name.getArabic();
        }
    }

    private int ifHasSelected(ArrayList<ComponentModifierItem> items) {
        int size = items.size();
        for (int i = 0; i < size; i++) {
            if (items.get(i).selected) {
                return i;
            }
        }
        return -1;
    }

    public void updateComponents(int componentPosition, List<ComponentModifierItem> componentModifierItems, int previous, int selected) {
        selectedModifierIndex = selected;
        if(componentsAdapters.containsKey(componentPosition)) {
            ComboItemListAdapter adapter = componentsAdapters.get(componentPosition);
            adapter.updateComponents(componentModifierItems, previous, selected);
        }
        updateComponentModifierGroups(componentPosition, componentModifierItems, selected);
    }

    public void updateComponentModifierGroups(int componentPosition, List<ComponentModifierItem> componentModifierItems, int selected) {
        if(componentModifierGroupAdapters.containsKey(componentPosition)) {
            ComboModifiersGroupListAdapter adapter = componentModifierGroupAdapters.get(componentPosition);
            adapter.updateModifierGroups(componentModifierItems.get(selected).name.getValue(LocaleUtil.getInstance().getLanguage()), selected, componentModifierItems.get(selected).modifierGroups);
        }
    }

}