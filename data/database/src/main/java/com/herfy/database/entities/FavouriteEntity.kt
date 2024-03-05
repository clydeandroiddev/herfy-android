package com.herfy.database.entities

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.herfy.utils.constants.Values

@Entity(tableName = Values.TABLE_FAVOURITES)
data class FavouriteEntity(
    @NonNull
    @PrimaryKey
    var id: String = "",
    @ColumnInfo(name = "application_id")
   var applicationId: String = "",
    @ColumnInfo(name = "data")
    var data: String = "",
    @Embedded(prefix = "menu_")
    var menuItem: MenuItemEntity,
    @ColumnInfo(name = "itemId")
    var itemId: String? = null,
    @ColumnInfo(name = "categoryId")
    var categoryId: String? = null,
) {
    companion object {

    }
}