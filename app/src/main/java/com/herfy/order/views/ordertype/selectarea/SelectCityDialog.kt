package com.herfy.order.views.ordertype.selectarea

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.herfy.order.views.ordertype.selectarea.adapter.SelectCityAdapter
import com.herfy.ordertype.R
import com.herfy.ordertype.databinding.SelectCityDialogBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.area.Area
import com.skylinedynamics.solosdk.api.models.objects.area.City

class SelectCityDialog : DialogFragment() {

    private var onSubmit: OnSubmit? = null
    private lateinit var binding: SelectCityDialogBinding
    private lateinit var adapter: SelectCityAdapter
    private lateinit var selectAreaDialog: SelectAreaDialog
    private var selectedCity: City? = null
    private var originalList: List<City> = listOf()

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

    private fun filter(text: String) {
        binding.clearSearch.isVisible = text.isNotEmpty()
        val filteredList = originalList.filter { it.name.contains(text, ignoreCase = true) }
        binding.cityList.isVisible = filteredList.isNotEmpty()
        binding.emptyStateLayout.isVisible = filteredList.isEmpty()
        if (filteredList.isEmpty()) {
            binding.emptyState.text = CacheUtil.getInstance()
                .getTranslations(Text.SEARCH_EMPTY_RESULT, "No result found for \"(x)\"")
                .replace("(x)", text)
        } else {
            adapter.updateList(ArrayList(filteredList))
        }
    }

    private fun setupList() {
        originalList = CacheUtil.getInstance().cities
        adapter.updateList(ArrayList(originalList))
        binding.apply {
            searchEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    filter(searchEdit.text.toString())
                }
            })

            clearSearch.setOnClickListener {
                emptyStateLayout.isVisible = false
                searchEdit.setText("")
                binding.cityList.isVisible = true
                binding.clearSearch.isVisible = false
                adapter.updateList(ArrayList(originalList))
            }
        }
    }

    private fun checkButton() {
        binding.continueButton.alpha = 0.5f
        binding.continueButton.setOnClickListener(null)
        selectedCity?.let { city ->
            binding.continueButton.alpha = 1f
            binding.continueButton.setOnClickListener {
                selectCityDialog(city.areas)
            }
        }
    }

    private fun setupViews() {
        binding.apply {
            back.setOnClickListener {
                dismiss()
            }

            searchEdit.hint =
                CacheUtil.getInstance().getTranslations(Text.SEARCH_CITY, "Search city")

            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue")
            appTitle.text = CacheUtil.getInstance().getTranslations(Text.SELECT_CITY, "Select City")
            checkButton()

            adapter = SelectCityAdapter(
                ArrayList(), object : SelectCityAdapter.SelectCityListener {
                    override fun onSelectedCity(city: City, position: Int) {
                        selectedCity = city
                        adapter.updateSelectedId(city.id)
                        checkButton()
                    }
                }, ""
            )

            binding.cityList.adapter = adapter
        }
    }

    private fun dismissAreaDialog() {
        try {
            selectAreaDialog.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun selectCityDialog(areas: ArrayList<Area>) {
        dismissAreaDialog()
        selectAreaDialog = SelectAreaDialog.newInstance(areas, object : SelectAreaDialog.OnSubmit {
            override fun onSuccess(
                area: Area
            ) {
                onSubmit?.onSuccess(area)
                dismissAreaDialog()
            }

            override fun exit() {
                dismissAreaDialog()
            }
        })
        selectAreaDialog.show(
            requireActivity().supportFragmentManager, SelectAreaDialog::class.java.simpleName
        )
    }

    companion object {
        fun newInstance(listener: OnSubmit): SelectCityDialog {
            val selectCityDialogFragment = SelectCityDialog()
            selectCityDialogFragment.onSubmit = listener
            return selectCityDialogFragment
        }
    }
}
