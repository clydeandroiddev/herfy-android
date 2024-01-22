package com.herfy.menu.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.herfy.R;
import com.herfy.constants.Text;
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
import java.util.List;

public class ComboMainYourWayListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public int promoWidth = 200;
    private List<MenuItemComponents> menuItemComponents;
    private Activity context;
    private MenuContract.Presenter presenter;
    private MenuItem mainMenuItem;

    public ComboMainYourWayListAdapter(Activity context, List<MenuItemComponents> menuItemComponents, MenuContract.Presenter presenter, MenuItem menuItem) {
        this.menuItemComponents = menuItemComponents;
        this.context = context;
        this.mainMenuItem = menuItem;
        this.presenter = presenter;
    }

    @Override
    public int getItemCount() {
        return menuItemComponents.size();
    }

    private int getYourWayModifier(ArrayList<ModifierGroup> modifierGroups) {
        for (int i = 0; i < modifierGroups.size(); i++) {
            if (modifierGroups.get(i).getAttributes().customFieldObjectMap.label.equalsIgnoreCase("yourway")) {
                return i;
            }
        }
        return -1;
    }

    private ArrayList<ModifierItem> filterNotIngredients(ArrayList<ModifierItem> items) {
        ArrayList<ModifierItem> ingredientItems = new ArrayList<>();
        int size = items.size();
        for (int i = 0; i < size; i++) {
            if (!items.get(i).getAttributes().customFieldObjectMap.isIngredient.equalsIgnoreCase("1")) {
                ingredientItems.add(items.get(i));
            }
        }
        return ingredientItems;
    }

    private String getCorrespondingIngredient(ArrayList<ModifierItem> items, String label) {
        int size = items.size();
        for (int i = 0; i < size; i++) {
            if (items.get(i).getAttributes().customFieldObjectMap.modifierGroup.equalsIgnoreCase(label)) {
                return items.get(i).getAttributes().getTranslatedName().getValue(LocaleUtil.getInstance().getLanguage());
            }
        }
        return null;
    }

    private ArrayList<ModifierItem> filterIngredients(ArrayList<ModifierItem> items) {
        ArrayList<ModifierItem> ingredientItems = new ArrayList<>();
        int size = items.size();
        for (int i = 0; i < size; i++) {
            if (items.get(i).getAttributes().customFieldObjectMap.isIngredient.equalsIgnoreCase("1") && !items.get(i).getAttributes().customFieldObjectMap.modifierGroup.isEmpty()) {
                String name = getCorrespondingIngredient(items, items.get(i).getAttributes().customFieldObjectMap.modifierGroup);
                items.get(i).getAttributes().yourWayStubLabel = name;
                ingredientItems.add(items.get(i));
            }
        }
        return ingredientItems;
    }

    @SuppressLint("SetTextI18n")
    private void setupDataView(final ViewHolderData holder, final int position) {
        final MenuItemComponents menuItem = menuItemComponents.get(position);
        if (!menuItem.items.isEmpty()) {
            int selectedModifierIndex = ifHasSelected(menuItem.items);
            holder.combosModifierList.setVisibility(View.GONE);
            holder.additionsLayout.setVisibility(View.GONE);
            if (selectedModifierIndex != -1) {
                if (!menuItem.items.get(selectedModifierIndex).modifierGroups.isEmpty()) {
                    String optionsName = menuItem.items.get(selectedModifierIndex).name.getValue(LocaleUtil.getInstance().getLanguage());
                    /*if (menuItem.name.getEnglish().toLowerCase().contains("sandwich") || menuItem.name.getFrench().toLowerCase().contains("sandwich")) {
                        optionsName = translated(LocaleUtil.getInstance().getLanguage(), menuItem.items.get(selectedModifierIndex).name);
                    }*/
                    holder.combosModifierList.setVisibility(View.VISIBLE);
                    ComboModifiersGroupListAdapter comboModifierGroupListAdapter = new ComboModifiersGroupListAdapter(context, menuItem.items.get(selectedModifierIndex).modifierGroups, presenter, selectedModifierIndex, position, new ComboModifiersGroupListAdapter.ComboModifierGroupActionListener() {
                        @Override
                        public void onCheckChanged(boolean isSelected, ModifierGroup modifierGroup, ModifierItem modifierItem, int getIndex, ComboModifiersGroupListAdapter adapter) {
                            boolean proceed = true;
                            if (isSelected) {
                                if (modifierGroup.getAttributes().getMaximum() != 1 && presenter.isAboveMax(modifierGroup, presenter.modifierItemQuantityCount(modifierGroup.getModifierItems()))) {
                                    proceed = false;
                                    Toast.makeText(context, modifierGroup.getAttributes().getName() + " " + CacheUtil.getInstance().getTranslations(Text.MODIFIER_LIMIT_REACHED, "Options limit reached"), Toast.LENGTH_SHORT).show();
                                    presenter.refresh();
                                }
                            } else if (modifierGroup.getAttributes().getMinimum() == 1) {
                                proceed = false;
                            }
                            if (proceed) {
                                presenter.updateComponentsFromYourWayQuantityCheck(modifierGroup, modifierItem, position, selectedModifierIndex, getIndex, isSelected);
                                presenter.resetMenuItem();
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
                                presenter.updateComponentsFromYourWayQuantity(modifierItem, position, selectedModifierIndex, getIndex, add, quantity);
                                presenter.resetMenuItem();
                            }
                        }
                    }, optionsName);
                    holder.combosModifierList.setAdapter(comboModifierGroupListAdapter);
                    comboModifierGroupListAdapter.notifyDataSetChanged();
                    //}
                } else {
                    holder.combosModifierList.setVisibility(View.GONE);
                }
            } else {
                holder.combosModifierList.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderData(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_yourway_list, parent, false));
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

    public class ViewHolderData extends RecyclerView.ViewHolder {

        RecyclerView combosModifierList;
        ConstraintLayout additionsLayout;
        TextView nameAddition;
        TextView selectAddition;
        TextView minimumMaximumAddition;
        RecyclerView recyclerViewAddition;

        public ViewHolderData(View itemView) {
            super(itemView);
            combosModifierList = itemView.findViewById(R.id.combosModifiersList);
            additionsLayout = itemView.findViewById(R.id.additionsLayout);
            nameAddition = itemView.findViewById(R.id.nameAddition);
            selectAddition = itemView.findViewById(R.id.selectAddition);
            minimumMaximumAddition = itemView.findViewById(R.id.minimum_maximum_addition);
            recyclerViewAddition = itemView.findViewById(R.id.recyclerViewAddition);
        }

    }


}
