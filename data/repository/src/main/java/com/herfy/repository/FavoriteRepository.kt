package com.herfy.repository

import com.herfy.analytics.kochava.KochavaTrackerUtil
import com.herfy.utils.analytics.MixpanelUtil
import com.herfy.utils.analytics.MixpanelUtil.Companion.trackEvent
import com.herfy.utils.constants.Text
import com.herfy.utils.handler.ErrorHandler
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.kochava.tracker.events.EventType
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.requestbodies.AddFavoriteItemBody
import com.skylinedynamics.solosdk.util.AppLogger
import javax.inject.Inject


interface FavoriteRepository {

    suspend fun addToFavorite(menuItem: MenuItem, result:(String) -> Unit)

    suspend fun removeFromFavorite(menuItem: MenuItem, result:(String) -> Unit)

}

class FavoriteRepositoryImpl @Inject constructor(
) : FavoriteRepository {

    override suspend fun addToFavorite(menuItem: MenuItem, result:(String) -> Unit) {
        CustomersApiCall.getInstance().addItemToFavorite(
            AuthUtil.getInstance().customer.id,
            AddFavoriteItemBody.getCdn(LocaleUtil.getInstance().language, menuItem),
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    trackEvent(MixpanelUtil.EVENT_FAVORITE_ITEM)
                    KochavaTrackerUtil.trackEvent(EventType.ADD_TO_WISH_LIST)
                    val favorites = CacheUtil.getInstance().favorites
                    favorites.add(menuItem.id)
                    CacheUtil.getInstance().favorites = favorites
                    result(
                        CacheUtil.getInstance().getTranslations(Text.ADDED_TO_FAVORITES)
                    )
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    result(
                        ErrorHandler.getErrorMessage(
                            o, CacheUtil.getInstance().getTranslations(
                                Text.ERROR_ADD_TO_FAVORITES
                            ), null
                        )
                    )
                }
            })
    }

    override suspend fun removeFromFavorite(menuItem: MenuItem, result:(String) -> Unit) {
        CustomersApiCall.getInstance().deleteItemFromFavorites(
            AuthUtil.getInstance().customer.id,
            menuItem.id,
            object : ApiRequestListener {
                override fun onSuccess(o: Any) {
                    val favorites = CacheUtil.getInstance().favorites
                    favorites.remove(menuItem.id)
                    CacheUtil.getInstance().favorites = favorites
                    result(
                        CacheUtil.getInstance().getTranslations(Text.REMOVED_FROM_FAVORITES)
                    )
                }

                override fun onError(o: Any) {
                    AppLogger.printRetrofitError(o)
                    result(
                        ErrorHandler.getErrorMessage(
                            o, CacheUtil.getInstance().getTranslations(
                                Text.ERROR_REMOVE_FROM_FAVORITES
                            ), null
                        )
                    )
                }
            })
    }

}