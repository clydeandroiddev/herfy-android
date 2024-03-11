package com.herfy.favorites;

import android.widget.ImageView;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.skylinedynamics.solosdk.api.models.objects.Favorite;
import com.skylinedynamics.solosdk.api.models.objects.MenuItem;
import com.skylinedynamics.solosdk.api.models.objects.TimedEvent;

import java.util.ArrayList;

import javax.annotation.Nullable;

public interface FavoritesContract {

    interface View extends BaseView<Presenter> {

        void setIsLoading(boolean isLoading);

        void showFavorites(ArrayList<Favorite> favorites);

        void setFavorite(Favorite favorite, ArrayList<String> outOfStockLocationList, double discount, double specialPrice);

        void showMenuItem(String categoryId, String menuItemId);

        void addToCartFavorite(MenuItem menuItem, ImageView view);

        void showMessage(String message);

        void showError(String error);

        void showSearchFavorites(ArrayList<Favorite> favorites);

        void logFirebaseEvent(String event);

        @Nullable String getFromCache(String key);

    }

    interface Presenter extends BasePresenter {

        void searchFavorites(String text);

        void setToSearch(boolean isFromSearch);

        void getFavorites(int page, String orderTypeId);

        void onBindFavoriteViewAtPosition(int position, FavoriteViewHolder favoriteViewHolder);

        int getFavoritesCount();

        ArrayList<Favorite> getFavorites();

        void deleteFavorite(Favorite favorite);

        void addToCartFavorite(Favorite favorite, ImageView view);

        void getTimedEvent(Favorite favorite, MenuItem menuItem);

        void getConcept(Favorite favorite, TimedEvent timedEvent);

        void addCartItem(Favorite favorite);

        void getModifiers(Favorite favorite, String menuCategoryId, MenuItem menuItem);

        void getIngredients(Favorite favorite, String menuCategoryId, MenuItem menuItem);

        String getMenuCategory(String menuItemId);

        void removeMukafaaRedemption();

        void getFavouritesByOrderTypeId(String orderTypeId, String orderTypeName);

    }

}
