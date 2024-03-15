package com.herfy.menu.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.analytics.FirebaseAnalytics
import com.herfy.R
import com.herfy.analytics.FirebaseECommerceEventUtil
import com.herfy.analytics.facebook.FacebookAppEventUtil
import com.herfy.base.BaseActivity
import com.herfy.base.BaseFragment
import com.herfy.ext.cast
import com.herfy.firebase.FirebaseService
import com.herfy.main.MainActivity
import com.herfy.main.NavigationActivity
import com.herfy.menu.MenuContract
import com.herfy.menu.MenuPresenter
import com.herfy.menu.adapters.*
import com.herfy.menu.viewholders.BaseModifierItemViewHolder
import com.herfy.newmenu.NewMenuFragmentCommonListener
import com.herfy.nutritiondetails.NutritionInfoFragmentBottomSheet
import com.herfy.nutritiondetails.NutritionInfoFragmentBottomSheet.Companion.NAV_ARGS_INFO_KEY
import com.herfy.nutritiondetails.NutritionInfoParams
import com.herfy.productpagedetails.databinding.FragmentMenuItemDetailsBinding
import com.herfy.util.*
import com.herfy.utils.constants.Text
import com.herfy.utils.constants.Values
import com.herfy.utils.extensions.gone
import com.herfy.utils.extensions.invisible
import com.herfy.utils.extensions.ninjaTap
import com.herfy.utils.extensions.setVisible
import com.herfy.utils.extensions.visible
import com.herfy.utils.helper.AuthUtil
import com.herfy.utils.helper.BuildUtil
import com.herfy.utils.helper.CacheUtil
import com.herfy.utils.helper.CacheUtilHelper
import com.herfy.utils.helper.LocaleUtil
import com.herfy.utils.helper.viewLifecycleScope
import com.herfy.utils.menu.MenuUtil
import com.kochava.tracker.events.EventType
import com.skylinedynamics.solosdk.api.models.objects.*
import com.skylinedynamics.solosdk.util.GsonHelper
import io.shipbook.shipbooksdk.Log
import java.util.*


class MenuItemFragment : BaseFragment(), MenuContract.View {

    private lateinit var binding: FragmentMenuItemDetailsBinding

    private val args: MenuItemFragmentArgs by navArgs()

    private lateinit var menuPresenter: MenuContract.Presenter
    private var cartMenuItem = -1
    private var fromFavorite = false

    private var originalMenuItem: String? = null
    private var sizeId: String? = null
    private var canChange = false
    private var canChangeSize = false
    private var comboMainYourWayListAdapter: ComboMainYourWayListAdapter? = null
    private var combosListAdapter: CombosListAdapter? = null
    private var previousMenuItem: MenuItem? = null
    private var sandwichMenuItem: MenuItem? = null
    private var menuItemSelected: MenuItem? = null
    private var menuCategorySelected: MenuCategory? = null
    private var sandwichId = ""
    private var sizesAdapter: SizesRecyclerViewAdapter? = null
    private var modifierGroupsAdapter: ModifierGroupsNewRecyclerViewAdapter? = null
    private var frequentBoughItemsAdapter: FrequentBoughItemsAdapter? = null
    private var ingredientsAdapter: IngredientsRecyclerViewAdapter? = null
    private var initialModifierId = ""
    private var listener: NewMenuFragmentCommonListener? = null
    private var shouldReset = false
    private var lastPosition = 0
    private var viewEventSent = false
    private var _totalPrice = 0.0
    private var calorieCount = 0;
    private val cacheUtilHelper by lazy {
        CacheUtilHelper(requireContext())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.cast<MainActivity> {
            listener = it
        }
        menuPresenter = MenuPresenter(this)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMenuItemDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { act ->
            val displayMetrics = DisplayMetrics()
            act.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val toolbarHeight = (displayMetrics.widthPixels / 1.1).toInt()
            binding.mainToolbar.layoutParams.height = toolbarHeight
            binding.image.layoutParams.height = toolbarHeight
            binding.mainToolbar.invalidate()
            binding.image.invalidate()
        }
        menuPresenter.start()

        initialModifierId = ""
        activity?.cast<BaseActivity> {
            if (BuildUtil.soloAppKey().equals("SpeWD37HZx9Uqora", ignoreCase = true)) {
                it.showLoadingDialog()
            }
        }
        if (!args.isPromo) {
            if (args.menuItemId.isNotEmpty()) {
                menuPresenter.setFromCartEdit(false)
                if (args.menuCategoryId.isNotEmpty()) {
                    menuPresenter.setMenuCategory(args.menuCategoryId)
                }
                val isUpsell = args.isUpsell
                if(isUpsell){
                    menuPresenter.getMenuItem(false, args.menuItemId, isUpsell)
                }else {
                    menuPresenter.getMenuItem(false, args.menuItemId)
                }
            } else if (args.cartMenuItemPosition != -1) {
                cartMenuItem = args.cartMenuItemPosition
                val menuItems: List<MenuItem>? = CacheUtil.getInstance().cart
                if (menuItems != null && cartMenuItem < menuItems.size) {
                    val menuItem = menuItems[cartMenuItem]
                    menuPresenter.setFromCartEdit(true)
                    menuPresenter.setMenuCategory(menuItem)
                    menuPresenter.menuItem = menuItem
                    setMenuItemDetails(menuPresenter.menuItem)
                    setMenuItem(menuPresenter.menuCategory, menuPresenter.menuItem)
                    menuPresenter.showHideModifierItemPrice()
                }
            }
        } else {
            val menuItem: MenuItem? = if (!args.isEdit) {
                if (CacheUtil.getInstance().couponToAdd != null) {
                    CacheUtil.getInstance().couponToAdd.menuItem
                } else if (CacheUtil.getInstance().campaignToAdd != null) {
                    CacheUtil.getInstance().campaignToAdd.menuItem
                } else {
                    null
                }
            } else {
                if (CacheUtil.getInstance().couponCode != null &&
                    CacheUtil.getInstance().couponMenuItem != null
                ) {
                    CacheUtil.getInstance().couponMenuItem
                } else if (CacheUtil.getInstance().campaign != null &&
                    CacheUtil.getInstance().campaignPromotionMenuItem != null
                ) {
                    CacheUtil.getInstance().campaignPromotionMenuItem
                } else {
                    null
                }
            }
            if (menuItem != null) {
                menuPresenter.setFromCartEdit(args.isEdit)
                menuPresenter.setMenuCategory(menuItem)
                menuPresenter.menuItem = menuItem
                setMenuItemDetails(menuPresenter.menuItem)
                setMenuItem(menuPresenter.menuCategory, menuPresenter.menuItem)
                menuPresenter.showHideModifierItemPrice()
            }
        }
    }

