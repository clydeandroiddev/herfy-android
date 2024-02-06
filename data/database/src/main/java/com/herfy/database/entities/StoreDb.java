package com.herfy.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.herfy.utils.constants.Values;

@Entity(tableName = Values.TABLE_STORES)
public class StoreDb {

    @NonNull
    @PrimaryKey
    public String id = "";

    @ColumnInfo(name = "application_id")
    public String applicationId = "";

    @ColumnInfo(name = "menu_item_id")
    public String menuItemId = "";

    @ColumnInfo(name = "data")
    public String data = "";

}