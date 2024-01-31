package com.herfy.digitalcoupons.views

import android.app.Activity
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.herfy.digitalcoupons.DigitalCouponsContract
import com.herfy.digitalcoupons.DigitalCouponsPresenter
import com.herfy.util.CacheUtil
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.data.constants.Text
import com.herfy.databinding.FragmentDigitalCouponBinding
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.herfy.analytics.MixpanelUtil
import com.herfy.util.NavigationUtils
import java.lang.Exception
import java.util.*

class DigitalCouponFragment : BaseFragment(), DigitalCouponsContract.View {

    private lateinit var binding: FragmentDigitalCouponBinding

    private val args: DigitalCouponFragmentArgs by navArgs()

    private lateinit var digitalCouponsPresenter: DigitalCouponsContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val activity: Activity? = activity
            if (activity != null) {
                sharedElementEnterTransition =
                    TransitionInflater.from(activity).inflateTransition(android.R.transition.move)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        digitalCouponsPresenter = DigitalCouponsPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDigitalCouponBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        digitalCouponsPresenter.start()
        digitalCouponsPresenter.setDigitalCoupon(args.digitalCoupon)
        showDigitalCoupon(digitalCouponsPresenter.digitalCoupon)
    }

    override fun setPresenter(presenter: DigitalCouponsContract.Presenter) {
        digitalCouponsPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.let {
            val baseActivity = it as BaseActivity
            baseActivity.logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue)
        }
    }

    override fun setCartExpiry() {}
    override fun setupViews() {
        binding.avail.setOnClickListener {
            try {
                val activity = activity as BaseActivity?
                if (activity != null) {
                    if (CacheUtil.getInstance().couponCode != null) {
                        activity.showPromptAlertDialog("",
                            CacheUtil.getInstance().getTranslations(
                                Text.ONE_DISCOUNT_ONLY_MESSAGE,
                                "You can only apply one discount at a time. Currently applied discount will be removed"
                            ),
                            CacheUtil.getInstance().getTranslations(
                                Text.YES_CAPS
                            ),
                            DialogInterface.OnClickListener { dialogInterface, i ->
                                dialogInterface.dismiss()
                                CacheUtil.getInstance().couponCode = null
                                CacheUtil.getInstance().couponName = null
                                CacheUtil.getInstance().couponMenuItem = null
                                CacheUtil.getInstance().couponToAdd = null
                                activity.showLoadingDialog()
                                digitalCouponsPresenter.validateDigitalCoupon()
                            },
                            CacheUtil.getInstance().getTranslations(Text.NO_CAPS),
                            DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int -> dialogInterface.dismiss() })
                    } else {
                        activity.showLoadingDialog()
                        digitalCouponsPresenter.validateDigitalCoupon()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.avail.text = CacheUtil.getInstance()
            .getTranslations(Text.AVAIL_NOW_CAPS, "AVAIL NOW")
    }

    override fun showCart() {
        MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
        NavigationUtils.navigateSafe(
            NavHostFragment.findNavController(this),
            DigitalCouponFragmentDirections.actionDigitalCouponToCart(
                isFromPromotions = true
            )
        )
    }

    override fun showDigitalCoupons(digitalCoupons: ArrayList<Campaign>) {}
    override fun showDigitalCoupon(digitalCoupon: Campaign) {
        binding.card.transitionName = digitalCoupon.id
        try {
            val activity: Activity? = activity
            if (activity != null) {
                val imageUrl = digitalCoupon.attributes.imageUri
                if (imageUrl != null && !imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) && !imageUrl.isEmpty() && !imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    Glide.with(activity).load(imageUrl).skipMemoryCache(false).diskCacheStrategy(
                        DiskCacheStrategy.ALL
                    ).listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            //holder.digitalImage.setVisibility(View.INVISIBLE);
                            //holder.progresssImage.setVisibility(View.GONE);
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any,
                            target: Target<Drawable?>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            //holder.digitalImage.setVisibility(View.VISIBLE);
                            //holder.progresssImage.setVisibility(View.GONE);
                            return false
                        }
                    }).into(binding.image)
                }
                binding.name.text = digitalCoupon.attributes.name
                val descriptionValue = digitalCoupon.attributes.description
                if (descriptionValue != null && !descriptionValue.equals(
                        "null",
                        ignoreCase = true
                    ) && !descriptionValue.isEmpty()
                ) {
                    binding.description.text = descriptionValue
                    binding.descriptionCard.visibility = View.VISIBLE
                } else {
                    binding.descriptionCard.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun showError(error: String, back: Boolean) {
        try {
            val activity = activity as BaseActivity?
            activity?.showAlertDialog(
                "",
                error,
                CacheUtil.getInstance().getTranslations(Text.OK)
            ) { dialog, which ->
                if (back) {
                    activity.onBackPressed()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}