    private fun setMenuItemCalorieCount(menuItem: MenuItem) {
        calorieCount = 0
        menuItem.let {
            if (it.sizes.modifierItems.size <= 3) {
                for (modifierItem in it.sizes.modifierItems) {
                    if (modifierItem.attributes.isSelected) {
                        calorieCount = modifierItem.attributes.calorieCount
                        break
                    }
                }
            }

            if (calorieCount == 0) {
                val caloriesValue = it.calories
                val calorieCount = it.calorieCount
                if (calorieCount != 0) {
                    this@MenuItemFragment.calorieCount = calorieCount
                } else if (caloriesValue != null && caloriesValue.isNotEmpty() && caloriesValue.trim { it <= ' ' } != "null" && caloriesValue.trim { it <= ' ' } != "0") {
//                    binding.calories.text = "(${
//                        CacheUtil.getInstance().getTranslations(Text.CALORIES_LABEL, "(x) cal")
//                            .replace("(x)", caloriesValue)
//                    })"
                }
            }
            var caloriesFromModifiers = 0
            for (modifierGroup in it.modifierGroups) {
                for (modifierItem in modifierGroup.modifierItems) {
                    var calorieCountPerModifier = 0

                    if (modifierItem.attributes.isSelected && modifierItem.attributes.calorieCount > 0) {
                        if (modifierItem.attributes.quantity > 0) {
                            calorieCountPerModifier =
                                modifierItem.attributes.calorieCount * modifierItem.attributes.quantity
                        } else {
                            calorieCountPerModifier = modifierItem.attributes.calorieCount
                        }
                    }
                    if (calorieCountPerModifier > 0) {
                        caloriesFromModifiers += calorieCountPerModifier
                    }
                }
            }

            if (caloriesFromModifiers > 0) {
                calorieCount += caloriesFromModifiers;
            }

            if (calorieCount > 0) {
//                binding.calories.text = "(${
//                    CacheUtil.getInstance().getTranslations(Text.CALORIES_LABEL, "(x) cal")
//                        .replace("(x)", calorieCount.toString())
//                })"
            }


        }


    }

    override fun setPresenter(presenter: MenuContract.Presenter) {
        menuPresenter = presenter
    }

    override fun logEvent(
        eventName: String?,
        eventAttributes: HashMap<String, String>?,
        eventMetricName: String?,
        eventMetricValue: Double
    ) {
        activity?.cast<BaseActivity> {
            it.logEventAttributesMetric(
                eventName,
                eventAttributes,
                eventMetricName,
                eventMetricValue
            )
        }
    }

    override fun setCartExpiry() {
        activity?.cast<BaseActivity> {
            it.setCartExpiry()
        }
    }

    override fun setupViews() {
//        binding.appbar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
//            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
//                binding.toolbar.apply {
//                    when(state) {
//                        State.EXPANDED ->
//                            setBackgroundResource(R.drawable.bg_menu_item_background)
//                        State.COLLAPSED ->
//                            setBackgroundColor(Color.parseColor("#F7F7F7"))
//                        else -> {}
//                    }
//                }
//            }
//        })

        try {
            val activity: Activity? = activity
            if (activity != null) {
                binding.sizesRecyclerView.layoutManager = LinearLayoutManager(activity)
                binding.frequentlyBoughRecycleview.layoutManager = LinearLayoutManager(activity)
                binding.modifierGroupsRecyclerView.layoutManager = LinearLayoutManager(activity)
                (binding.modifierGroupsRecyclerView.itemAnimator as SimpleItemAnimator?)!!.supportsChangeAnimations =
                    false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.icClose.setOnClickListener {
           requireActivity().onBackPressed()
        }
        if (CacheUtil.getInstance().concept.isCartItemsNotesEnabled) {
            binding.notesLabel.visibility = View.VISIBLE
            binding.notesContainer.visibility = View.VISIBLE
            binding.notesLabel.text = CacheUtil.getInstance()
                .getTranslations(Text.SPECIAL_INSTRUCTION, "Special Instruction")
            binding.notesDescription.text = CacheUtil.getInstance()
                .getTranslations(Text.NOTES_DESCRIPTION,"Please let us know if you are allergic to anything or if we need to avoid anything." )

            binding.etItemNotes.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val inputMethodManager =
                        requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(binding.etItemNotes.windowToken, 0)
                    return@OnEditorActionListener true
                }
                false
            })

        } else {
            binding.notesLabel.visibility = View.GONE
            binding.notesContainer.visibility = View.GONE
        }

