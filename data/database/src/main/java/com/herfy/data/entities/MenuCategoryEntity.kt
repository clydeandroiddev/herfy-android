package com.herfy.data.entities

import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.herfy.data.constants.Values
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory

@Entity(tableName = Values.TABLE_MENU_CATEGORIES_ENTITY)
data class MenuCategoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") var id: String = "",
    @ColumnInfo(name = "application_id") var applicationId: String = "",
    @ColumnInfo(name = "name") @SerializedName("name") var name: TranslatedEntity? = null,
    @ColumnInfo(name = "image_uri") @SerializedName("image-uri") var image: String? = "",
    @ColumnInfo(name = "display_order") @SerializedName("display-order") var displayOrder: Int? = 0,
    @ColumnInfo(name = "enabled") @SerializedName("enabled") var enabled: Int? = 0,
    @ColumnInfo(name = "excluded_locations") @SerializedName("excluded-locations") var excludedLocations: List<String>? = emptyList(),
    @ColumnInfo(name = "excluded_apps") @SerializedName("excluded-apps") var excludedApps: List<String>? = emptyList()
) {

    companion object {

        @JvmStatic
        fun MenuCategoryEntity.toDomain(): MenuCategory {
            val menuCategory = MenuCategory()
            menuCategory.id = id
            menuCategory.setName(name?.toDomain())
            menuCategory.image = image
            menuCategory.displayOrder = displayOrder ?: 0
            menuCategory.enabled = enabled ?: 1
            menuCategory.setExcludedLocations(ArrayList(excludedLocations ?: emptyList()))
            menuCategory.setExcludedApps(ArrayList(excludedApps ?: emptyList()))
            return menuCategory
        }

        @JvmStatic
        fun List<MenuCategoryEntity>.toDomain(): List<MenuCategory> {
            return map { it.toDomain() }
        }

    }

}