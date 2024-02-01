package com.herfy.productpagedetails.ui.viewholder.util.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ItemAttributesUIModel(
    val createdAt: String,
    val updatedAt: String,
    val code: String,
    val categoryId: Int,
    val displayOrder: Int,
    val name: String,
    val description: String,
    val imageUri: String,
    val price: Int,
    val listPrice: Int,
    val calorieCount: Int,
    val calories: String?,
    val personCountPerServing: Int,
    val favorite: Boolean,
    val enabled: Int,
    val disableForPickup: Int,
    val disableForDelivery: Int,
    val isCustomizable: Boolean,
    val isCombo: Boolean,
    val isFavouritable: Boolean,
    val isReorderable: Boolean,
    val isPromo: Int,
    val isFeatured: Boolean,
    val isDiscountAllowed: Boolean,
    val preparationTime: Int,
    val itemsSold: Int?,
    val isHiddenFromMenu: Boolean,
    val excludedApps: List<String>,
    val prices: List<LocalPriceUIModel>,
    val hasTimedEvent: Boolean,
    val favoriteId: Int?,
    val mustBeCustomized: Boolean
): Parcelable {
}