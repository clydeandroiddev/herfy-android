package com.herfy.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.herfy.data.constants.Values;

@Entity(tableName = Values.TABLE_ALLERGENS)
public class AllergenDb {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "item_allergen_id")
    public String itemAllergenId = "";

    @ColumnInfo(name = "application_id")
    public String applicationId = "";

    @ColumnInfo(name = "id")
    public String id = "";

    @ColumnInfo(name = "menu_item_id")
    public String menuItemId = "";

    @ColumnInfo(name = "data")
    public String data = "";

}