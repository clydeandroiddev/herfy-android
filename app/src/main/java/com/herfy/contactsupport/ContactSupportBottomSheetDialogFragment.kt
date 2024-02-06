package com.herfy.contactsupport

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.herfy.R
import com.herfy.base.BaseBottomSheetDialogFragment
import com.herfy.databinding.BottomSheetDialogContactSupportBinding
import com.herfy.util.ViewUtil
import com.herfy.utils.constants.Text
import com.herfy.utils.helper.CacheUtil


class ContactSupportBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    private var binding: BottomSheetDialogContactSupportBinding? = null

    private var selectedPhone = ""

    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(isGranted) {
            callSupport()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetDialogContactSupportBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val content = SpannableStringBuilder()

        val developedBy = resources.getString(R.string.contact_support_developed_by)
        if(developedBy.isNotEmpty()) {
            content.append(
                CacheUtil.getInstance().getTranslations(
                    Text.CONTACT_SUPPORT_DEVELOPED_BY,
                    "This app is developed by (x)."
                ).replace("(x)", developedBy)
            )
        }

        val email = resources.getString(R.string.contact_support_email)
        val emailSubject = resources.getString(R.string.contact_support_email_subject)
        if(email.isNotEmpty() && emailSubject.isNotEmpty()) {
            val emailTranslation = " " + CacheUtil.getInstance().getTranslations(
                Text.CONTACT_SUPPORT_EMAIL,
                "For all support regarding this app, please write to (x) with the subject like ‘(y)’.")
            val emailSpan = SpannableString(
                emailTranslation.replace("(x)", email).replace("(y)", emailSubject)
            )
            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(textView: View) {
                    activity?.apply {
                        val selectorIntent = Intent(Intent.ACTION_SENDTO)
                        selectorIntent.data = Uri.parse("mailto:")
                        val emailIntent = Intent(Intent.ACTION_SEND)
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                        emailIntent.selector = selectorIntent
                        activity?.startActivity(Intent.createChooser(
                            emailIntent,
                            CacheUtil.getInstance().getTranslations(
                                Text.CONTACT_SUPPORT,
                                "Contact Support"
                            )
                        ))
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }
            val startIndex = emailTranslation.indexOf("(")
            emailSpan.setSpan(
                clickableSpan,
                startIndex,
                startIndex + email.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            content.append(emailSpan)
        }

        val phones = resources.getTextArray(R.array.contact_support_phone)
        if(phones.isNotEmpty()) {
            val phoneTranslation = " " + CacheUtil.getInstance().getTranslations(
                Text.CONTACT_SUPPORT_PHONE,
                "Or you may call (x)")
            val phoneSpan = SpannableStringBuilder(
                phoneTranslation.replace("(x)", "")
            )
            phones.forEachIndexed { index, phone ->
                val phonesText = SpannableString(phone)
                if(index != 0) {
                    phoneSpan.append(" / ")
                }
                val clickableSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(textView: View) {
                        selectedPhone = phone.toString()
                        callSupport()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                }
                phonesText.setSpan(
                    clickableSpan,
                    0,
                    phone.length,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
                phoneSpan.append(phonesText)
            }
            content.append(phoneSpan)
        }

        binding?.tvContent?.apply {
            text = content
            movementMethod = LinkMovementMethod.getInstance();
            highlightColor = Color.TRANSPARENT
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { parentView ->
                val behaviour = BottomSheetBehavior.from(parentView)
                setupFullHeight(parentView)
                activity?.let { context ->
                    behaviour.maxHeight = ViewUtil.getScreenHeight(context) -
                            ViewUtil.dpToPx(context, 100)
                }
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    private fun callSupport() {
        if(selectedPhone.isNotEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(Intent.ACTION_CALL)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.data = Uri.parse("tel:$selectedPhone")
                startActivity(intent)
            } else {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}