        if (args.isPromo) {
            binding.minus.invisible()
            binding.plus.invisible()
        } else {
            binding.minus.visible()
            binding.plus.visible()
        }
        binding.minus.setOnClickListener { menuPresenter.changeMenuItemQuantity(false) }
        binding.plus.setOnClickListener { menuPresenter.changeMenuItemQuantity(true) }
    }

    override fun setupFonts() {}
    override fun setupTranslations() {
        binding.txtMoreInfo.text = CacheUtil.getInstance()
            .getTranslations(Text.MORE_INFO, "More Info")
        val originalQuantityWithColon = CacheUtil.getInstance().getTranslations(Text.ITEM_QUANTITY,"Item quantity")
        val formattedQuantityWithoutColon = originalQuantityWithColon.replace(":", "")
        binding.txtQuantity.text = formattedQuantityWithoutColon
        binding.txtFrequntlyBought.text = CacheUtil.getInstance().getTranslations(Text.FREQUENTLY_BOUGHT,"Frequently bought together")
        binding.notesLabel.text = CacheUtil
            .getInstance().getTranslations(Text.SPECIAL_INSTRUCTION,"Special Instruction")
    }

    override fun showStore(store: Store) {}
    override fun showStoreOpen(open: Boolean, message: String) {}
    override fun showFrequentBoughtItems(frequentBoughtItems: MutableList<MenuItem>?) {
        frequentBoughItemsAdapter?.notifyDataSetChanged()
    }

    override fun showAddress(address: String) {}
    override fun reloadMenu() {}
    override fun showMenuCategories(
        refresh: Boolean,
        isFromInitialLoad: Boolean,
        menuCategories: MutableList<MenuCategory>?
    ) {
    }

    override fun selectMenuCategory(position: Int, menuCategory: MenuCategory) {}
    override fun selectMenuItem(menuCategory: MenuCategory?, menuItem: MenuItem?) {}
    override fun setMenuCategory(menuCategory: MenuCategory, selected: Boolean) {}
    override fun setMenuItemDetails(menuItem: MenuItem) {
        menuPresenter.getFrequentBoughtItemsByMenuId(1,menuItem.id)
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
//                binding.mealDealsContainer.visibility = View.GONE
                binding.txtMoreInfo.isVisible = !menuItem.customFieldObjectMap.nutritionInfoImageUrl.isNullOrEmpty()
                frequentBoughItemsAdapter = FrequentBoughItemsAdapter(requireContext(),menuPresenter)
                frequentBoughItemsAdapter?.notifyDataSetChanged()
                binding.frequentlyBoughRecycleview.layoutManager = LinearLayoutManager(requireContext())

                binding.frequentlyBoughRecycleview.adapter = frequentBoughItemsAdapter
                binding.txtMoreInfo.ninjaTap(viewLifecycleScope) {
                    val bundle = Bundle()
                    bundle.putParcelable(NAV_ARGS_INFO_KEY,  NutritionInfoParams(
                        nutritionImageUrl = menuItem.customFieldObjectMap.nutritionInfoImageUrl.orEmpty(),
                        nutritionStory = menuItem.customFieldObjectMap.nutritionInfoStory.orEmpty(),
                        menuName = menuItem.getName(LocaleUtil.getInstance().language)
                    )
                    )
                    NutritionInfoFragmentBottomSheet.newInstance(bundle).show(childFragmentManager,NutritionInfoFragmentBottomSheet.TAG)
                }
                if (menuItem.attributes.notes.isNotEmpty()) {
                    binding.etItemNotes.setText(menuItem.attributes.notes)
                }

                canChange = false
                var imageUrl = menuItem.image
                if (imageUrl == null || imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) || imageUrl.isEmpty() || imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    imageUrl = Values.DEFAULT_IMAGE_URL
                }
                val requestOptions: RequestOptions
                if (BuildUtil.soloAppKey().equals("kExl05bOT4ttFEO2", ignoreCase = true)) {
                    requestOptions = RequestOptions.centerInsideTransform()
                    binding.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
                } else {
                    requestOptions = RequestOptions.centerCropTransform()
                    binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
                }
                Glide.with(it)
                    .load(imageUrl)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(requestOptions)
                    .listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
//                            binding.imageShimmer.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any,
                            target: Target<Drawable?>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
//                            binding.imageShimmer.visibility = View.GONE
                            return false
                        }
                    })
                    .into(binding.image)

                binding.name.text = menuItem.getName(LocaleUtil.getInstance().language)
                val caloriesValue = menuItem.calories
                val calorieCount = menuItem.calorieCount
                if (caloriesValue != null && !caloriesValue.isEmpty() && caloriesValue.trim { it <= ' ' } != "null" && caloriesValue.trim { it <= ' ' } != "0") {
                    binding.calories.text = MenuUtil.getCaloriesText(caloriesValue, calorieCount)
                    binding.calories.visibility = View.VISIBLE
                } else if (calorieCount != 0) {
                    binding.calories.text = MenuUtil.getCaloriesText(caloriesValue, calorieCount)
                    binding.calories.visibility = View.VISIBLE
                } else {
                    binding.calories.visibility = View.GONE
                }
                val descriptionValue = menuItem.getDescription(LocaleUtil.getInstance().language)
                if (descriptionValue == null || descriptionValue.isEmpty() || descriptionValue == "null") {
                    binding.description.visibility = View.GONE
                } else {
                    binding.description.text = descriptionValue
                    binding.description.visibility = View.VISIBLE
                }
