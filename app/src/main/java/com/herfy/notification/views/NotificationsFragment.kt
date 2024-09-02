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
import com.herfy.R
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.main.MainActivity
import com.herfy.notification.NotificationContract
import com.herfy.notification.NotificationPresenter
import com.herfy.notification.adapter.NotificationAdapter
import com.herfy.notification.adapter.RecyclerTouchListener
import com.herfy.notification.adapter.RecyclerTouchListener.OnRowClickListener
import com.herfy.notification.databinding.FragmentNewNotificationBinding
import com.herfy.utils.constants.Text
import com.herfy.utils.extensions.navigateToDeepLink
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.NavigationUtils
import com.herfy.utils.helper.viewLifecycleScope
import com.skylinedynamics.solosdk.api.models.objects.Campaign
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifData
import com.skylinedynamics.solosdk.api.models.objects.notification.NotifDeeplink
import com.skylinedynamics.solosdk.api.models.objects.notification.Notification
import java.util.LinkedList

class NotificationsFragment : BaseFragment(), NotificationContract.View {

    private lateinit var binding: FragmentNewNotificationBinding

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
        binding = FragmentNewNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                        val notification = notificationPresenter?.getNotification(position)
                        val notifDeeplink = notification?.notifData?.deeplink
                        val notificationId = notificationPresenter?.getNotification(position)?.id
                        notificationId?.let {
                            notificationPresenter?.markNotificationAsRead(position, it)
                        }
                        if (notifDeeplink != null && notifDeeplink.type != null) {
                            processDeeplink(notifDeeplink)
                        } else {
                            notificationId?.let {
                                findNavController().navigateToDeepLink("nav_notification/$it")
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
                    notificationPresenter!!.getNotifications(page)
                }
            }
        })
        binding
            .icClose.ninjaTap(viewLifecycleScope) {
                findNavController().navigateUp()
            }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.emptyState.setTitle( CacheUtil.getInstance()
            .getTranslations(Text.NO_NOTIFICATIONS, "No notifications.") + ".");
        binding.txtNotificationTitle.setText(CacheUtil
            .getInstance().getTranslations(Text.NOTIFICATIONS, "Notifications"))
    }

    override fun showNotifications(notifications: LinkedList<Notification>, pageCount: Int) {
        isLoadingHistory = false
        if (page < pageCount) {
            page += 1
        }
        binding.notificationShimmer.visibility = View.GONE
        if (notifications.isEmpty()) {
            page = 1
        }
        notificationAdapter!!.notifyDataSetChanged()
        dismissDialogs()
        if (notifications.size > 0) {
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
        } else {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvNotif.visibility = View.GONE
            activity?.cast<MainActivity> {
                it.showToolbarCustomActionButton(false)
            }
        }
    }

    override fun setupNotification(notification: Notification) {}
    override fun showNotificationDetails(notifData: NotifData) {}
    override fun orderDeleted() {
//        notificationPresenter!!.getNotifications(page)
    }

    override fun showNotifError() {
        isLoadingHistory = false
        //        canLoadMoreItems = false;
        dismissDialogs()
    }

    override fun showDigitalCoupon(digitalCoupon: Campaign) {
        dismissDialogs()
    }

    override fun showError(message: String) {
        activity?.cast<BaseActivity> {
            it.showAlertDialog("", message)
        }
    }

    override fun setIsLoading(isLoading: Boolean) {
        binding.rvNotif.setVisible(!isLoading)
        binding.notificationShimmer.setVisible(isLoading)
        binding.notificationShimmer.startShimmer()
    }

    override fun goToItemDetails(menuCategoryId: String, menuItem: MenuItem) {
        dismissDialogs()
        if(menuItem.isCombo) {
            NavigationUtils.navigateSafe(
                findNavController(),
                NotificationsFragmentDirections.actionNotificationsToComboMeal(
                    menuCategoryId = menuCategoryId,
                    menuItemId = menuItem.id,
                    cartMenuItemPosition = -1,
                    isUpsell = false
                )
            )
        } else {
            NavigationUtils.navigateSafe(
                findNavController(),
                NotificationsFragmentDirections.actionNotificationsToMenuItem(
                    menuItemId = menuItem.id,
                    menuCategoryId = menuCategoryId,
                    cartMenuItemPosition = -1,
                    isUpsell = false
                )
            )
        }
    }

    override fun markNotificationAsRead(position: Int) {
        notificationAdapter?.notifyItemChanged(position)
    }

    private fun processDeeplink(notifDeeplink: NotifDeeplink) {
        val type = notifDeeplink.type
        val value = notifDeeplink.value
        when (type) {
            "home" -> findNavController().navigateUp()
            "categories", "category" -> findNavController().navigateToDeepLink("nav_menu/$value")
            "item" -> {
                showLoadingDialog()
                notificationPresenter?.getMenuItem(value)
            }
            "digital-coupon", "promotions", "promo" -> findNavController().navigateToDeepLink("nav_promotions")
            "shoppingcart", "shopping-cart", "cart" -> {
                findNavController().navigateToDeepLink("nav_cart")
            }
            "order" -> findNavController().navigateToDeepLink("nav_order/$value")
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