package com.herfy.utils.extensions

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.NavigationUtils

fun NavController.navigateToDeepLink(destination: String) {
    val request = NavDeepLinkRequest.Builder.fromUri(
        Uri.parse(
            "${Values.NAV_DEEP_LINK_BASE_URL}$destination"
        )
    ).build()
    NavigationUtils.navigateSafe(this, request)
}