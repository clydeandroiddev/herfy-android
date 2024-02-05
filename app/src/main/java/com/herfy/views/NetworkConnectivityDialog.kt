package com.herfy.views

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.herfy.R
import com.herfy.databinding.DialogNetworkConnectivityBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil

class NetworkConnectivityDialog : DialogFragment() {

	private var _binding: DialogNetworkConnectivityBinding? = null

	private val binding get() = _binding ?: throw Error("binding should not be null")

	companion object {
		fun newInstance(isConnected : Boolean) = NetworkConnectivityDialog().apply {
			arguments = Bundle().apply {
				putBoolean("isConnected",isConnected)
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
		_binding = DialogNetworkConnectivityBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val isConnected = arguments?.getBoolean("isConnected")
		if(isConnected == true) {
			binding.content.setBackgroundResource(R.drawable.network_connectivity_available)
			binding.title.text = CacheUtil.getInstance().getTranslations(Text.CONNECTION_RESTORED,"Connection Restored")
			binding.imageView.setImageDrawable(view.context.getDrawable(R.drawable.ic_connected))
		} else {
			binding.content.setBackgroundResource(R.drawable.network_connectivity_unavailable)
			binding.title.text = CacheUtil.getInstance().getTranslations(Text.NO_NETWORK_CONNECTION,"No network connection")
			binding.imageView.setImageDrawable(view.context.getDrawable(R.drawable.ic_no_connection))
		}
	}

	override fun onStart() {
		super.onStart()
		val dialog = dialog
		if (dialog != null) {
			val width = ViewGroup.LayoutParams.MATCH_PARENT
			val height = ViewGroup.LayoutParams.WRAP_CONTENT
			dialog.window!!.setLayout(width, height)


			getDialog()!!.window!!.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP)

			val window = getDialog()!!.window
			val params = window!!.attributes
			params.x = 300
			params.y = 100
			window.attributes = params

			dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
			dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
			dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				val typedValue = TypedValue()
				dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
				dialog.window!!.statusBarColor = typedValue.data
			}
		}
	}

	override fun onDestroy() {
		_binding = null
		super.onDestroy()

	}

}

