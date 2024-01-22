package com.herfy.database.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.herfy.constants.Values;

@Entity(tableName = Values.TABLE_FACILITIES)
public class FacilityEntity {

    @NonNull
    @PrimaryKey
    public String id = "";

    @ColumnInfo(name = "application_id")
    public String applicationId = "";

    @ColumnInfo(name = "data")
    public String data = "";

}