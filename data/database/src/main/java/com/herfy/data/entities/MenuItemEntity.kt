package com.herfy.data.entities

import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.herfy.data.constants.Values
import com.skylinedynamics.solosdk.api.models.objects.LocationPrice
import com.skylinedynamics.solosdk.api.models.objects.MenuItem

@Entity(tableName = Values.TABLE_MENU_ITEMS_ENTITY)
data class MenuItemEntity(
    @PrimaryKey @ColumnInfo(name = "id") var id: String = "",
    @ColumnInfo(name = "application_id") var applicationId: String = "",
    @ColumnInfo(name = "menu_category_id") var menuCategoryId: String = "",
    @ColumnInfo(name = "name") @SerializedName("name") var name: TranslatedEntity? = null,
    @ColumnInfo(name = "description") @SerializedName("description") var description: TranslatedEntity? = null,
    @ColumnInfo(name = "image_uri") @SerializedName("image-uri") var image: String? = "",
    @ColumnInfo(name = "price") @SerializedName("price") var price: Double? = 0.0,
    @ColumnInfo(name = "prices") @SerializedName("prices") var prices: List<LocationPrice>? = emptyList(),
    @ColumnInfo(name = "display_order") @SerializedName("display-order") var displayOrder: Int? = 0,
    @ColumnInfo(name = "must_be_customized") @SerializedName("must-be-customized") var mustBeCustomized: Boolean = false,
    @ColumnInfo(name = "is_combo") @SerializedName("is-combo") var isCombo: Boolean = false,
    @ColumnInfo(name = "enabled") @SerializedName("enabled") var enabled: Int? = 0,
    @ColumnInfo(name = "disable_for_pickup") @SerializedName("disable-for-pickup") var disableForPickup: Int? = 0,
    @ColumnInfo(name = "disable_for_delivery") @SerializedName("disable-for-delivery") var disableForDelivery: Int? = 0,
    @ColumnInfo(name = "excluded_locations") @SerializedName("excluded-locations") var excludedLocations: List<String>? = emptyList(),
    @ColumnInfo(name = "out_of_stock_locations") @SerializedName("out-of-stock-locations") var outOfStockLocations: List<String>? = emptyList(),
    @ColumnInfo(name = "excluded_apps") @SerializedName("excluded-apps") var excludedApps: List<String>? = emptyList(),
    @ColumnInfo(name = "calories") @SerializedName("calories") var calories : String? = "",
    @ColumnInfo(name = "calorie_count") @SerializedName("calorie-count") var calorieCount : Int? = 0
) {

    companion object {

        @JvmStatic
        fun MenuItemEntity.toDomain(): MenuItem {
            val menuItem = MenuItem()
            menuItem.id = id
            menuItem.setName(name?.toDomain())
            menuItem.setDescription(description.toDomain())
            menuItem.image = image
            menuItem.price = price ?: 0.0
            menuItem.prices = prices
            menuItem.displayOrder = displayOrder ?: 0
            menuItem.isMustBeCustomized = mustBeCustomized
            menuItem.isCombo = isCombo
            menuItem.enabled = enabled ?: 0
            menuItem.disableForPickup = disableForPickup ?: 0
            menuItem.disableForDelivery = disableForDelivery ?: 0
            menuItem.excludedLocations = excludedLocations
            menuItem.outOfStockLocations = outOfStockLocations
            menuItem.excludedApps = excludedApps
            menuItem.calories = calories
            menuItem.calorieCount = calorieCount ?: 0
            return menuItem
        }

        @JvmStatic
        fun List<MenuItemEntity>.toDomain(): List<MenuItem> {
            return map { it.toDomain() }
        }

    }

}

