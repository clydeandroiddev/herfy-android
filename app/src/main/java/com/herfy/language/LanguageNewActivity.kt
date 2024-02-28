package com.herfy.language

import android.os.Bundle
import com.herfy.base.BaseActivity
import com.herfy.changelanguage.databinding.ActivityChangeLanguageBinding

class LanguageNewActivity : BaseActivity(), LanguageContract.View {

    private lateinit var languagePresenter: LanguageContract.Presenter

    private lateinit var binding: ActivityChangeLanguageBinding

    override fun setPresenter(presenter: LanguageContract.Presenter?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        languagePresenter = LanguagePresenter(this)
        languagePresenter.start()
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {

    }

    override fun setupViews() {

    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {

    }

    override fun selectArabic() {

    }

    override fun selectEnglish() {

    }

}