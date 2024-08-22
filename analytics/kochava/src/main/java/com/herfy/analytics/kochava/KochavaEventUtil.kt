package com.herfy.analytics.kochava

import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.Slide
import org.json.JSONArray
import org.json.JSONObject

class KochavaEventUtil {

    companion object {

        fun itemObject(menuItem: MenuItem): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("app_name", Values.HERFY_SOLO_ANDROID)
            jsonObject.put("item_id", menuItem.id)
            jsonObject.put("currency", "SAR")
            val user = AuthUtil.getInstance()
            if (user.isSignedIn) {
                jsonObject.put("user_id", user.customer.id)
            }
            jsonObject.put("item_name", menuItem.attributes.name)
            jsonObject.put("price", menuItem.attributes.price)
            jsonObject.put("quantity", menuItem.quantity)
            return jsonObject
        }

        fun itemAddToWishList(menuItem: MenuItem): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("app_name", Values.HERFY_SOLO_ANDROID)
            jsonObject.put("item_id", menuItem.id)
            jsonObject.put("currency", "SAR")
            val user = AuthUtil.getInstance()
            if (user.isSignedIn) {
                jsonObject.put("user_id", user.customer.id)
            }
            jsonObject.put("item_name", menuItem.attributes.name)
            jsonObject.put("price", menuItem.attributes.price)
            jsonObject.put("quantity", menuItem.quantity)
            return jsonObject
        }

        fun viewProductObject(menuItem: MenuItem, categoryName: String): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("app_name", Values.HERFY_SOLO_ANDROID)
            jsonObject.put("item_id", menuItem.id)
            jsonObject.put("currency", "SAR")
            val user = AuthUtil.getInstance()
            jsonObject.put("category_name", categoryName)
            jsonObject.put("item_name", menuItem.attributes.name)
            jsonObject.put("price", menuItem.attributes.price)
            return jsonObject
        }

        fun slideObject(slide: Slide): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("id", slide.id)
            jsonObject.put("link_type", slide.linkType)
            jsonObject.put("link_label", slide.label)
            jsonObject.put("title", slide.title)
            jsonObject.put("link_value", slide.linkValue)
            return jsonObject
        }

        fun customer(id: String): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("app_name", Values.HERFY_SOLO_ANDROID)
            jsonObject.put("customer_id", id)
            return jsonObject
        }

        fun purchaseObj(price: Double, items: List<MenuItem>): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("app_name", Values.HERFY_SOLO_ANDROID)

            val user = AuthUtil.getInstance()
            if (user.isSignedIn) {
                jsonObject.put("user_id", user.customer.id)
            }

            val couponCodeVal = when {
                CacheUtil.getInstance().couponCode != null -> CacheUtil.getInstance().couponCode
                else -> ""
            }
            jsonObject.put("coupon-code", couponCodeVal)
            jsonObject.put("currency", "SAR")
            jsonObject.put("price", price)
            val itemsArray = JSONArray()
            for (menuItem in items) {
                val itemObject = JSONObject()
                itemObject.put("item_name", menuItem.attributes.name)
                itemObject.put("price", menuItem.attributes.price)
                itemObject.put("quantity", menuItem.quantity)
                itemsArray.put(itemObject)
            }
            jsonObject.put("items", itemsArray)
            return jsonObject
        }

        fun globalObj(): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("app_name", Values.HERFY_SOLO_ANDROID)
            return jsonObject
        }

    }
}