package com.herfy.loyalty

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.herfy.base.BaseActivity
import com.herfy.databinding.ActivityLoyaltyIframeBinding
import com.herfy.util.AppLogger
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.ApiRequestListener
import com.skylinedynamics.solosdk.api.calls.RewardApiCall
import org.json.JSONObject

class LoyaltyIFrameActivity : BaseActivity() {

    private lateinit var binding: ActivityLoyaltyIframeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoyaltyIframeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbarColor = ContextCompat.getColor(this, com.herfy.commonstyles.R.color.toolbar_color)
        setStatusBarColor(toolbarColor)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        binding.title.text = CacheUtil.getInstance().getTranslations(Text.REWARDS_LABEL)

        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.webView.webViewClient = object : WebViewClient() {}
        binding.webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true

        if (AuthUtil.getInstance().isSignedIn) {
            showLoadingDialog()
            val customer = AuthUtil.getInstance().customer
            RewardApiCall.getInstance().getLoyaltyIframeToken(customer.id, object : ApiRequestListener {
                override fun onSuccess(obj: Any?) {
                    dismissDialogs()
                    try {
                        val jsonObject = JSONObject(obj.toString())
                        val data = jsonObject.optJSONObject("data")
                        if (data != null) {
                            val fetchedToken = data.getString("token")
                            CacheUtil.getInstance().loyaltyIframeToken = fetchedToken
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    loadWebViewUrl()
                }

                override fun onError(error: Any?) {
                    AppLogger.printRetrofitError(error)
                    dismissDialogs()
                    loadWebViewUrl()
                }
            })
        } else {
            loadWebViewUrl()
        }
    }

    private fun loadWebViewUrl() {
        val iframeName = intent.getStringExtra("iframe_name") ?: Values.IFRAME_NAME_HOME
        val intentUrl = intent.getStringExtra("url")
        val forceRemoteConfigUrl = intent.getBooleanExtra("force_remote_config_url", false)
        val remoteConfigUrl = getIframeUrlFromRemoteConfig(iframeName)
            .ifEmpty { CacheUtil.getInstance().getCapillaryIframeUrl(iframeName) }
        val shouldUsePopupActionUrl = !forceRemoteConfigUrl &&
            (iframeName == Values.IFRAME_NAME_HOME || iframeName == Values.IFRAME_NAME_ORDER_DETAILS)
        val capillaryPopupUrl = if (shouldUsePopupActionUrl) {
            CacheUtil.getInstance().capillaryPopup?.actionValue
        } else {
            null
        }
        val token = CacheUtil.getInstance().loyaltyIframeToken
        val mobile = AuthUtil.getInstance().customer?.attributes?.mobile ?: ""

        val baseUrl = if (!intentUrl.isNullOrEmpty()) {
            intentUrl
        } else if (!capillaryPopupUrl.isNullOrEmpty()) {
            capillaryPopupUrl
        } else if (!remoteConfigUrl.isNullOrEmpty()) {
            remoteConfigUrl
        } else {
            "https://demoapps.capillarytech.com/herfy-app"
        }

        val normalizedBaseUrl = ensureTrailingSlash(ensureHttpsUrl(baseUrl.trim()))

        val url = if (!normalizedBaseUrl.contains("token=")) {
            val separator = if (normalizedBaseUrl.contains("?")) "&" else "?"
            "${normalizedBaseUrl}${separator}token=${token}&mobile=${mobile}&lang=${LocaleUtil.getInstance().languageSymbol}"
        } else {
            normalizedBaseUrl
        }
        binding.webView.loadUrl(url)
    }

    private fun getIframeUrlFromRemoteConfig(iframeName: String): String {
        return try {
            val configJson = CacheUtil.getInstance().getCapillaryConfig()
            if (configJson.isEmpty()) {
                return ""
            }

            val iframes = JSONObject(configJson).optJSONArray("capillary_iframes") ?: return ""
            for (i in 0 until iframes.length()) {
                val iframe = iframes.optJSONObject(i) ?: continue
                if (iframeName.equals(iframe.optString("name"), ignoreCase = true)) {
                    return iframe.optString("url", "")
                }
            }
            ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }


    private fun ensureHttpsUrl(url: String): String {
        val value = url.trim()
        return value.replaceFirst(Regex("(?i)^http://"), "https://")
    }

    private fun ensureTrailingSlash(url: String): String {
        val parts = url.split("?", limit = 2)
        val base = parts[0].trimEnd('/') + "/"
        return if (parts.size > 1) {
            "$base?${parts[1]}"
        } else {
            base
        }
    }

}