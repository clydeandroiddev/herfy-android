package com.herfy.util.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import androidx.lifecycle.LiveData

class NetworkConnection(private val context: Context) : LiveData<Boolean>() {

	private var isRegistered = false
	private val connectivityManager: ConnectivityManager =
		context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

	private lateinit var networkConnectionCallback: ConnectivityManager.NetworkCallback

	override fun onActive() {
		super.onActive()

		updateNetworkConnection()
		if (isRegistered) {
			// Unregister the previous callback to avoid TooManyRequestsException
			unregisterCallback()
		}
		when {
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
				isRegistered = true
				connectivityManager.registerDefaultNetworkCallback(connectivityManagerCallback())
			}
			else -> {
				context.registerReceiver(
					networkReceiver,
					IntentFilter(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
				)

				isRegistered = true
			}
		}
	}
	private fun unregisterCallback() {
		try {
			connectivityManager.unregisterNetworkCallback(networkConnectionCallback)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		isRegistered = false
	}

	override fun onInactive() {
		super.onInactive()
//		if (isRegistered) {
//			isRegistered = false
//			connectivityManager.unregisterNetworkCallback(connectivityManagerCallback())
//		}
	}

	private fun connectivityManagerCallback(): ConnectivityManager.NetworkCallback {
		networkConnectionCallback = object : ConnectivityManager.NetworkCallback() {
			override fun onLost(network: Network) {
				super.onLost(network)
				postValue(false)
			}

			override fun onAvailable(network: Network) {
				super.onAvailable(network)
				postValue(true)
			}
		}
		return networkConnectionCallback
	}

	private fun updateNetworkConnection() {
		val activeNetworkConnection: NetworkInfo? = connectivityManager.activeNetworkInfo
		postValue(activeNetworkConnection?.isConnected == true)
	}

	private val networkReceiver= object : BroadcastReceiver() {
		override fun onReceive(p0: Context?, p1: Intent?) {
			updateNetworkConnection()
		}
	}

}