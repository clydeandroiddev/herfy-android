package com.herfy.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.herfy.utils.constants.Values;

@Entity(tableName = Values.TABLE_MENU_ITEMS)
public class MenuItemDb {

    @NonNull
    @PrimaryKey
    public String id = "";

    @ColumnInfo(name = "application_id")
    public String applicationId = "";

    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "menu_category_id")
    public String menuCategoryId = "";

    @ColumnInfo(name = "data")
    public String data = "";

    @ColumnInfo(name = "created")
    public long created = 0L;

}