//                binding.detailsShimmer.visibility = View.GONE
            }
        }
    }


    override fun setMenuItem(menuCategory: MenuCategory, menuItem: MenuItem) {
        activity?.cast<BaseActivity> { baseActivity ->
            baseActivity.runOnUiThread {
                if (menuItem.attributes.notes.isNotEmpty()) {
                    binding.etItemNotes.setText(menuItem.attributes.notes)
                }

                binding.favoriteContainer.setOnClickListener {
                    baseActivity.showLoadingDialog()
                    menuPresenter!!.addRemoveFavorite(false)
                }
                setFavorite(menuItem)
                binding.etItemNotes
                    .setHint(
                        CacheUtil
                            .getInstance()
                            .getTranslations(Text.NOTE_FOR_MENU_HINT, "e.g. no mayo")
                    )
                if (!args.isPromo && AuthUtil.getInstance().isSignedIn && !menuItem.isCombo) {
                    binding.favoriteCard.visible()
                } else {
                    binding.favoriteCard.invisible()
                }
                val eventAttributes = HashMap<String, String>()
                eventAttributes["Item ID"] = menuItem.id
                eventAttributes["Item Name"] = menuItem.attributes.name
                logEvent("ItemView", eventAttributes, "", 0.0)
                canChange = false
                if (menuItem.isCombo || menuItem.isSandwichOnly) {
                    if (menuItem.customFieldObjectMap.isNotItemable && !menuItem.isSandwichOnly) {
//                        binding.mealDealsContainer.visibility = View.GONE
                    } else {
//                        binding.mealDealsContainer.visibility = View.VISIBLE
                        if (menuItem.isSandwichOnly) {
//                            if (!LocaleUtil.getInstance().isArabic) binding.mealDealsTabs.buttonType.setPosition(
//                                1,
//                                false
//                            ) else binding.mealDealsTabs.buttonType.setPosition(0, false)
                        } else {
//                            if (!LocaleUtil.getInstance().isArabic) binding.mealDealsTabs.buttonType.setPosition(
//                                0,
//                                false
//                            ) else binding.mealDealsTabs.buttonType.setPosition(1, false)
                        }
//                        binding.mealDealsTabs.buttonType.onPositionChangedListener =
//                            OnPositionChangedListener { position: Int ->
//                                when (position) {
//                                    0 -> if (canChange) {
//                                        if (!LocaleUtil.getInstance().isArabic) {
//                                            changeMealDeal(false)
//                                        } else {
//                                            changeMealDeal(true)
//                                        }
//                                    }
//
//                                    1 -> if (canChange) {
//                                        if (LocaleUtil.getInstance().isArabic) {
//                                            changeMealDeal(false)
//                                        } else {
//                                            changeMealDeal(true)
//                                        }
//                                    }
//                                }
//                            }
                    }
                }
                canChange = true
                canChangeSize = false
                setSizes(menuItem)
                canChangeSize = true

                if (shouldReset || menuPresenter!!.modifierGroupsCount > 0) {
                    binding.modifierGroupsRecyclerView.visibility = View.VISIBLE
                    val showModifierItemPrice = HashMap<Int, Boolean>()
                    for (group in menuItem.modifierGroups) {
                        val indexOfGroup = menuItem.modifierGroups.indexOf(group)
                        showModifierItemPrice[indexOfGroup] =
                            group.attributes.freeModifiersCount == 0
                    }
                    val shouldShowModifierCheckbox = HashMap<Int, Boolean>()
                    menuItem.modifierGroups.forEachIndexed { index, group ->
                        shouldShowModifierCheckbox[index] =
                            group.modifierItems.any { it.attributes.isSelected }
                    }
                    shouldReset = false
                    modifierGroupsAdapter =
                        ModifierGroupsNewRecyclerViewAdapter(
                            activity,
                            menuPresenter,
                            showModifierItemPrice,
                            shouldShowModifierCheckbox,
                        )
                    modifierGroupsAdapter!!.setOnAction(object :
                        BaseModifierItemViewHolder.OnAction {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onCheckedChanged(
                            isSelected: Boolean,
                            modifierGroupPosition: Int,
                            modifierGroup: ModifierGroup,
                            modifierItemPosition: Int,
                            modifierItem: ModifierItem
                        ) {

                            Log.d(
                                "OnAction", "onCheckedChanged - isSelected: " + isSelected +
                                        ", modifierGroupPosition: " + modifierGroupPosition +
                                        ", modifierGroup: " + modifierGroup +
                                        ", modifierItemPosition: " + modifierItemPosition +
                                        ", modifierItem: " + modifierItem.name
                            );

                            menuPresenter!!.selectModifierItem(
                                isSelected,
                                modifierGroupPosition,
                                modifierGroup,
                                modifierItemPosition,
                                modifierItem
                            )
                            menuPresenter.updateModifierGroupSelectionIndicator(
                                isSelected, modifierGroupPosition,
                            )
                            setSizes(menuItem)
                            shouldReset = false
                            if (modifierGroupsAdapter != null) {
                                modifierGroupsAdapter!!.notifyDataSetChanged()
                            }


                        }

                        override fun onQuantityChanged(
                            add: Boolean,
                            modifierGroupPosition: Int,
                            modifierGroup: ModifierGroup,
                            modifierItemPosition: Int,
                            modifierItem: ModifierItem,
                            isQuantityVisible: Boolean,
                            clickCount: Int
                        ) {
                            menuPresenter!!.changeModifierItemQuantity(
                                add,
                                modifierGroupPosition,
                                modifierGroup,
                                modifierItemPosition,
                                modifierItem,
                                isQuantityVisible,
                                clickCount
                            )
                            setSizes(menuItem)
                            shouldReset = false

                            //Trigger calorie count
                        }
                    })
                    if (modifierGroupsAdapter != null) {
                        println("Modifier group test")
                        modifierGroupsAdapter!!.notifyDataSetChanged()
                    }
                    println("Modifier group count ${modifierGroupsAdapter?.itemCount}")
                    binding.modifierGroupsRecyclerView.visibility = View.VISIBLE
                    binding.modifierGroupsRecyclerView.setHasFixedSize(true)
                    ViewCompat.setNestedScrollingEnabled(binding.modifierGroupsRecyclerView, false)
                    binding.modifierGroupsRecyclerView.layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    binding.modifierGroupsRecyclerView.adapter = modifierGroupsAdapter
//
                } else {
                    println("Modifier gorup item count disabled")
                    binding.modifierGroupsRecyclerView.visibility = View.GONE
                    binding.modifierGroupsRecyclerView.visibility = View.GONE
                }
                binding.minus.isFocusable = menuItem.attributes.quantity != 1
                binding.minus.isClickable = menuItem.attributes.quantity != 1
                // todo revert this once needed
//                if (menuItem.attributes.quantity != 1) {
//                    binding.minus.setColorFilter(
//                        Color.parseColor(CacheUtil.getInstance().colorMain),
//                        PorterDuff.Mode.SRC_IN
//                    )
//                } else {
//                    binding.minus.setColorFilter(
//                        Color.parseColor("#BEBEBE"),
//                        PorterDuff.Mode.SRC_IN
//                    )
//                }
                setMenuItemQuantity(menuItem.attributes.quantity)
                var addToCart = ""
                if (cartMenuItem >= 0 || args.isEdit) {
                    binding.add.text = CacheUtil.getInstance().getTranslations(Text.UPDATE)
                } else {


                    menuPresenter!!.getMenuItemTotalPrice(menuPresenter!!.menuItem, -1)
                    binding.coordinatorLayout.visibility = View.VISIBLE
                    binding.footer.visibility = View.VISIBLE
                    if (CacheUtil.getInstance().application.attributes.menuOnly) {
//                    binding.add.backgroundTintList = ColorStateList.valueOf(
//                        ContextCompat.getColor(
//                            requireContext(),
//                            R.color.disabled_tint
//                        )
//                    )
                    } else {
//                    binding.add.backgroundTintList = ColorStateList.valueOf(
//                        ContextCompat.getColor(
//                            requireContext(),
//                            R.color.primary_tint
//                        )
//                    )
//                }
                    }
                    binding.add.setOnClickListener(View.OnClickListener {
                        if (CacheUtil.getInstance().application.attributes.menuOnly) {
                            FirebaseService.newNotification(
                                CacheUtil.getInstance().getTranslations(
                                    Text.MENU_ONLY_MESSAGE,
                                    "Sorry, ordering is not available at the moment"
                                ), binding.add.context
                            )
                            return@OnClickListener
                        }
                        var itemNote = binding.etItemNotes.text.toString() ?: ""
                        if (itemNote.isEmpty()) {
                            menuPresenter.setItemNote("")
                        } else {
                            menuPresenter.setItemNote(itemNote)
                        }

                        if (!args.isPromo) {
                            if (cartMenuItem >= 0) {
                                menuPresenter.updateCartItem(false, cartMenuItem)
                            } else {
                                menuPresenter.getCartMenuItem()
                            }
                        } else {
                            menuPresenter.updateCartItem(true, cartMenuItem)
                        }
                        FirebaseECommerceEventUtil.addToCartEvent(
                            activity,
                            menuItem,
                            menuCategory,
                            _totalPrice,
                            Integer.parseInt(binding.quantity.text.toString())
                        );
                        FacebookAppEventUtil.addToCartEvent(baseActivity, menuItem, _totalPrice)
                        com.herfy.analytics.kochava.KochavaTrackerUtil.trackEvent(EventType.ADD_TO_CART)
                    })
                    baseActivity.dismissDialogs()
                    menuItemSelected = menuItem
                    menuCategorySelected = menuCategory
                    baseActivity.dismissDialogs()
                    menuItemSelected = menuItem
                    menuCategorySelected = menuCategory
//                setupCombos(menuItem)
                    menuPresenter!!.getMenuItemTotalPrice(menuPresenter!!.menuItem, -1)
                    if (!menuItem.isSandwichOnly) {
                        previousMenuItem = menuItem
                    }
                    if (CacheUtil.getOrderStoreId() != null && menuItem.outOfStockLocations.contains(
                            CacheUtil.getOrderStoreId()
                        )
                    ) {
//                    binding.soldout.text =
//                        CacheUtil.getInstance().getTranslations(Text.SOLD_OUT_CAPS, "SOLD OUT")
//                    binding.soldout.visibility = View.VISIBLE
                        binding.image.setColorFilter(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.soldout
                            )
                        )
//                    binding.add.backgroundTintList = ColorStateList.valueOf(
//                        ContextCompat.getColor(
//                            requireContext(),
//                            R.color.disabled_tint
//                        )
//                    )
                        binding.add.isEnabled = false
                    }
//                if (menuItem.listPrice > 0.0) {
//                    binding.discountIndicator.visibility = View.VISIBLE
//                } else {
//                    binding.discountIndicator.visibility = View.GONE
//                }

//                binding.componentsShimmer.visibility = View.GONE
                    var imageUrl = menuItem.image
                    if (imageUrl == null || imageUrl.equals(
                            "null",
                            ignoreCase = true
                        ) || imageUrl.isEmpty() || imageUrl.lowercase(
                            Locale.getDefault()
                        ).contains("default-image.png")
                    ) {
                        imageUrl = Values.DEFAULT_IMAGE_URL
                    }
                    val requestOptions: RequestOptions
                    if (BuildUtil.soloAppKey().equals("kExl05bOT4ttFEO2", ignoreCase = true)) {
                        requestOptions = RequestOptions.centerInsideTransform()
                        binding.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    } else {
                        requestOptions = RequestOptions.centerCropTransform()
                        binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    Glide.with(baseActivity)
                        .load(imageUrl)
                        .skipMemoryCache(false)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .apply(requestOptions)
                        .listener(object : RequestListener<Drawable?> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any,
                                target: Target<Drawable?>,
                                isFirstResource: Boolean
                            ): Boolean {
//                            binding.imageShimmer.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any,
                                target: Target<Drawable?>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
//                            binding.imageShimmer.visibility = View.GONE
                                return false
                            }
                        })
                        .into(binding.image)

                    if (!viewEventSent) {
                        viewEventSent = true
                        FirebaseECommerceEventUtil.viewItemEvent(activity, menuItem, menuCategory)
                        FacebookAppEventUtil.viewMenuItemEvent(activity, menuItem)
                    }
                }
            }
        }
    }


    private fun changeMealDeal(isSandwich: Boolean) {
        val menuItem1: MenuItem? = if (previousMenuItem != null) {
            previousMenuItem
        } else {
            GsonHelper.gsonFinal().fromJson(
                originalMenuItem,
                MenuItem::class.java
            )
        }
        val item: MenuItem? = if (sandwichMenuItem != null) {
            sandwichMenuItem
        } else {
            menuPresenter!!.getMenuItemSingle(sandwichId)
        }
        if (item != null && isSandwich) {
            item.isSandwichOnly = true
            sandwichMenuItem = item
            menuItemSelected = item
        } else {
            menuItem1?.isSandwichOnly = false
            menuItemSelected = menuItem1
        }
        menuPresenter!!.menuItem = menuItemSelected
        setMenuItem(menuCategorySelected!!, menuItemSelected!!)
    }

