package com.herfy.history.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.herfy.R
import com.herfy.utils.constants.Text.*
import com.herfy.databinding.DialogCancelOrderBinding
import com.herfy.utils.helper.*

class CancelOrderDialog : DialogFragment() {

	private var _onAction : OnAction? = null
	private var _binding: DialogCancelOrderBinding? = null

	private val onAction get() = _onAction ?: throw Error("onAction should not be null")
	private val binding get() = _binding ?: throw Error("binding should not be null")

	companion object {
		fun newInstance() : CancelOrderDialog {
			return CancelOrderDialog()
		}
	}

//	override fun getTheme(): Int {
//		return R.style.FullScreenDialog
//	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
		_binding = DialogCancelOrderBinding.inflate(inflater, container, false)


		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		val cancelButton = view.findViewById<TextView>(R.id.btn_cancel)
		val backButton = view.findViewById<TextView>(R.id.btn_back)
		val title = view.findViewById<TextView>(R.id.title)
		val desc = view.findViewById<TextView>(R.id.label_desc)

		title.text = CacheUtil.getInstance().getTranslations(CANCEL_ORDER, "CANCEL ORDER")
		desc.text = CacheUtil.getInstance().getTranslations(ARE_YOU_SURE_YOU_WANT_TO_CANCEL, "Are you sure you want to cancel this order? This can't be undone.")
		backButton.text = CacheUtil.getInstance().getTranslations(BACK, "Back")
		cancelButton.text = CacheUtil.getInstance().getTranslations(YES_CANCEL, "Yes, cancel")

		cancelButton.setOnClickListener {
			dismiss()
			onAction.onCancel()
		}

		backButton.setOnClickListener {
			dismiss()
			onAction.onBack()
		}
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		_onAction = when {
			context is OnAction -> context
			parentFragment is OnAction -> parentFragment as OnAction
			else -> null
		}
	}

	override fun onStart() {
		super.onStart()
		val dialog = dialog
		if (dialog != null) {
			val width = ViewGroup.LayoutParams.MATCH_PARENT
			val height = ViewGroup.LayoutParams.WRAP_CONTENT
			dialog.window!!.setLayout(width, height)
			dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				val typedValue = TypedValue()
				dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
				dialog.window!!.statusBarColor = typedValue.data
			}
		}
	}

	override fun onDestroy() {
		_binding = null
		_onAction = null
		super.onDestroy()

	}


	interface OnAction {
		fun onCancel()
		fun onBack()
	}


}