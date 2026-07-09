package com.herfy.utils.widgets.otp

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.herfy.utils.R
import java.util.regex.Pattern

class OTPTextView : FrameLayout {

    private var totalSpace = 0
    private var heightNew = 0
    private var space = 0
    private var marginHorizontal = 0
    private var widthNew = 0
    private var spaceLeft = 0
    private var spaceRight = 0
    private var spaceTop = 0
    private var spaceBottom = 0
    private var attrsNew: AttributeSet? = null

    private var itemViews: MutableList<ItemView>? = null
    private var otpChildEditText: OTPChildEditText? = null
    var otpListener: OTPListener? = null

    private var length: Int = 0
    fun setLength(length: Int) {
        this.length = length
    }

    private val filter: InputFilter
        get() = InputFilter { source, start, end, _, _, _ ->
            for (i in start until end) {
                if (!Pattern.compile(
                        PATTERN
                    ).matcher(source[i].toString()).matches()
                ) {
                    return@InputFilter ""
                }
            }
            null
        }

    var otp: String? = ""
        get() = otpChildEditText?.text?.toString()

    constructor(context: Context) : super(context) {
        init(null, context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(attrs, context)
    }

    private fun init(attrs: AttributeSet?, context: Context) {
        val styles = context.obtainStyledAttributes(attrs, R.styleable.OtpTextView)
        styleEditTexts(styles, attrs, context)
        styles.recycle()
    }

    private fun styleEditTexts(
        styles: TypedArray, attrs: AttributeSet?, context: Context
    ) {
        length = styles.getInt(R.styleable.OtpTextView_length, DEFAULT_LENGTH)
        generateViews(styles, attrs, context)
    }

    private fun generateViews(
        styles: TypedArray, attrs: AttributeSet?, context: Context
    ) {
        attrsNew = attrs
        if (length > 0) {
            otp = styles.getString(R.styleable.OtpTextView_otp)
            heightNew = styles.getDimension(
                R.styleable.OtpTextView_height, Utils.getPixels(context, DEFAULT_HEIGHT).toFloat()
            ).toInt()

            space = styles.getDimension(
                R.styleable.OtpTextView_box_margin,
                Utils.getPixels(context, DEFAULT_SPACE).toFloat()
            ).toInt()

            marginHorizontal = styles.getDimension(
                R.styleable.OtpTextView_box_margin_horizontal,
                Utils.getPixels(context, DEFAULT_MARGIN_HORIZONTAL).toFloat()
            ).toInt()

            totalSpace = ((space * 2) * length) + ((marginHorizontal * 2))

            widthNew = (getDeviceWidth(context) - totalSpace) / length

            spaceLeft = styles.getDimension(
                R.styleable.OtpTextView_box_margin_left,
                Utils.getPixels(context, DEFAULT_SPACE_LEFT).toFloat()
            ).toInt()

            spaceRight = styles.getDimension(
                R.styleable.OtpTextView_box_margin_right,
                Utils.getPixels(context, DEFAULT_SPACE_RIGHT).toFloat()
            ).toInt()

            spaceTop = styles.getDimension(
                R.styleable.OtpTextView_box_margin_top,
                Utils.getPixels(context, DEFAULT_SPACE_TOP).toFloat()
            ).toInt()

            spaceBottom = styles.getDimension(
                R.styleable.OtpTextView_box_margin_bottom,
                Utils.getPixels(context, DEFAULT_SPACE_BOTTOM).toFloat()
            ).toInt()

            generate()

        } else {
            throw IllegalStateException("Please specify the length of the otp view")
        }
    }

    fun generate() {
        removeAllViews()
        invalidate()

        itemViews = ArrayList()

        totalSpace = ((space * 2) * length) + ((marginHorizontal * 2))

        widthNew = (getDeviceWidth(context) - totalSpace) / length

        val params = LinearLayout.LayoutParams(widthNew, heightNew)
        if (space > 0) {
            params.setMargins(space, space, space, space)
        } else {
            params.setMargins(spaceLeft, spaceTop, spaceRight, spaceBottom)
        }

        val editTextLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )

        editTextLayoutParams.gravity = Gravity.CENTER
        otpChildEditText = OTPChildEditText(context)
        otpChildEditText?.filters = arrayOf(filter, InputFilter.LengthFilter(length))
        setTextWatcher(otpChildEditText, length)
        addView(otpChildEditText, editTextLayoutParams)

        val linearLayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val linearLayout = LinearLayout(context)

        addView(linearLayout, linearLayoutParams)

        for (i in 0 until length) {
            // Regeneration can happen after the original XML parser is no longer valid.
            // Fall back to a context-only constructor so OTP setup continues instead of crashing.
            val itemView = try {
                ItemView(context, attrsNew)
            } catch (e: RuntimeException) {
                Log.w("OTPTextView", "Falling back to default ItemView attrs", e)
                ItemView(context)
            }
            itemView.setViewState(ItemView.INACTIVE)
            linearLayout.addView(itemView, i, params)
            itemViews?.add(itemView)
        }

        if (otp != null) {
            setOTP(otp.toString())
        } else {
            setOTP("")
        }
    }

