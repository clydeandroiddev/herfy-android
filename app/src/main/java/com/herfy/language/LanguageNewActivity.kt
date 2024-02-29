package com.herfy.language

import android.os.Bundle
import com.herfy.base.BaseActivity
import com.herfy.changelanguage.databinding.ActivityChangeLanguageBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil

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
        binding.apply {
            chooseYourEnglishLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.CHOOSE_LANGUAGE_ENGLISH, "Choose your language to continue")
            chooseYourEnglishLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.CHOOSE_LANGUAGE_ARABIC, "اختر لغتك للمتابعة")
            continueButton.text =
                CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue")
            chooseArabic.setOnClickListener {

            }
            chooseEnglish.setOnClickListener {

            }
        }
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