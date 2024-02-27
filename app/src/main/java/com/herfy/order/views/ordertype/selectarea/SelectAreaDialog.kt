package com.herfy.order.views.ordertype.selectarea

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.herfy.order.views.ordertype.selectarea.adapter.SelectAreaAdapter
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.SelectCityDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.area.Area

class SelectAreaDialog : DialogFragment() {

    private var onSubmit: OnSubmit? = null
    private var areas: ArrayList<Area> = ArrayList()
    private lateinit var binding: SelectCityDialogBinding
    private lateinit var adapter: SelectAreaAdapter
    private var selectedArea: Area? = null

    interface OnSubmit {
        fun onSuccess(
            area: Area
        )

        fun exit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SelectCityDialogBinding.inflate(layoutInflater)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme_White)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
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
            val typedValue = TypedValue()
            dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog.window!!.statusBarColor = typedValue.data
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupList()
    }

    private fun setupList() {
        adapter.updateList(areas)
    }

    private fun checkButton() {
        binding.continueButton.alpha = 0.5f
        binding.continueButton.setOnClickListener(null)
        selectedArea?.let { area ->
            binding.continueButton.alpha = 1f
            binding.continueButton.setOnClickListener {
                onSubmit?.onSuccess(area)
            }
        }
    }

    private fun setupViews() {
        binding.apply {
            back.setOnClickListener {
                dismiss()
            }
            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue")
            appTitle.text = CacheUtil.getInstance().getTranslations(Text.SELECT_AREA, "Select Area")
            checkButton()
            adapter = SelectAreaAdapter(
                ArrayList(), object : SelectAreaAdapter.SelectCityListener {
                    override fun onSelectedCity(city: Area, position: Int) {
                        selectedArea = city
                        adapter.updateSelectedId(city.id)
                        checkButton()
                    }
                }, ""
            )
            binding.cityList.adapter = adapter
        }
    }

    companion object {
        fun newInstance(areas: ArrayList<Area>, listener: OnSubmit): SelectAreaDialog {
            val selectCityDialogFragment = SelectAreaDialog()
            selectCityDialogFragment.onSubmit = listener
            selectCityDialogFragment.areas = areas
            return selectCityDialogFragment
        }
    }
}