    private fun setTextWatcher(otpChildEditText: OTPChildEditText?, length: Int) {
        otpChildEditText?.addTextChangedListener(object : TextWatcher {
            /**
             * @param s
             * @param start
             * @param count
             * @param after
             */
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            /**
             * @param s
             * @param start
             * @param before
             * @param count
             */
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                otpListener?.let { otpListener ->
                    otpListener.onInteractionListener()
                    if (s.length == length) {
                        otpListener.onOTPComplete(s.toString())
                    }
                }
                setOTP(s)
                setFocus(s.length)
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
    }

    private fun setFocus(length: Int) {
        itemViews?.let { itemViews ->
            for (i in itemViews.indices) {
                if (i == length) {
                    itemViews[i].setViewState(ItemView.ACTIVE)
                } else {
                    itemViews[i].setViewState(ItemView.INACTIVE)
                }
            }
            if (length == itemViews.size) {
                itemViews[itemViews.size - 1].setViewState(ItemView.ACTIVE)
            }
        }
    }

    fun setOTP(s: CharSequence) {
        itemViews?.let { itemViews ->
            for (i in itemViews.indices) {
                if (i < s.length) {
                    itemViews[i].setText(s[i].toString())
                } else {
                    itemViews[i].setText("")
                }
            }
        }
    }

    fun requestFocusOTP() {
        otpChildEditText?.requestFocus()
    }

    fun showError() {
        itemViews?.let { itemViews ->
            for (itemView in itemViews) {
                itemView.setViewState(ItemView.ERROR)
            }
        }
    }

    fun resetState() {
        otp?.let {
            setFocus(it.length)
        }
    }

    fun showSuccess() {
        itemViews?.let { itemViews ->
            for (itemView in itemViews) {
                itemView.setViewState(ItemView.SUCCESS)
            }
        }
    }

    fun setOTP(otp: String) {
        otpChildEditText?.setText(otp)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setOnTouchListener(l: OnTouchListener) {
        super.setOnTouchListener(l)
        otpChildEditText?.setOnTouchListener(l)
    }

    companion object {

        private const val DEFAULT_LENGTH = 6
        private const val DEFAULT_HEIGHT = 43
        private const val DEFAULT_SPACE = -1
        private const val DEFAULT_SPACE_LEFT = 4
        private const val DEFAULT_SPACE_RIGHT = 4
        private const val DEFAULT_SPACE_TOP = 4
        private const val DEFAULT_MARGIN_HORIZONTAL = 16
        private const val DEFAULT_SPACE_BOTTOM = 4
        private const val PATTERN = "[1234567890]*"
    }

    private fun getDeviceWidth(context: Context): Int {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }
}
