package com.herfy.language

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.herfy.base.BaseActivity
import com.herfy.changelanguage.R
import com.herfy.changelanguage.databinding.ActivityChangeLanguageBinding
import com.herfy.main.MainActivity
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.helper.AnimUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.skylinedynamics.solosdk.api.SoloAuth


class LanguageNewActivity : BaseActivity(), LanguageContract.View {

    private lateinit var languagePresenter: LanguageContract.Presenter

    private lateinit var binding: ActivityChangeLanguageBinding

    private var isArabicSelected = false

    private var isLanguageSelected = false

    private var player: Player? = null

    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L

    override fun setPresenter(presenter: LanguageContract.Presenter) {
        languagePresenter = presenter
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
            continueButton.text =
                CacheUtil.getInstance().getArabicTranslations(Text.CONTINUE_LABEL, "Continue")

            var languages = ArrayList<String>()
            if (CacheUtil.getInstance().concept != null) {
                languages = ArrayList(CacheUtil.getInstance().concept.languages)
            } else {
                languages.add(LocaleUtil.EN_US)
                languages.add(LocaleUtil.AR_SA)
            }

            for (lng in languages) {
                if (lng.equals(LocaleUtil.EN_US, ignoreCase = true)) {
                    chooseEnglish.visibility = View.VISIBLE
                } else if (lng.equals(LocaleUtil.AR_SA, ignoreCase = true)) {
                    chooseArabic.visibility = View.VISIBLE
                }
            }

            chooseYourEnglishLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.CHOOSE_LANGUAGE_ENGLISH, "Choose your language to continue")
            chooseYourArabicLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.CHOOSE_LANGUAGE_ARABIC, "اختر لغتك للمتابعة")

            chooseArabic.setOnClickListener {
                selectArabic()
            }

            chooseEnglish.setOnClickListener {
                selectEnglish()
            }

            continueButton.setOnClickListener {
                showLoadingDialog();
                if (isLanguageSelected) {
                    val eventAttributes = java.util.HashMap<String, String>()
                    if (isArabicSelected) {
                        LocaleUtil.getInstance().language = "ar"
                        SoloAuth.getInstance().setLanguage(LocaleUtil.AR_SA)
                        eventAttributes["ChangeLanguage"] = "AR"
                    } else {
                        LocaleUtil.getInstance().language = "en"
                        SoloAuth.getInstance().setLanguage(LocaleUtil.EN_US)
                        eventAttributes["ChangeLanguage"] = "EN"
                    }
                    logEvent("ChangeLanguage", eventAttributes, null, 0.0)
                    languagePresenter.changeLanguage()
                    dismissDialogs()
                    val intent = Intent(this@LanguageNewActivity, MainActivity::class.java)
                    intent.putExtra("home", true)
                    intent.putExtra("autoLogin", true)
                    startActivity(intent)
                    overridePendingTransition(AnimUtil.inF(), AnimUtil.inS())
                    finish()
                }
            }
        }
    }

    private fun setupButton() {
        if (isLanguageSelected) {
            binding.continueButton.alpha = 1f
        } else {
            binding.continueButton.alpha = 0.5f
        }
    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {

    }

    private fun updateLanguageSelection(
        isArabic: Boolean,
        chooseArabicDrawable: Int,
        chooseEnglishDrawable: Int,
        selectedArabicDrawable: Int,
        selectedEnglishDrawable: Int
    ) {
        isArabicSelected = isArabic
        isLanguageSelected = true
        binding.apply {
            chooseArabic.setBackgroundResource(chooseArabicDrawable)
            chooseEnglish.setBackgroundResource(chooseEnglishDrawable)
            selectedArabic.setImageResource(selectedArabicDrawable)
            selectedEnglish.setImageResource(selectedEnglishDrawable)
        }
        setupButton()
    }

    override fun selectArabic() {
        binding.continueButton.text =
            CacheUtil.getInstance().getArabicTranslations(Text.CONTINUE_LABEL, "Continue")
        updateLanguageSelection(
            true,
            R.drawable.bg_language_selected,
            R.drawable.bg_language_unselected,
            R.drawable.ic_language_circle_selected,
            R.drawable.ic_language_circle_unselected
        )
    }

    override fun selectEnglish() {
        binding.continueButton.text =
            CacheUtil.getInstance().getTranslations(Text.CONTINUE_LABEL, "Continue")
        updateLanguageSelection(
            false,
            R.drawable.bg_language_unselected,
            R.drawable.bg_language_selected,
            R.drawable.ic_language_circle_unselected,
            R.drawable.ic_language_circle_selected
        )
    }


    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer

                val mediaItem = MediaItem.Builder()
                    .setUri(Values.LANGUAGE_BACKGROUND_VIDEO_URL)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()

                exoPlayer.setMediaItems(listOf(mediaItem), mediaItemIndex, playbackPosition)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                exoPlayer.prepare()
                exoPlayer.play()
            }
    }

    private fun releasePlayer() {
        player?.let { player ->
            playbackPosition = player.currentPosition
            mediaItemIndex = player.currentMediaItemIndex
            playWhenReady = player.playWhenReady
            player.release()
        }
        player = null
    }

    public override fun onStart() {
        super.onStart()
        initializePlayer()
     }

    public override fun onResume() {
        super.onResume()

        if (player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onStop() {
        super.onStop()
        releasePlayer()
    }

}