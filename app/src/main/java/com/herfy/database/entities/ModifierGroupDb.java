package com.herfy.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.herfy.constants.Values;

@Entity(tableName = Values.TABLE_MODIFIER_GROUPS)
public class ModifierGroupDb {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "item_group_id")
    public String itemGroupId = "";

    @ColumnInfo(name = "application_id")
    public String applicationId = "";

    @ColumnInfo(name = "id")
    public String id = "";

    @ColumnInfo(name = "menu_item_id")
    public String menuItemId = "";

    @ColumnInfo(name = "type")
    public int type = 0;

    @ColumnInfo(name = "data")
    public String data = "";

}