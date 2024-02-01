package com.herfy.notification.views

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.herfy.notification.NotificationContract
import com.herfy.notification.adapter.RecyclerTouchListener
import com.herfy.notification.adapter.NotificationAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.data.constants.Text
import com.herfy.databinding.FragmentNotificationsBinding
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.notification.NotificationPresenter
import com.herfy.utils.helper.LocaleUtil
import com.herfy.notification.adapter.RecyclerTouchListener.OnRowClickListener
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifDeeplink
import com.herfy.util.CacheUtil
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifData
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.notification.Notification
import com.herfy.analytics.MixpanelUtil
import com.herfy.util.NavigationUtils
import java.util.*

class NotificationsFragment : BaseFragment(), NotificationContract.View {

    private lateinit var binding: FragmentNotificationsBinding

    private var touchListener: RecyclerTouchListener? = null

    //    private boolean canLoadMoreItems = false;
    private var isLoadingHistory = false
    private var page = 1
    private var notificationPresenter: NotificationContract.Presenter? = null
    private var notificationAdapter: NotificationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPresenter = NotificationPresenter(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoadingDialog()
        notificationPresenter!!.start()
        notificationPresenter!!.getNotifications(page)
        binding.rvNotif.addOnItemTouchListener(touchListener!!)
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
        val linearLayoutManager = LinearLayoutManager(context)
        binding.rvNotif.layoutManager = linearLayoutManager
        notificationAdapter = NotificationAdapter(context, notificationPresenter)
        binding.rvNotif.adapter = notificationAdapter
        touchListener = RecyclerTouchListener(activity, binding.rvNotif, LocaleUtil.getInstance().language)
        touchListener!!.setClickable(true)
            .setIndependentViews(R.id.container)
            .setClickable(object : OnRowClickListener {
                override fun onRowClicked(position: Int) {}
                override fun onIndependentViewClicked(independentViewID: Int, position: Int) {
                    if (independentViewID == R.id.container) {
                        val notification = notificationPresenter?.getNotification(position);
                        val notifDeeplink = notification?.notifData?.deeplink
                        if (notifDeeplink != null && notifDeeplink.type != null) {
                            processDeeplink(notifDeeplink)
                            notificationPresenter?.getNotification(notification.id)
                        } else {
                            notificationPresenter?.getNotification(position)?.id?.let {
                                NavigationUtils.navigateSafe(findNavController(), NotificationsFragmentDirections.actionNotificationsToNotification(it))
                            }
                        }
                    }
                }
            })
            .setSwipeOptionViews(R.id.clDelete)
            .setSwipeable(R.id.foregroundRow, R.id.rowBG) { viewID, position ->
                when (viewID) {
                    R.id.clDelete -> {
                        val notifId = notificationPresenter!!.notificationList[position].id
                        notificationPresenter!!.deleteNotif(notifId)
                    }
                }
            }
        binding.rvNotif.addOnItemTouchListener(touchListener!!)
        binding.rvNotif.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isLoadingHistory && page <= notificationPresenter!!.totalNotifPageCount && linearLayoutManager.findLastCompletelyVisibleItemPosition() >= linearLayoutManager.itemCount - 1) {
                    isLoadingHistory = true
                    binding.loadMoreProgressbar.visibility = View.VISIBLE
                    notificationPresenter!!.getNotifications(page)
                }
            }
        })
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.tvNoNotif.text = CacheUtil.getInstance()
            .getTranslations(Text.NO_NOTIFICATIONS, "No notifications")
        binding.tvNoNotifDesc.text = CacheUtil.getInstance().getTranslations(
            Text.YOU_HAVE_NO_NOTIFICATIONS,
            "You have no notifications yet."
        )
    }

    override fun showNotifications(notifications: LinkedList<Notification>, pageCount: Int) {
        isLoadingHistory = false
        if (page < pageCount) {
            page += 1
        }
        binding.loadMoreProgressbar.visibility = View.GONE
        if (notifications.isEmpty()) {
            page = 1
        }
        notificationAdapter!!.notifyDataSetChanged()
        dismissDialogs()
        if (notifications.size > 0) {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvNotif.visibility = View.VISIBLE
            activity?.cast<MainActivity> {
                if(it.currentDestination == R.id.nav_notifications) {
                    it.showToolbarCustomActionButton(true)
                    it.setToolbarCustomActionText(
                        CacheUtil.getInstance().getTranslations(Text.CLEAR_ALL, "Clear All")
                    )
                    it.setToolbarCustomActionButton {
                        if (notificationPresenter?.notificationList?.isNotEmpty() == true) {
                            showClearAllDialog()
                        }
                    }
                }
            }
            activity?.cast<NavigationActivity> {
                if(it.currentDestination == R.id.nav_notifications) {
                    it.showToolbarCustomActionButton(true)
                    it.setToolbarCustomActionText(
                        CacheUtil.getInstance().getTranslations(Text.CLEAR_ALL, "Clear All")
                    )
                    it.setToolbarCustomActionButton {
                        if (notificationPresenter?.notificationList?.isNotEmpty() == true) {
                            showClearAllDialog()
                        }
                    }
                }
            }
        } else {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvNotif.visibility = View.GONE
            activity?.cast<MainActivity> {
                it.showToolbarCustomActionButton(false)
            }
            activity?.cast<NavigationActivity> {
                it.showToolbarCustomActionButton(false)
            }
        }
    }

    override fun setupNotification(notification: Notification) {}
    override fun showNotificationDetails(notifData: NotifData) {}
    override fun orderDeleted() {
        Snackbar.make(
            binding.coordinatorLayout,
            CacheUtil.getInstance()
                .getTranslations(Text.NOTIFICATIONS_DELETED, "Notification Deleted"),
            Snackbar.LENGTH_SHORT
        ).show()
        page = 1
        notificationPresenter!!.getNotifications(page)
    }

    override fun showNotifError() {
        isLoadingHistory = false
        //        canLoadMoreItems = false;
        dismissDialogs()
    }

    override fun showDigitalCoupon(digitalCoupon: Campaign) {
        dismissDialogs()
        NavigationUtils.navigateSafe(findNavController(), NotificationsFragmentDirections.actionNotificationsToDigitalCoupon(
            Gson().toJson(digitalCoupon)
        ))
    }

    override fun showError(message: String) {
        activity?.cast<BaseActivity> {
            it.showAlertDialog("", message)
        }
    }
    private fun processDeeplink(notifDeeplink: NotifDeeplink) {
        val type = notifDeeplink.type
        val value = notifDeeplink.value
        when (type) {
            "home" -> findNavController().navigateUp()
            "categories", "category" -> NavigationUtils.navigateSafe(findNavController(), NotificationsFragmentDirections.actionNotificationsToMenu(value))
            "item" -> NavigationUtils.navigateSafe(findNavController(), NotificationsFragmentDirections.actionNotificationsToMenuItem("", value))
            "digital-coupon", "promotions", "promo" -> {
                showLoadingDialog()
                notificationPresenter?.getDigitalCoupons(value)
            }
            "shoppingcart", "shopping-cart", "cart" -> {
                MixpanelUtil.trackEvent(MixpanelUtil.EVENT_VIEW_CART)
                NavigationUtils.navigateSafe(findNavController(), NotificationsFragmentDirections.actionNotificationsToCart())
            }
            "order" -> NavigationUtils.navigateSafe(findNavController(), NotificationsFragmentDirections.actionNotificationsToOrder(value))
        }
    }

    private fun showClearAllDialog() {
        val alertDialog = AlertDialog.Builder(context)
        val customLayout = layoutInflater.inflate(R.layout.dialog_notif_delete_all, null)
        alertDialog.setView(customLayout)
        val dialogHeading = customLayout.findViewById<TextView>(R.id.heading)
        val dialogDesc = customLayout.findViewById<TextView>(R.id.label_desc)
        val btnYes = customLayout.findViewById<TextView>(R.id.btn_yes)
        val btnNo = customLayout.findViewById<TextView>(R.id.btn_no)
        dialogHeading.text = CacheUtil.getInstance()
            .getTranslations(Text.CLEAR_ALL_NOTIFICATIONS, "Clear all notifications?")
        dialogDesc.text = CacheUtil.getInstance().getTranslations(
            Text.ARE_YOU_SURE_YOU_WANT,
            "Are you sure you want to delete these all these notifications? This can’t be undone."
        )
        btnYes.text = CacheUtil.getInstance().getTranslations(Text.YES_CLEAR_CAPS, "YES, CLEAR")
        btnNo.text = CacheUtil.getInstance().getTranslations(Text.NO_KEEP_CAPS, "NO, KEEP")
        val alert = alertDialog.create()
        btnYes.setOnClickListener {
            alert.dismiss()
            showLoadingDialog()
            notificationPresenter!!.clearAllNotif()
        }
        btnNo.setOnClickListener { alert.dismiss() }
        alert.setCanceledOnTouchOutside(false)
        alert.show()
        alert.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

}