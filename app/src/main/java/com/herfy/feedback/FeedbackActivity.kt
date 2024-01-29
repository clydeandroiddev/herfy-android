package com.herfy.feedback

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.nguyenhoanglam.imagepicker.helper.DeviceHelper
import com.nguyenhoanglam.imagepicker.model.ImagePickerConfig
import com.nguyenhoanglam.imagepicker.model.RootDirectory
import com.nguyenhoanglam.imagepicker.ui.imagepicker.registerImagePicker
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.data.constants.Text
import com.herfy.databinding.ActivityFeedbackBinding
import com.herfy.util.AuthUtil
import com.herfy.util.CacheUtil
import com.herfy.util.StringUtil
import com.herfy.util.ViewUtil
import java.io.File
import java.io.FileOutputStream

class FeedbackActivity : BaseActivity(), FeedbackContract.View {

    private lateinit var binding: ActivityFeedbackBinding

    private lateinit var back: MaterialButton
    private lateinit var title: TextView

    private lateinit var feedbackPresenter: FeedbackContract.Presenter

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if(isGranted) {
                binding.uploadImage.performClick()
            }
        }

    private val readPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if(isGranted) {
                showImagePicker()
            }
        }

    private var imageFile: File? = null

    private val imagePickerLauncher = registerImagePicker { images ->
        if(images.isNotEmpty()) {

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, images[0].uri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, images[0].uri)
            }

            imageFile = File.createTempFile("IMG_", ".jpg", this@FeedbackActivity.cacheDir)
            FileOutputStream(imageFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 30, it) }
            bitmap.recycle()

            Glide.with(this)
                .load(images[0].uri)
                .into(binding.image)
            binding.imgContainer.visibility = View.VISIBLE
            binding.imageEmptyContainer.visibility = View.GONE

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        feedbackPresenter = FeedbackPresenter(this)
    }

    override fun onResume() {
        super.onResume()
        feedbackPresenter.start()
    }

    override fun setPresenter(presenter: FeedbackContract.Presenter?) {
        presenter?.let {
            feedbackPresenter = it
        }
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        logEventAttributesMetric(
            eventName ?: "",
            eventAttributes ?: HashMap(), eventMetricName ?: "", eventMetricValue
        )
    }

    override fun setupViews() {

        val collapsing = binding.appBarLayout.findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar_layout)
        val toolbar = collapsing.findViewById<Toolbar>(R.id.toolbar)

        val cart = toolbar.findViewById<ConstraintLayout>(R.id.cart)
        cart.visibility = View.INVISIBLE

        back = toolbar.findViewById(R.id.back)
        back.visibility = View.VISIBLE
        back.setOnClickListener { onBackPressed() }

        title = toolbar.findViewById(R.id.title)

        val key = CacheUtil.getInstance().conceptKey
        val isBurgerizzr = key.equals("CQcFAwgLDA0", ignoreCase = true)

        if (AuthUtil.getInstance().isSignedIn) {
            val customer = AuthUtil.getInstance().customer
            binding.fullName.setText(customer.attributes.firstName + " " + customer.attributes.lastName)
            binding.username.setText(customer.attributes.email)
            //phoneNumber.setText(StringUtil.localize(customer.getAttributes().getMobile()));
            if (isBurgerizzr) {
                binding.phoneNumber.setText(
                    StringUtil.localize(
                        CacheUtil.getInstance().concept.dialingCode + customer.attributes.mobile.substring(
                            1
                        )
                    )
                )
            } else {
                binding.phoneNumber.setText(StringUtil.localize(customer.attributes.mobile))
            }
            binding.comment.requestFocus()
        } else {
            if (isBurgerizzr) {
                binding.username.setText(StringUtil.generateRandomEmail())
            }
            binding.fullName.requestFocus()
        }

        binding.usernameLabel.visibility = if (isBurgerizzr) View.GONE else View.VISIBLE
        binding.usernameContainer.visibility = if (isBurgerizzr) View.GONE else View.VISIBLE

        val phoneNumberTextWatcher: TextWatcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                binding.phoneNumber.removeTextChangedListener(this)
                binding.phoneNumber.setText(StringUtil.localize(binding.phoneNumber.getText().toString()))
                binding.phoneNumber.setSelection(editable.length)
                binding.phoneNumber.addTextChangedListener(this)
            }
        }

        binding.phoneNumber.addTextChangedListener(phoneNumberTextWatcher)

        binding.comment.setOnEditorActionListener(TextView.OnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                binding.submit.performClick()
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.comment.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })

        binding.delete.setOnClickListener(View.OnClickListener {
            imageFile = null
            binding.imgContainer.visibility = View.GONE
            binding.imageEmptyContainer.visibility = View.VISIBLE
        })

        binding.uploadImage.setOnClickListener(View.OnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                val readPermission =
                if (DeviceHelper.isMinSdk33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(
                        this,
                        readPermission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    readPermissionLauncher.launch(readPermission)
                } else {
                    showImagePicker()
                }
            }
        })

        binding.submit.setOnClickListener(View.OnClickListener {
            showLoadingDialog()
            feedbackPresenter.post(
                binding.fullName.text.toString().trim { it <= ' ' },
                binding.username.text.toString().trim { it <= ' ' },
                StringUtil.toEnglish(binding.phoneNumber.text.toString().trim { it <= ' ' }
                    .replace("+", "")),
                binding.comment.text.toString().trim { it <= ' ' },
                imageFile
            )
        })

    }

    override fun setupFonts() {

    }

    override fun setupTranslations() {
        back.text = CacheUtil.getInstance().getTranslations(Text.BACK, "Back")

        title.setText(CacheUtil.getInstance().getTranslations(Text.FEEDBACK))
        binding.message.setText(
            CacheUtil.getInstance().getTranslations(
                Text.FEEDBACK_MESSAGE,
                "We value your opinion, so please tell us about your experience with our app or about your meal."
            )
        )
        binding.addImageLabel.text =
            CacheUtil.getInstance().getTranslations(Text.ADD_AN_IMAGE_CAPS, "ADD AN IMAGE")
        binding.uploadImage.text = CacheUtil.getInstance().getTranslations(Text.UPLOAD_IMAGE_CAPS, "UPLOAD IMAGE")
        binding.fullNameLabel.text = CacheUtil.getInstance().getTranslations(Text.FULL_NAME)
        binding.usernameLabel.text = CacheUtil.getInstance().getTranslations(Text.EMAIL_ADDRESS)
        binding.phoneNumberLabel.text = CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER)
        binding.commentLabel.text = CacheUtil.getInstance().getTranslations(Text.YOUR_COMMENT)
        binding.comment.hint = CacheUtil.getInstance().getTranslations(
            Text.TELL_US_ABOUT_YOUR_EXPERIENCE_HERE,
            "Tell us about your experience here"
        )
        binding.submit.text = CacheUtil.getInstance().getTranslations(Text.SEND_FEEDBACK_CAPS, "SEND FEEDBACK")
    }

    override fun showErrorFullName(error: String?) {
        if (error!!.isEmpty()) {
            binding.fullNameError.visibility = View.GONE
        } else {
            binding.fullNameError.text = error
            binding.fullNameError.visibility = View.VISIBLE
            dismissDialogs()
        }
    }

    override fun showErrorEmailAddress(error: String?) {
        if (error!!.isEmpty()) {
            binding.usernameError.visibility = View.GONE
        } else {
            binding.usernameError.text = error
            binding.usernameError.visibility = View.VISIBLE
            dismissDialogs()
        }
    }

    override fun showErrorPhoneNumber(error: String?) {
        if (error!!.isEmpty()) {
            binding.phoneNumberError.visibility = View.GONE
        } else {
            binding.phoneNumberError.text = error
            binding.phoneNumberError.visibility = View.VISIBLE
            dismissDialogs()
        }
    }

    override fun showErrorComment(error: String?) {
        if (error!!.isEmpty()) {
            binding.commentError.visibility = View.GONE
        } else {
            binding.commentError.text = error
            binding.commentError.visibility = View.VISIBLE
            dismissDialogs()
        }
    }

    override fun showMessage(message: String?) {
        dismissDialogs()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showError(error: String?) {
        showAlertDialog("", error)
    }

    override fun showSuccess(success: String?) {
        showAlertDialog(
            "", success, CacheUtil.getInstance().getTranslations(Text.OK)
        ) { dialog, which ->
            onBackPressed()
            finish()
        }
    }


    private fun showImagePicker() {
        val white = StringUtil.colorIntToString(
            ContextCompat.getColor(
                this@FeedbackActivity,
                R.color.white
            )
        )
        val primary =
            StringUtil.colorIntToString(ViewUtil.getColorMain(this@FeedbackActivity))

        /*val config = ImagePickerConfig(
            isFolderMode = false,
            folderTitle = CacheUtil.getInstance().getTranslations(Text.ALBUMS, "Albums"),
            imageTitle = CacheUtil.getInstance().getTranslations(Text.PHOTOS, "Photos"),
            uploadButtonText = CacheUtil.getInstance().getTranslations(Text.UPLOAD_SELECTED_IMAGE, "UPLOAD SELECTED IMAGE"),
            backButtonText = CacheUtil.getInstance().getTranslations(Text.BACK, "Back"),
            subDirectory = getString(R.string.app_name),
            primaryColor = primary,
            statusBarColor = primary,
            toolbarColor = white,
            backgroundColor = white,
            progressIndicatorColor = primary,
            selectedIndicatorColor = primary,
            isShowNumberIndicator = false,
            isMultipleMode = true,
            maxSize = 1,
            isShowCamera = true
        )*/
        val config = ImagePickerConfig(
            isFolderMode = false,
            uploadButtonText = CacheUtil.getInstance().getTranslations(Text.UPLOAD_SELECTED_IMAGE, "UPLOAD SELECTED IMAGE"),
            backButtonText = CacheUtil.getInstance().getTranslations(Text.BACK, "Back"),
            primaryColor = primary,
            statusBarColor = primary,
            toolbarColor = white,
            isLightStatusBar = false,
            backgroundColor = white,
            progressIndicatorColor = primary,
            selectedIndicatorColor = primary,
            isShowNumberIndicator = false,
            isCameraOnly = false,
            isMultipleMode = true,
            isAlwaysShowDoneButton = false,
            doneTitle = "DONE",
            folderTitle = CacheUtil.getInstance().getTranslations(Text.ALBUMS, "Albums"),
            imageTitle = CacheUtil.getInstance().getTranslations(Text.PHOTOS, "Photos"),
            isShowCamera = true,
            rootDirectory = RootDirectory.DCIM,
            subDirectory = getString(R.string.app_name),
            maxSize = 1
        )
        imagePickerLauncher.launch(config)
    }

}