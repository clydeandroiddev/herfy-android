package com.herfy.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.herfy.utils.constants.Values;

@Entity(tableName = Values.TABLE_MODIFIER_ITEMS)
public class ModifierItemDb {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "item_group_modifier_id")
    public String itemGroupModifierId = "";

    @ColumnInfo(name = "application_id")
    public String applicationId = "";

    @ColumnInfo(name = "id")
    public String id = "";

    @ColumnInfo(name = "menu_item_id")
    public String menuItemId = "";

    @ColumnInfo(name = "modifier_group_id")
    public String modifierGroupId = "";

    @ColumnInfo(name = "data")
    public String data = "";

    @ColumnInfo(name = "created")
    public long created = 0L;

}