package com.herfy.analytics.kochava

import com.herfy.utils.helper.AuthUtil
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.Slide
import org.json.JSONObject

class KochavaEventUtil {

    companion object {

        fun itemObject(menuItem: MenuItem, quantity: Int): JSONObject {
            val jsonObject = JSONObject()
            jsonObject.put("item_id", menuItem.id)
            jsonObject.put("currency", "SAR")
            val user = AuthUtil.getInstance()
            if (user.isSignedIn) {
                jsonObject.put("user_id", user.customer.id)
            }
            jsonObject.put("item_name", menuItem.attributes.name)
            jsonObject.put("price", menuItem.attributes.price)
            jsonObject.put("quantity", quantity)
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
    }
}