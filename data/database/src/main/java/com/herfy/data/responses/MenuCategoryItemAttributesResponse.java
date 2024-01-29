package com.herfy.data.responses;

import com.google.gson.annotations.SerializedName;
import com.herfy.data.entities.MenuCategoryEntity;
import com.herfy.data.entities.MenuItemEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MenuCategoryItemAttributesResponse implements Serializable {

    @SerializedName("category")
    private MenuCategoryEntity menuCategory;

    @SerializedName("items")
    private List<MenuItemEntity> menuItems = new ArrayList<>();

    public MenuCategoryItemAttributesResponse() {

    }

    public MenuCategoryEntity getMenuCategory() {
        return menuCategory;
    }

    public void setMenuCategory(MenuCategoryEntity menuCategory) {
        this.menuCategory = menuCategory;
    }

    public List<MenuItemEntity> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItemEntity> menuItems) {
        this.menuItems = menuItems;
    }

}
