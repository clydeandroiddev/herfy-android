package com.herfy.country

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.request.RequestOptions
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.herfy.BuildConfig
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.utils.constants.Text
import com.herfy.databinding.FragmentCountriesBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.skylinedynamics.solosdk.api.SoloApi
import com.skylinedynamics.solosdk.api.SoloAuth
import com.herfy.util.*
import com.herfy.util.hmac.HmacHeaderInterceptor
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.EnvUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.ApiHeaders

class CountriesFragment : BaseFragment(), CountriesContract.View,
    ConfirmConceptSelectionDialog.OnDialogAction {

    companion object {
        const val ARGS_KEY_FRAGMENT_RESULT = "fragmentResult"
        const val ARGS_KEY_FRAGMENT_RELOAD = "reload"
    }

    private lateinit var binding: FragmentCountriesBinding

    private var country: com.herfy.model.SupportedCountry? = null
    private lateinit var countriesPresenter: CountriesContract.Presenter
    private var countriesAdapter: CountriesAdapterNew? = null
    private var confirmConceptSelectionDialog: ConfirmConceptSelectionDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        countriesPresenter = CountriesPresenter(this)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveCountry()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCountriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        countriesPresenter.start()
        setupViews()
        setupTranslations()
    }

    override fun setPresenter(presenter: CountriesContract.Presenter) {}
    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
    }

    override fun setCartExpiry() {}
    override fun setupViews() {
        activity?.cast<MainActivity> { mainActivity ->
            mainActivity.setCloseButton {
                close()
            }
        }
        var countriesList: List<com.herfy.model.SupportedCountry> = ArrayList()
        activity?.let {
            countriesList = CacheUtil.getSupportedCountries(BuildConfig.APPLICATION_ID, it)
        }

        country = CacheUtil.getInstance().country
        if (country == null) {
            for (i in countriesList.indices) {
                if (CacheUtil.getInstance().application.attributes.key.equals(
                        countriesList[i].applicationKey,
                        ignoreCase = true
                    )
                ) {
                    country = countriesList[i]
                    CacheUtil.getInstance().country = country
                }
            }
        }
        if (countriesAdapter == null) {
            val size = ViewUtil.getScreenWidthHalf(activity)
            countriesAdapter = CountriesAdapterNew(size, countriesList, country)
            binding.countries!!.adapter = countriesAdapter
        }
        binding.confirm.setOnClickListener {
            var proceedChange = false
            country = countriesAdapter!!.selectedCountry
            CacheUtil.getInstance().country = country
            if (CacheUtil.getInstance().country != null) {
                if (!country?.applicationId.equals(
                        CacheUtil.getInstance().country.applicationId, ignoreCase = true
                    )
                ) {
                    proceedChange = true
                }
            } else {
                proceedChange =
                    !country?.applicationId.equals(BuildConfig.APPLICATION_ID, ignoreCase = true)
            }
            val isCacheExpired = countriesPresenter.isCacheExpired(country);
            if (proceedChange || isCacheExpired) {
                if (proceedChange && CacheUtil.getInstance().cartQuantity != 0) {
                    if (confirmConceptSelectionDialog != null) {
                        confirmConceptSelectionDialog!!.dismiss()
                    }
                    confirmConceptSelectionDialog = ConfirmConceptSelectionDialog.newInstance()
                    confirmConceptSelectionDialog?.show(
                        childFragmentManager,
                        ConfirmConceptSelectionDialog::class.java.simpleName
                    )
                } else {
                    onYes()
                }
            } else {
                close()
            }
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.confirm.text = CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM")
    }

    override fun proceedRestart() {
        saveCountry()
    }

    override fun preloadImage(imageUrl: String, requestOptions: RequestOptions) {}

    override fun onYes() {
        activity?.cast<BaseActivity> {
            it.showLoadingDialog()
        }
        countriesPresenter.setOldCountry(CacheUtil.getInstance().country)
        CacheUtil.getInstance().clearOrder()
        CacheUtil.getInstance().country = country
        CacheUtil.getInstance().applicationKey = country!!.applicationKey
        CacheUtil.getInstance().conceptKey = country!!.applicationConcept
        initSoloApi()
        SoloAuth.getInstance().setLanguage(
            LocaleUtil.getInstance().languageSymbol
        )
        SoloApi.getInstance().isDebug = BuildConfig.BUILD_TYPE == "debug"
        countriesPresenter!!.getApplication()
    }

    private fun initSoloApi() {
        activity?.let {
            // Create the Collector
            val chuckerCollector = ChuckerCollector(
                it,  // Toggles visibility of the push notification
                true,
                RetentionManager.Period.ONE_HOUR
            )
            SoloApi.init(
                it,
                EnvUtil.getEnvironmentUrl(),
                CacheUtil.getInstance().applicationKey
            )
            // Create the Interceptor
            val chuckerInterceptor = ChuckerInterceptor.Builder(it)
                .collector(chuckerCollector)
                .redactHeaders(
                    ApiHeaders.AUTHORIZATION_HEADER,
                    ApiHeaders.SOLO_APP_HEADER,
                    HmacHeaderInterceptor.HEADER_SIGNATURE
                )
                .alwaysReadResponseBody(true) // Controls Android shortcut creation. Available in SNAPSHOTS versions only at the moment
                .build()
            SoloApi.getInstance()
                .addInterceptor(chuckerInterceptor)

            SoloApi.getInstance().addInterceptor(HmacHeaderInterceptor())
        }

    }

    override fun onBack() {
        if (confirmConceptSelectionDialog != null) {
            confirmConceptSelectionDialog!!.dismiss()
        }
    }

    private fun saveCountry() {
        activity?.let {
            it.runOnUiThread {
                activity?.cast<CountriesActivity> { countriesActivity ->
                    showMain(countriesActivity)
                }
                activity?.cast<MainActivity> {
                    setFragmentResult(
                        ARGS_KEY_FRAGMENT_RESULT,
                        bundleOf(
                            ARGS_KEY_FRAGMENT_RELOAD to true
                        )
                    )
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun showMain(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.putExtra("home", true)
        intent.putExtra("forceLocation", activity.intent.extras?.getBoolean("forceLocation", false))
        intent.putExtra(ARGS_KEY_FRAGMENT_RELOAD, true)
        startActivity(intent)
        activity.finish()
    }

    private fun close() {
        activity?.cast<CountriesActivity> { countriesActivity ->
            showMain(countriesActivity)
        }
        activity?.cast<MainActivity> {
            it.runOnUiThread {
                setFragmentResult(
                    ARGS_KEY_FRAGMENT_RESULT,
                    bundleOf(
                        ARGS_KEY_FRAGMENT_RELOAD to false
                    )
                )
                findNavController().navigateUp()
            }

        }
    }

}