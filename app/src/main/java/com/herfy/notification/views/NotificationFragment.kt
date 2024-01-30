package com.herfy.notification.views

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import com.herfy.base.BaseFragment
import com.herfy.data.constants.Text
import com.herfy.databinding.FragmentNotificationBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.notification.NotificationContract
import com.herfy.notification.NotificationPresenter
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifData
import com.skylinedynamics.solosdk.api.models.objects.notification.Notification
import com.herfy.util.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class NotificationFragment : BaseFragment(), NotificationContract.View {

    private lateinit var binding: FragmentNotificationBinding

    private val args: NotificationFragmentArgs by navArgs()

    private var notificationPresenter: NotificationContract.Presenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPresenter = NotificationPresenter(this)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoadingDialog()
        notificationPresenter?.start()
        notificationPresenter?.getNotification(args.notificationId)
    }

    override fun setPresenter(presenter: NotificationContract.Presenter) {
        notificationPresenter = presenter
    }

    override fun logEvent(
        eventName: String,
        eventAttributes: HashMap<String, String>,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
    }

    override fun setCartExpiry() {

    }

    override fun setupViews() {

    }

    override fun setupFonts() {}
    override fun setupTranslations() {

    }

    override fun showNotifications(notifications: LinkedList<Notification>, pageCount: Int) {}
    override fun setupNotification(notification: Notification) {}
    override fun showNotificationDetails(notifData: NotifData) {
        dismissDialogs()
        activity?.cast<MainActivity> {
            it.showToolbarCustomActionButton(true)
            it.setToolbarCustomActionText(CacheUtil.getInstance().getTranslations(Text.DELETE, "Delete"))
            it.setToolbarCustomActionButton {
                showLoadingDialog()
                notificationPresenter!!.deleteNotif(args.notificationId)
            }
        }
        activity?.cast<NavigationActivity> {
            it.showToolbarCustomActionButton(true)
            it.setToolbarCustomActionText(CacheUtil.getInstance().getTranslations(Text.DELETE, "Delete"))
            it.setToolbarCustomActionButton {
                showLoadingDialog()
                notificationPresenter!!.deleteNotif(args.notificationId)
            }
        }
        binding.tvTitle.text = notifData.title.getValue(LocaleUtil.getInstance().language)
        binding.tvDesc.text = notifData.message.getValue(LocaleUtil.getInstance().language)
        if (notifData.createdAt != null) {
            val simpleDateFormat = SimpleDateFormat("MMMM dd, yyyy hh:mm a")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
            try {
                val date = dateFormat.parse(notifData.createdAt.replace("T", " "))
                binding.tvDate.text = simpleDateFormat.format(date)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }
        if (notifData.imageUri != null) {
            binding.image.visibility = View.VISIBLE
            Glide.with(this)
                .load(notifData.imageUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.image)
        }
    }

    override fun orderDeleted() {
        dismissDialogs()
        Snackbar.make(
            binding.coordinatorLayout,
            CacheUtil.getInstance()
                .getTranslations(Text.NOTIFICATIONS_DELETED, "Notification Deleted"),
            Snackbar.LENGTH_SHORT
        ).show()
        Handler(Looper.getMainLooper()).postDelayed({ findNavController().navigateUp() }, 400)
    }

    override fun showNotifError() {}
    override fun showDigitalCoupon(digitalCoupon: Campaign) {}
    override fun showError(message: String?) {}
}