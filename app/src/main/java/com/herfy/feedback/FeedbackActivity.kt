package com.herfy.feedback

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.herfy.base.BaseActivity
import com.herfy.utils.constants.Text
import com.herfy.databinding.ActivityFeedbackBinding
import com.herfy.util.StringUtil
import java.io.File
import java.io.FileOutputStream
import com.herfy.utils.helper.*

class FeedbackActivity : BaseActivity(), FeedbackContract.View {

    private lateinit var binding: ActivityFeedbackBinding

    private lateinit var feedbackPresenter: FeedbackContract.Presenter

    private var imageFile: File? = null

    private var pickerDialog: PickerDialog? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val fileUri = result.data?.data
                fileUri?.let {
                    processImage(it)
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ImagePicker.getError(result.data), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    private val standardImagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    processImage(it)
                }
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

        binding.toolbar.setNavButtonAction {
            onBackPressed()
        }

        if (AuthUtil.getInstance().isSignedIn) {
            val customer = AuthUtil.getInstance().customer
            binding.fullName.setText(customer.attributes.firstName + " " + customer.attributes.lastName)
            binding.username.setText(customer.attributes.email)
            binding.phoneNumber.setText(StringUtil.localize(customer.attributes.mobile))
            binding.comment.requestFocus()
        } else {
            binding.fullName.requestFocus()
        }

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
            showSelection()
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
        binding.toolbar.title = CacheUtil.getInstance().getTranslations(Text.HELP_SUPPORT_LABEL, "Help & Support")

        binding.message.text = CacheUtil.getInstance().getTranslations(
            Text.FEEDBACK_MESSAGE,
            "We value your opinion, so please tell us about your experience with our app or about your meal."
        )
        binding.addImageLabel.text =
            CacheUtil.getInstance().getTranslations(Text.ADD_AN_IMAGE_CAPS, "ADD AN IMAGE")
        binding.uploadImage.text = CacheUtil.getInstance().getTranslations(Text.UPLOAD_IMAGE, "Upload Image")
        binding.fullNameLabel.text = CacheUtil.getInstance().getTranslations(Text.FULL_NAME)
        binding.usernameLabel.text = CacheUtil.getInstance().getTranslations(Text.EMAIL_ADDRESS)
        binding.phoneNumberLabel.text = CacheUtil.getInstance().getTranslations(Text.PHONE_NUMBER)
        binding.commentLabel.text = CacheUtil.getInstance().getTranslations(Text.YOUR_COMMENT)
        binding.comment.hint = CacheUtil.getInstance().getTranslations(
            Text.TELL_US_ABOUT_YOUR_EXPERIENCE_HERE,
            "Tell us about your experience here"
        )
        binding.submit.text = CacheUtil.getInstance().getTranslations(Text.SUBMIT, "Submit")
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

    private fun showSelection() {
        pickerDialog = PickerDialog.newInstance(object : PickerDialog.OnAction {
            override fun onClose() {
                pickerDialog?.dismiss()
            }

            override fun onCamera() {
                ImagePicker.with(currentActivity)
                    .cameraOnly()
                    .createIntent { intent ->
                        cameraLauncher.launch(intent)
                    }
                pickerDialog?.dismiss()
            }

            override fun onGallery() {
                showImagePicker()
                pickerDialog?.dismiss()
            }

        })
        pickerDialog?.isCancelable = true
        pickerDialog?.show(supportFragmentManager, PickerDialog::class.java.toString())
    }

    private fun processImage(uri: Uri) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
        imageFile = File.createTempFile("IMG_", ".jpg", this@FeedbackActivity.cacheDir)
        FileOutputStream(imageFile).use {
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                30,
                it
            )
        }
        bitmap.recycle()
        Glide.with(this)
            .load(uri)
            .into(binding.image)
        binding.imgContainer.visibility = View.VISIBLE
        binding.imageEmptyContainer.visibility = View.GONE
    }


    private fun showImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        standardImagePickerLauncher.launch(intent)
    }

}