//    private fun setupCombos(menuItem: MenuItem) {
//        if (!menuItemSelected!!.isCombo) {
//            binding.combosList.visibility = View.GONE
//            binding.yourWayList.visibility = View.GONE
//            return
//        }
//        if (menuItemSelected!!.combo.isEmpty()) {
//            binding.yourWayList.visibility = View.GONE
//            binding.combosList.visibility = View.GONE
//            return
//        }
//        var comboIndex = 0
//        if (binding.sizesLabel.visibility == View.VISIBLE || binding.sizesCard.visibility == View.VISIBLE) {
//            var counter = 0
//            for (modifierItem in menuItemSelected!!.sizes.modifierItems) {
//                if (modifierItem.attributes.isSelected) {
//                    comboIndex = counter
//                    break
//                }
//                counter++
//            }
//        }
//        menuPresenter!!.assignComboIndex(comboIndex)
//        menuPresenter!!.assignMenuItem(menuItemSelected)
//        menuItemSelected!!.combo[comboIndex].components = menuPresenter!!.componentsSelectFirst(
//            menuItemSelected!!.combo[comboIndex].components
//        )
//        menuPresenter!!.assignMenuItem(menuItemSelected)
//        try {
//            for (i in menuItemSelected!!.combo[comboIndex].components.indices) {
//                if (menuItemSelected!!.combo[comboIndex].components[i].name.english.lowercase(Locale.getDefault())
//                        .contains("sandwich") || menuItemSelected!!.combo[comboIndex].components[i].name.french.lowercase(
//                        Locale.getDefault()
//                    ).contains("sandwich")
//                ) {
//                    binding.mealDealsTabs.sandwichOnlyButton.text =
//                        CacheUtil.getInstance().getTranslations(Text.SANDWICH_ONLY, "(x) only")
//                            .replace(
//                                "(x)",
//                                translated(
//                                    LocaleUtil.getInstance().language,
//                                    menuItemSelected!!.combo[comboIndex].components[i].name
//                                )
//                            )
//                    if (!menuItemSelected!!.combo[comboIndex].components[i].items.isEmpty()) sandwichId =
//                        menuItemSelected!!.combo[comboIndex].components[i].items[0].itemId.toString()
//                    break
//                }
//            }
//            val components = menuItemSelected!!.combo[comboIndex].components
//            val itemAnimator = binding.combosList.itemAnimator
//            if (itemAnimator != null) {
//                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
//            }
//            val activity: Activity? = activity
//            if (activity != null) {
//                combosListAdapter =
//                    CombosListAdapter(activity, components, menuPresenter, menuItemSelected)
//                binding.combosList.adapter = combosListAdapter
//                ViewCompat.setNestedScrollingEnabled(binding.combosList, false)
//                if (menuItemSelected!!.isSandwichOnly) {
//                    binding.combosList.visibility = View.GONE
//                } else {
//                    binding.combosList.visibility = View.VISIBLE
//                }
//                try {
//                    val state = binding.combosList.layoutManager!!.onSaveInstanceState()
//                    combosListAdapter!!.notifyDataSetChanged()
//                    binding.combosList.layoutManager!!.onRestoreInstanceState(state)
//                } catch (e: Exception) {
//                    combosListAdapter!!.notifyDataSetChanged()
//                    e.printStackTrace()
//                }
//
//                //setupYourWay(components);
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    fun translated(language: String, name: Translated): String {
        return if (language.equals("en-us", ignoreCase = true)) {
            name.english
        } else {
            if (language.equals("fr", ignoreCase = true)) name.french else name.arabic
        }
    }


    override fun setFavorite(menuItem: MenuItem) {
        if (!args.isPromo && AuthUtil.getInstance().isSignedIn && CacheUtil.getInstance().favorites.contains(
                menuItem.id
            )
        ) {
            binding.favoriteIcon.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            binding.favoriteIcon.setImageResource(R.drawable.ic_heart_unselected)
        }
    }

    override fun showSearchedMenuItems(searchedMenuItems: ArrayList<MenuItem>) {}
    override fun hideSearchedMenuItems(menuCategory: MenuCategory) {}


    @SuppressLint("NotifyDataSetChanged")
    override fun setSizes(menuItem: MenuItem) {

        var showSizes = true
        val visibilityConfig = menuItem.sizes.attributes.visibilityConfig
        if (visibilityConfig != null && visibilityConfig.initialValue != null && visibilityConfig.initialValue.equals(
                "hidden",
                ignoreCase = true
            ) && !menuItem.sizes.attributes.type.equals("drinks", ignoreCase = true)
        ) {
            showSizes = false
        }
        var initialSelectedEncountered = false
        var countSelected = 0
        if (showSizes) {
            binding.sizesContainer.removeAllViews()
            if (menuItem.sizes.modifierItems.size > 1) {
                binding.sizesLabel.visible()

                binding.checkBox.setVisible(true)
                binding.sizesLabel.text = menuItem.sizes.attributes.name
                val imageUrl = menuItem.sizes.attributes.image
                if (imageUrl != null && !imageUrl.equals(
                        "null",
                        ignoreCase = true
                    ) && !imageUrl.isEmpty() && !imageUrl.lowercase(
                        Locale.getDefault()
                    ).contains("default-image.png")
                ) {
                    Glide.with(requireContext()).load(imageUrl).apply(RequestOptions.centerCropTransform())
                        .placeholder(R.drawable.imagepicker_image_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL).into(binding.sizesImage)
                }

                if (menuItem.sizes.modifierItems.size <= 3) {
                    menuItem.sizes.modifierItems.forEach { modifierItem ->
                        val view = LayoutInflater.from(activity)
                            .inflate(R.layout.item_new_sizes_fixed, binding.sizesContainer, false)
                        view.tag = modifierItem.id
                        val card: MaterialCardView = view.findViewById(R.id.card)
                        val txtPrice =
                            card.findViewById<MaterialTextView>(R.id.txtSizesPrice)
                        val txtSizesName =
                            card.findViewById<MaterialTextView>(R.id.name)
                        val sizesRadiButton =
                            card.findViewById<ImageView>(R.id.radioButton)
                        if (modifierItem.getAttributes().isSelected) {
                            if (!initialSelectedEncountered) {
                                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.color_blue)
                                card.strokeWidth = ViewUtil.dpToPx(requireActivity(), 1)
                                card.radius = resources.getDimensionPixelSize(R.dimen.radius_large).toFloat()
                                txtSizesName.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                                txtPrice.setTextColor(ContextCompat.getColor(requireContext(),R.color.white))
                                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_blue))
                                sizesRadiButton.setImageResource(R.drawable.ic_size_drawable_checked)
                                initialSelectedEncountered =true
                            } else {
                                card.strokeColor = ContextCompat.getColor(requireContext(), R.color.color_light_blue_shade)
                                card.strokeWidth = ViewUtil.dpToPx(requireActivity(), 1)
                                card.radius = resources.getDimensionPixelSize(R.dimen.radius_large).toFloat()
                                txtSizesName.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                                txtPrice.setTextColor(ContextCompat.getColor(requireContext(),R.color.color_gray))
                                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_light_blue_shade))
                                sizesRadiButton.setImageResource(R.drawable.modifier_item_radio_unselected)
                            }

                        } else {
                            card.strokeColor = ContextCompat.getColor(requireContext(), R.color.color_light_blue_shade)
                            card.strokeWidth = ViewUtil.dpToPx(requireActivity(), 1)
                            card.radius = resources.getDimensionPixelSize(R.dimen.radius_large).toFloat()
                            txtSizesName.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            txtPrice.setTextColor(ContextCompat.getColor(requireContext(),R.color.color_gray))
                            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_light_blue_shade))
                            sizesRadiButton.setImageResource(R.drawable.modifier_item_radio_unselected)

                        }
                        val container = view!!.findViewById<LinearLayout>(R.id.container)
                        container.tag = modifierItem.getId()
                        container.setOnClickListener { view1 ->
                            for (i in 0 until binding.sizesContainer.childCount) {
                                val childView = binding.sizesContainer.getChildAt(i)
                                val childCard: MaterialCardView = childView.findViewById(R.id.card)
                                val txtModifierSizes =
                                    childView.findViewById<MaterialTextView>(R.id.name)
                                val txtModifierPrice = childView.findViewById<MaterialTextView>(R.id.txtSizesPrice)
                                val sizesRadioBtn = childView.findViewById<ImageView>(R.id.radioButton)
                                if (childView.tag == view1.tag) {
                                    childCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.color_blue)
                                    childCard.strokeWidth = ViewUtil.dpToPx(requireActivity(), 1)
                                    childCard.radius = resources.getDimensionPixelSize(R.dimen.radius_large).toFloat()
                                    txtModifierSizes.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_light_blue_shade))
                                    txtModifierPrice.setTextColor(ContextCompat.getColor(requireContext(),R.color.white))
                                    childCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_blue))
                                    sizesRadioBtn.setImageResource(R.drawable.ic_size_drawable_checked)
                                    binding.checkBox.setVisible(true)

                                } else {
//
                                    childCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.color_light_blue_shade)
                                    childCard.strokeWidth = ViewUtil.dpToPx(requireActivity(), 1)
                                    childCard.radius = resources.getDimensionPixelSize(R.dimen.radius_large).toFloat()
                                    txtModifierSizes.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                                    txtModifierPrice.setTextColor(ContextCompat.getColor(requireContext(),R.color.color_gray))
                                    childCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_light_blue_shade))
                                    sizesRadioBtn.setImageResource(R.drawable.modifier_item_radio_unselected)

                                    }
                                }
                              menuPresenter.selectSize(view1.tag as String)
                            }

                        val txtModifierSizes = view.findViewById<MaterialTextView>(R.id.name)
                        val txtModifierPrice =
                            view.findViewById<MaterialTextView>(R.id.txtSizesPrice)
                        txtModifierPrice.setVisible(modifierItem.attributes.price != 0.0)
                        txtModifierPrice.text =
                            "+${StringUtil.getFormattedPrice(modifierItem.attributes.price)}"
                        txtModifierSizes.text = modifierItem.attributes.name
                        binding.sizesContainer.addView(view)
                    }
                    binding.sizesLayoutTextContainer.visible()
                    binding.sizesContainer.visible()
                    binding.viewSizesSeparator.visible()
                    binding.sizesRecyclerView.gone()
                } else {
                    try {
                        val activity: Activity? = activity
                        if (activity != null) {
                            sizesAdapter = SizesRecyclerViewAdapter(activity, menuPresenter)
                            sizesAdapter!!.setOnAction { modifierItem: ModifierItem ->
                                menuPresenter!!.selectSize(modifierItem.id)
                                sizesAdapter!!.notifyDataSetChanged()
                            }
                            binding.sizesRecyclerView.adapter = sizesAdapter
                            ViewCompat.setNestedScrollingEnabled(binding.sizesRecyclerView, false)
                            binding.sizesLabel.visibility = View.VISIBLE
                            binding.sizesRecyclerView.visibility = View.VISIBLE
                            binding.viewSizesSeparator.visible()
                            binding.sizesLayoutTextContainer.visible()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                binding.sizesLayoutTextContainer.gone()
                binding.sizesContainer.visibility = View.GONE
                binding.sizesRecyclerView.visibility = View.GONE
            }
            for (modifierItem in menuItem.sizes.modifierItems) {
                if (modifierItem.attributes.isSelected) {
                    sizeId = modifierItem.id
                    menuPresenter!!.setSelectedSizeId(sizeId)
                }
            }
        } else {
            binding.sizesLayoutTextContainer.gone()
            binding.sizesContainer.gone()
            binding.sizesRecyclerView.gone()
        }
    }

    override fun setModifierItem(
        modifierGroup: ModifierGroup,
        modifierItem: ModifierItem,
        position: Int
    ) {
    }

    override fun setModifierItem(
        modifierGroupPosition: Int,
        modifierGroup: ModifierGroup,
        modifierItemPosition: Int,
        modifierItem: ModifierItem,
        showModifierItemPrice: Boolean
    ) {
    }



    override fun setIngredient(ingredient: Ingredient) {}
    override fun setModifierGroup(
        position: Int,
        modifierGroup: ModifierGroup,
        showModifierItemPrice: Boolean,
        showShowModifierCheckBox: Boolean
    ) {
    }

    override fun setMenuItemQuantity(quantityValue: Int) {
        binding.minus.isFocusable = quantityValue != 1
        binding.minus.isClickable = quantityValue != 1

        // todo revert this once needed
//        if (quantityValue != 1) {
//            binding.minus.setColorFilter(
//                Color.parseColor(CacheUtil.getInstance().colorMain),
//                PorterDuff.Mode.SRC_IN
//            )
//        } else {
//            binding.minus.setColorFilter(Color.parseColor("#BEBEBE"), PorterDuff.Mode.SRC_IN)
//        }
        binding.quantity.text = StringUtil.localize(quantityValue.toString())
    }

    override fun setMenuItemTotalPrice(totalPrice: Double, menuItemPrice: Double) {
        setMenuItemCalorieCount(menuPresenter.menuItem)
        if (totalPrice <= 0.0 && args.isPromo) {
            binding.total.text = CacheUtil.getInstance().getTranslations(
                Text.FREE
            )
        } else {
            binding.total.text = StringUtil.getFormattedPrice(menuItemPrice)
            binding.add.text = CacheUtil.getInstance().getTranslations(Text.ADD_TO_BAG_AMOUNT, "Add to bag - (x)")
                .replace("(x)","${StringUtil.getFormattedPrice(totalPrice)}")

        }
        _totalPrice = totalPrice
    }

    override fun selectSize() {
        if (menuPresenter!!.selectedSizeId() != sizeId) {
            val menuItem = menuPresenter!!.correctSizeModifier(sizeId)
            menuPresenter!!.assignMenuItem(menuItem)
            setMenuItem(menuCategorySelected!!, menuItem)
        }
    }

    override fun showOrderType() {}
    override fun addCartItem(menuItem: MenuItem) {
        try {
            activity?.cast<MainActivity> {
                it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
            }
            activity?.cast<NavigationActivity> {
                it.setCartQuantity(CacheUtil.getInstance().cartQuantity)
            }
            activity?.cast<BaseActivity> {
                it.runOnUiThread {
                    val vibrator = it.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    500,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            vibrator.vibrate(100)
                        }
                    }
                    it.dismissDialogs()
                    Toast.makeText(
                        it,
                        CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART),
                        Toast.LENGTH_SHORT
                    ).show()
                    listener?.animateViewToCart(
                        binding.image,
                        menuItem.attributes.image,
                        ViewUtil.dpToPx(it, 16)
                    )
                    activity?.onBackPressed()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun addCartItem(menuItem: MenuItem, view: ImageView) {}
    override fun updateCartItem() {
        var message = CacheUtil.getInstance().getTranslations(Text.ITEM_UPDATED, "Item updated")
        if (args.isPromo && !args.isEdit) {
            message = CacheUtil.getInstance().getTranslations(Text.ITEM_ADDED_TO_CART, "Item added to cart")
        }
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
            it.onBackPressed()
            Toast.makeText(
                it,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun showMessage(message: String) {
        activity?.cast<BaseActivity> {
            it.dismissDialogs()
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun showError(error: String) {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                it.dismissDialogs()
                it.showAlertDialog("", error)
            }
        }
    }
    override fun updateFavoriteByPosition(
        position: Int,
        menuItem: MenuItem?
    ) {
        frequentBoughItemsAdapter?.notifyItemChanged(position,menuItem)
    }
    override fun showHideModifierItemPrice(show: Boolean, modifierGroupPosition: Int) {
        modifierGroupsAdapter!!.showModifierItemPrice(show, modifierGroupPosition)
    }

    override fun showModifierSelectionIndicator(
        hasAtleastOneSelectedItem: Boolean,
        modifierGroupPosition: Int
    ) {
       modifierGroupsAdapter!!.shouldShowCheckBox(hasAtleastOneSelectedItem,modifierGroupPosition)
    }

    override fun showFavorites(fromAddRemove: Boolean, favorites: ArrayList<Favorite>) {}
    override fun setFavorite(
        favorite: Favorite,
        outOfStockLocations: ArrayList<String>,
        discount: Double,
        specialPrice: Double
    ) {
    }

    override fun showMenuItem(categoryId: String, menuItemId: String) {}
    override fun showOrders(newestToOldest: Boolean, orders: ArrayList<OrderHistory>) {}
    override fun setOrder(order: OrderHistory) {}
    override fun selectOrder(order: OrderHistory) {}
    override fun showBestSellers(menuCategories: ArrayList<MenuItem>) {}
    override fun showLatestOffers(latestOffers: List<Campaign>) {}
    override fun showDigitalCoupon(digitalCoupon: Campaign) {}
    override fun setCoupon(campaign: Campaign) {}
    override fun setBestSeller(
        menuItem: MenuItem,
        outOfStockLocation: ArrayList<String>,
        discount: Double,
        specialPrice: Double
    ) {
    }

    override fun setRecentOrder(
        menuItem: MenuItem,
        outOfStockLocation: ArrayList<String>,
        discount: Double,
        specialPrice: Double
    ) {
    }

    override fun logFirebaseEvent(event: String) {
        if (Values.sDeepLinkUtm != null) {
            activity?.cast<BaseActivity> {
                val firebaseAnalytics = FirebaseAnalytics.getInstance(it)
                firebaseAnalytics.logEvent(event, Values.sDeepLinkUtm)
            }
        }
    }

    override fun saveToCache(key: String, value: String) {
        CacheHelper.save(requireContext(), key, value)
    }

    override fun getFromCache(key: String): String? {
        activity?.let {
            return CacheHelper.retrieve(it, key)
        }
        return null
    }

    override fun showLoader() {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                binding.coordinatorLayout.visibility = View.VISIBLE
//                binding.imageShimmer.visibility = View.VISIBLE
//                binding.detailsShimmer.visibility = View.VISIBLE
//                binding.componentsShimmer.visibility = View.VISIBLE
            }
        }
    }

    override fun updateComponents(
        componentPosition: Int,
        componentModifierItems: List<ComponentModifierItem>,
        previous: Int,
        selected: Int
    ) {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                if (combosListAdapter != null) {
                    combosListAdapter!!.updateComponents(
                        componentPosition,
                        componentModifierItems,
                        previous,
                        selected
                    )
                    menuPresenter!!.getMenuItemTotalPrice(menuPresenter!!.menuItem, -1)
                }
            }
        }
    }

    override fun updateComponentModifierGroups(
        componentPosition: Int,
        componentModifierItems: List<ComponentModifierItem>,
        selected: Int
    ) {
        activity?.cast<BaseActivity> {
            it.runOnUiThread {
                if (combosListAdapter != null) {
                    combosListAdapter!!.updateComponentModifierGroups(
                        componentPosition,
                        componentModifierItems,
                        selected
                    )
                    menuPresenter!!.getMenuItemTotalPrice(menuPresenter!!.menuItem, -1)
                }
            }
        }
    }

    override fun updateOrderTypeContainer(isPreselected: Boolean) {
    }

    override fun logAddToWishlistEvent(menuItem: MenuItem) {
        FacebookAppEventUtil.addToWishlistEvent(requireContext(), menuItem)
    }

    override fun setOriginalMenuItemValue(value: String?) {
        if (originalMenuItem == null) {
            originalMenuItem = value
        }
    }

    override fun showRecentOrders(menuItems: ArrayList<MenuItem>) {}

    override fun setMenuCategoryForAnalytics(
        menuItem: MenuItem?,
        menuCategory: MenuCategory?,
        price: Double,
        quantity: Int
    ) {
    }

    override fun updateFloatingTotalPrice() {

    }

}
