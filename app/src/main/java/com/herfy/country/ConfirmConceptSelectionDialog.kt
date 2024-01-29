package com.herfy.country

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.herfy.R
import com.herfy.base.BaseDialogFragment
import com.herfy.data.constants.Text
import com.herfy.databinding.DialogConfirmConceptSelectionBinding
import com.herfy.util.CacheUtil

class ConfirmConceptSelectionDialog : BaseDialogFragment() {

    private var _onDialogAction : OnDialogAction? = null
    private var _binding: DialogConfirmConceptSelectionBinding? = null

    private val onDialogAction get() = _onDialogAction ?: throw Error("onDialogAction should not be null")
    private val binding get() = _binding ?: throw Error("binding should not be null")

    companion object {
        fun newInstance() : ConfirmConceptSelectionDialog {
            return ConfirmConceptSelectionDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        _binding = DialogConfirmConceptSelectionBinding.inflate(inflater, container, false)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvNo = view.findViewById<MaterialButton>(R.id.no)
        val tvYes = view.findViewById<MaterialButton>(R.id.yes)
        val tvMessage = view.findViewById<TextView>(R.id.message)


        tvMessage.text = CacheUtil.getInstance().getTranslations(
            Text.MULTY_COUNTRY_SWITCH_MESSAGE,
            "Your current items in the cart will be removed. Do you want to proceed switching to another store?"
        )
        tvNo.text = CacheUtil.getInstance().getTranslations(Text.NO_CAPS, "NO")
        tvYes.text = CacheUtil.getInstance().getTranslations(Text.YES_CAPS, "YES")

        tvYes.setOnClickListener {
            dismiss()
            onDialogAction.onYes()
        }
        tvNo.setOnClickListener {
            dismiss()
            onDialogAction.onBack()
        }

    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        _onDialogAction = when {
            context is OnDialogAction -> context
            parentFragment is OnDialogAction -> parentFragment as OnDialogAction
            else -> null
        }
    }


    override fun onDestroy() {
        _binding = null
        _onDialogAction = null
        super.onDestroy()

    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
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

    interface OnDialogAction {
        fun onYes()
        fun onBack()
    }
}