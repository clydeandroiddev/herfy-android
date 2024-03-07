package com.herfy.favorites

import com.skylinedynamics.solosdk.api.models.objects.Favorite

public interface FavoritesAction {

    fun onFavouriteAddToCart(favorite: Favorite)
}