package com.herfy.newmenu.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.herfy.utils.constants.Text
import com.herfy.databinding.FragmentMenuItemsBinding
import com.herfy.ext.onScrollStateChanged
import com.herfy.menu.MenuContract
import com.herfy.newmenu.NewMenuTabListener
import com.herfy.newmenu.adapter.NewMenuRecyclerViewAdapter
import com.skylinedynamics.solosdk.api.models.objects.MenuCategory
import com.skylinedynamics.solosdk.api.models.objects.MenuItem
import com.herfy.util.scroll.ViewPagerContentRecyclerViewTouchEventInterceptor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.herfy.utils.helper.*

class MenuItemsFragment : Fragment() {
    private var position = 0
    private var presenter: MenuContract.Presenter? = null
    private var listener: NewMenuTabListener? = null
    private var adapter: NewMenuRecyclerViewAdapter? = null
    private var notifyDataSetChangedJob: Job? = null

    private var _binding: FragmentMenuItemsBinding? = null
    private val binding get() = _binding!!
    private var category: MenuCategory? = null

    private fun debouncedNotifyDataSetChanged() {
        notifyDataSetChangedJob?.cancel()
        val adapter = adapter ?: return
        notifyDataSetChangedJob = lifecycleScope.launch {
            delay(1000)
            adapter.notifyDataSetChanged()
//            adapter.updateItems();
        }
    }

    // TODO: fix bad practice
    fun newInstance(position: Int, presenter: MenuContract.Presenter?, listener: NewMenuTabListener?) {
        this.position = position
        this.presenter = presenter
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMenuItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setNestedScrollingEnabled(binding.menuItemsList, false)
        initAdapter()
    }

    override fun onResume() {
        super.onResume()
        debouncedNotifyDataSetChanged()
    }

    override fun onDestroy() {
        notifyDataSetChangedJob?.cancel()
        presenter = null
        listener = null
        super.onDestroy()
    }

    private fun initAdapter() {
        try {
            if (presenter != null) {
                var isScrolling = false
                var isBottomScrolled = false
                category = presenter?.getMenuCategoryByPosition(position) ?: return
                if (!category?.menuItems.isNullOrEmpty() && category?.menuItems?.let {
                        isAllItemExcluded(it)
                } == false) {
                    binding.containerItems.visibility = View.VISIBLE
                    binding.containerEmpty.visibility = View.GONE
                    val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    adapter = NewMenuRecyclerViewAdapter(context, position, category, presenter)
                    with(binding.nestedScrollView) {
                        onScrollStateChanged {
                            isScrolling = it
                            listener?.onMenuCategoryItemsScrolled(!isScrolling, isBottomScrolled)
                        }
                        setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                            isBottomScrolled = scrollY >= (v.getChildAt(0).measuredHeight - v.measuredHeight)
                        })
                    }
                    with(binding.menuItemsList) {
                        this.layoutManager = layoutManager
                        this.adapter = this@MenuItemsFragment.adapter
                        this.addOnItemTouchListener(ViewPagerContentRecyclerViewTouchEventInterceptor())
                        /*addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                super.onScrolled(recyclerView, dx, dy)
                                if (dy > 0) {
                                    isScrollable = true
                                    listener?.onLastItemReach(isScrollable, ScrollDirection.Up)
                                } else if (dy < 0) {
                                    isScrollable = true
                                    listener?.onLastItemReach(isScrollable, ScrollDirection.Down)
                                }
                            }
                            override fun onScrollStateChanged(
                                recyclerView: RecyclerView,
                                newState: Int
                            ) {
                                super.onScrollStateChanged(recyclerView, newState)
                                if(newState == 0 && layoutManager.findLastCompletelyVisibleItemPosition() == recyclerView.adapter?.itemCount?.minus(1)   && !isScrollable) {
                                    listener?.onLastItemReach( false, ScrollDirection.Idle)
                                }
                            }
                        })*/
                    }
                    listener?.setTabs(adapter)
                    adapter?.notifyDataSetChanged()
                } else {
                    binding.containerItems.visibility = View.GONE
                    binding.containerEmpty.visibility = View.VISIBLE
                    binding.tvNoAvailable.text = CacheUtil.getInstance().getTranslations(
                        Text.NO_AVAILALBLE_ITEMS, "No available items")
                    binding.tvPleaseTry.text = CacheUtil.getInstance().getTranslations(
                        Text.EMPTY_ITEMS_CHANGE_BRANCH, "Please try changing to a different branch.")
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun isAllItemExcluded(items: List<MenuItem>): Boolean {
        var excludedItems = arrayListOf<MenuItem>()
        if (CacheUtil.getOrderStoreId() != null) {
            val orderId = CacheUtil.getOrderStoreId()
            for (menuItem in items) {
                if (orderId.isNotEmpty() && menuItem.excludedLocations.contains(orderId)) {
                    excludedItems.add(menuItem)
                }
            }
        }
        return items.size == excludedItems.size
    }
}

enum class ScrollDirection {
    Up,
    Down,
    Idle
}