package com.herfy.subscription.premade.views;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.florent37.singledateandtimepicker.SingleDateAndTimePicker;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.order.views.AddressActivity;
import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.theme.DynamicTheme;
import com.skylinedynamics.solosdk.api.models.objects.Address;
import com.herfy.subscription.branch.PickupBranchActivity;
import com.herfy.subscription.premade.PreMadeStep1Contract;
import com.herfy.subscription.premade.PreMadeStep1Presenter;
import com.herfy.subscription.premade.adapter.DeliveryAddressAdapter;
import com.herfy.subscription.premade.adapter.DeliveryTimeAdapter;
import com.herfy.subscription.premade.adapter.MealTimeAdapter;
import com.herfy.subscription.premade.models.DeliveryTime;
import com.herfy.subscription.premade.models.StubPreMadeModel;
import com.herfy.subscription.subhome.views.NutritionFactsDialogFragment;
import com.herfy.utils.helper.AnimUtil;
import com.herfy.utils.helper.LocaleUtil;
import com.herfy.util.StringUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.suke.widget.SwitchButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PreMadeStep1Activity extends BaseActivity implements PreMadeStep1Contract.View, MealTimeAdapter.MealTimeItemListener, DeliveryTimeAdapter.DeliveryTimeListener, DeliveryAddressAdapter.DeliveryAddressListener {

    private PreMadeStep1Contract.Presenter preMadePresenter;
    private MealTimeAdapter adapter;
    private DeliveryTimeAdapter deliverTimeAdapter;
    private DeliveryAddressAdapter deliveryAddressAdapter;
    private final List<StubPreMadeModel> sList;
    private final List<DeliveryTime> dTimeList;
    private final List<Address> addressList;

    @BindView(R.id.sliding_panel_main)
    SlidingUpPanelLayout slidingPanelMain;
    @BindView(R.id.left_label)
    AppCompatTextView leftLabel;
    @BindView(R.id.left_layout)
    LinearLayout leftLayout;
    @BindView(R.id.options)
    AppCompatTextView options;
    @BindView(R.id.title)
    AppCompatTextView title;
    @BindView(R.id.plan_avg_cal)
    AppCompatTextView planAverageCal;
    @BindView(R.id.plan_days)
    AppCompatTextView planDays;
    @BindView(R.id.plan_image)
    ImageView planImage;
    @BindView(R.id.plan_name)
    AppCompatTextView planName;
    @BindView(R.id.plan_calories)
    AppCompatTextView planCalories;
    @BindView(R.id.plan_description)
    AppCompatTextView planDescription;
    @BindView(R.id.hdyw_label)
    AppCompatTextView hdywLabel;
    @BindView(R.id.total_label)
    AppCompatTextView totalLabel;
    @BindView(R.id.total_price)
    AppCompatTextView totalPrice;
    @BindView(R.id.total_vat_label)
    AppCompatTextView totalVatLabel;
    @BindView(R.id.see_meals)
    AppCompatTextView seeMeals;
    @BindView(R.id.exception_label)
    AppCompatTextView exceptionLabel;
    @BindView(R.id.deliveries_together_label)
    AppCompatTextView deliveriesTogetherLabel;
    @BindView(R.id.switch_button_dt)
    SwitchButton switchButton;
    @BindView(R.id.sliding_title)
    AppCompatTextView slidingTitle;
    @BindView(R.id.bottom_sliding_layout)
    LinearLayout bottomSlidingLayout;
    @BindView(R.id.cancel_button)
    AppCompatTextView cancelButton;
    @BindView(R.id.confirm_button)
    AppCompatTextView confirmButon;
    @BindView(R.id.delivery_address_layout)
    LinearLayout deliveryAddressLayout;
    @BindView(R.id.pickup_time_layout)
    FrameLayout pickupTimeLayout;
    @BindView(R.id.add_address_from_delivery_label)
    AppCompatTextView addAddressFromDeliveryLabel;
    @BindView(R.id.delivery_time_list)
    RecyclerView deliveryTimeList;
    @BindView(R.id.delivery_address_list)
    RecyclerView deliveryAddressList;
    @BindView(R.id.list_meal_plan)
    RecyclerView mealPlanList;
    @BindView(R.id.date_picker)
    SingleDateAndTimePicker datePicker;

    private int currentPosition;
    private boolean isTimeSelected;
    private boolean isPickupTab;
    private int currentSelectedPickup;
    private int currentSelectedPickupTime;
    private StubPreMadeModel currentSelectedMeal;
    private boolean isTime, isAddress, isFromConfirm;
    private String currentSelectedAddressId = "";
    private String currentSelectedName = "";
    private String selectedPickupId = "";
    private String selectedPickupBranch = "";
    private String dateSelected;
    private Date maxDate;
    private Date minDate;
    private Date selectedDate;


    public PreMadeStep1Activity() {
        sList = new ArrayList<>();
        addressList = new ArrayList<>();
        currentSelectedMeal = new StubPreMadeModel();
        dTimeList = new ArrayList<>();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premade_step1);
        ButterKnife.bind(this);
        initAdapterAndLayoutManager();
        setUpDatePicker();
        preMadePresenter = new PreMadeStep1Presenter(this);
        preMadePresenter.start();
        preMadePresenter.getList(true);
    }

    private void setUpDatePicker() {

        Calendar c = new GregorianCalendar();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        maxDate = c.getTime();

        Calendar minTime = Calendar.getInstance();

        minDate = minTime.getTime();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String strDate = dateFormat.format(minDate);
        dateSelected = strDate;
        if (selectedDate == null) {
            selectedDate = minDate;
        }

        datePicker.addOnDateChangedListener((displayed, date) -> {
            selectedDate = datePicker.getDate();
        });

        datePicker.setCustomLocale(new Locale(LocaleUtil.getInstance().getLanguage()));
        datePicker.setIsAmPm(true);
        datePicker.setDisplayDays(false);
        datePicker.setSelectorColor(android.R.color.white);
        datePicker.setDisplayYears(false);
        datePicker.setDefaultDate(minDate);
        datePicker.setTextSize(getResources().getDimensionPixelSize(company.tap.gosellapi.R.dimen.font_size_16sp));
        datePicker.setDisplayMonths(false);
        datePicker.setTextColor(this.getResources().getColor(R.color.silver_chalice));
        datePicker.setSelectedTextColor(Color.parseColor(CacheUtil.getInstance().getColorMain()));
        datePicker.setTypeface(FontHandler.getMediumTypeface());
        datePicker.setMaxDate(maxDate);
        datePicker.setMinDate(minDate);

    }

    private void getDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        dateSelected = dateFormat.format(date);
        sList.get(currentSelectedPickupTime).pickupTimeDone = true;
        sList.get(currentSelectedPickupTime).pickupTime = dateSelected;
        adapter.notifyDataSetChanged();
    }

    private void initAdapterAndLayoutManager() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        LinearLayoutManager dTimeLinearLayoutManager = new LinearLayoutManager(this);
        dTimeLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        LinearLayoutManager addressListLinearLayoutManager = new LinearLayoutManager(this);
        addressListLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mealPlanList.setLayoutManager(linearLayoutManager);
        deliveryTimeList.setLayoutManager(dTimeLinearLayoutManager);
        deliveryAddressList.setLayoutManager(addressListLinearLayoutManager);

        if (adapter == null) {
            adapter = new MealTimeAdapter(this, sList, this);
        }

        if (deliverTimeAdapter == null) {
            deliverTimeAdapter = new DeliveryTimeAdapter(this, dTimeList, this);
        }

        if (deliveryAddressAdapter == null) {
            deliveryAddressAdapter = new DeliveryAddressAdapter(this, addressList, this);
        }

        deliveryTimeList.setAdapter(deliverTimeAdapter);
        mealPlanList.setAdapter(adapter);
        deliveryAddressList.setAdapter(deliveryAddressAdapter);

    }

    @Override
    public void showList(List<StubPreMadeModel> list) {
        isPickupTab = false;
        currentPosition = -1;
        isTimeSelected = false;
        sList.clear();
        sList.addAll(list);
        adapter.notifyDataSetChanged();
    }

    private int currentDeliveryTimeSelected(String time) {
        for (int i = 0; i < dTimeList.size(); i++) {
            if (dTimeList.get(i).time.equals(time)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void showDeliveryTime(List<DeliveryTime> times) {
        dTimeList.clear();
        dTimeList.addAll(times);
        if (currentSelectedMeal.deliveryTimeDone) {
            deliverTimeAdapter.selectedTime = currentDeliveryTimeSelected(currentSelectedMeal.deliveryTime);
        }
        deliverTimeAdapter.notifyDataSetChanged();
        new Handler().postDelayed(() -> expandPanel(), 300);
    }

    @Override
    public void showAddresses(List<Address> addresses) {
        addressList.clear();
        addressList.addAll(addresses);
        deliveryAddressAdapter.selectedId = currentSelectedAddressId;
        deliveryAddressAdapter.notifyDataSetChanged();
    }

    @Override
    public void setPresenter(PreMadeStep1Contract.Presenter presenter) {
        preMadePresenter = presenter;
    }

    @Override
    public void setupViews() {

        String colorStr = DynamicTheme.getStringColor(this);

        leftLayout.setOnClickListener(v -> {
            finish();
        });

        switchButton.setOnCheckedChangeListener((view, isChecked) -> {
            preMadePresenter.getList(isChecked);
        });

        options.setOnClickListener(v -> {

        });

        planAverageCal.setOnClickListener(v -> {
            openNutritionFacts();
        });

        slidingPanelMain.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                switch ((newState)) {
                    case HIDDEN:
                    case COLLAPSED:
                        bottomSlidingLayout.setVisibility(View.GONE);
                        if (isFromConfirm) {
                            showSetDialog(isTime, isAddress);
                        }
                        isFromConfirm = false;
                        break;
                }
            }
        });

        //Stub data below
        planAverageCal.setText("3200 avg cal/meal");
        planDays.setText("20 days");
        planCalories.setText("(1654 avg. calories per meal)");
        planDescription.setText("2x meals per day  •  4x weeks  •  for 1x person" +
                "Effective Weight Loss Meals. Order tasty Keto or easy Low Carb meals. Effective for weight loss.");
        totalPrice.setText(StringUtil.getNewFormattedPrice(6051.99, true));
        planName.setText(Html.fromHtml("Keto Breakfast and Lunch Plan Lite " + "<font color=" + colorStr + ">(40 meals)</font>"));

    }

    private void openNutritionFacts() {
        Bundle args = new Bundle();
        args.putString("carbs", "62g");
        args.putString("fats", "42g");
        args.putString("calories", "8310");
        args.putString("protein", "25g");
        NutritionFactsDialogFragment fragment = NutritionFactsDialogFragment.newInstance();
        fragment.setArguments(args);
        fragment.show(getSupportFragmentManager(), NutritionFactsDialogFragment.class.getSimpleName());
    }

    private void goToPickUpPage(StubPreMadeModel menu) {
        Intent intent = new Intent(this, PickupBranchActivity.class);
        intent.putExtra("selectedId", menu.pickupSelectedId);
        startActivityForResult(intent, 103);
        overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
    }

    @Override
    public void setupTranslations() {
        leftLabel.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        exceptionLabel.setText(CacheUtil.getInstance().getTranslations(Text.YOU_CAN_MAKE_EXCEPTIONS_FOR_THIS_LATER_IN_STEP_2, "You can make exceptions for this later in step 2"));
        options.setText(CacheUtil.getInstance().getTranslations(Text.OPTIONS, "OPTIONS").replace(":", ""));
        title.setText(CacheUtil.getInstance().getTranslations(Text.PLAN_DETAILS, "PLAN DETAILS"));
        hdywLabel.setText(CacheUtil.getInstance().getTranslations(Text.HOW_DO_YOU_WANT_TO_GET_YOUR_FOOD, "HOW DO YOU WANT TO GET YOUR FOOD?"));
        deliveriesTogetherLabel.setText(CacheUtil.getInstance().getTranslations(Text.GET_ALL_YOUR_DELIVERIES_TOGETHER, "Get all deliveries together"));
        seeMeals.setText(CacheUtil.getInstance().getTranslations(Text.SEE_MEALS, "SEE MEALS"));
        totalLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_LABEL, "Total:").replace(":", "") + ":");
        totalVatLabel.setText(CacheUtil.getInstance().getTranslations(Text.TOTAL_IS_INCLUSIVE_OF_VAT, "Total is inclusive of VAT"));
        cancelButton.setText(CacheUtil.getInstance().getTranslations(Text.CANCEL, "CANCEL").toUpperCase());
        confirmButon.setText(CacheUtil.getInstance().getTranslations(Text.CONFIRM_CAPS, "CONFIRM").toUpperCase());
        addAddressFromDeliveryLabel.setText(CacheUtil.getInstance().getTranslations(Text.ADD_A_NEW_ADDRESS_PLUS, "+ ADD A NEW ADDRESS").toUpperCase());
    }

    @Override
    public void select(StubPreMadeModel menu, int position) {
        boolean isPickup = menu.isPickup;
        sList.get(position).isPickup = !isPickup;
        adapter.notifyDataSetChanged();
    }

    @Override
    public void setAddress(StubPreMadeModel menu, int position) {
        isPickupTab = sList.get(position).isPickup;
        isTimeSelected = false;
        currentPosition = position;
        if (!isPickupTab) {
            deliveryTimeList.setVisibility(View.GONE);
            deliveryAddressLayout.setVisibility(View.VISIBLE);
            pickupTimeLayout.setVisibility(View.GONE);
            slidingTitle.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_DELIVERY_ADDRESS, "SELECT DELIVERY ADDRESS").toUpperCase());
            setDeliveryAddress();
        } else {
            currentSelectedPickup = position;
            goToPickUpPage(menu);
        }
    }

    @Override
    public void setTime(StubPreMadeModel meal, int position) {
        isPickupTab = sList.get(position).isPickup;
        isTimeSelected = true;
        currentPosition = position;
        if (isPickupTab) {
            currentSelectedPickupTime = position;
            setPickUpTime(meal);
        } else {
            deliveryTimeList.setVisibility(View.VISIBLE);
            deliveryAddressLayout.setVisibility(View.GONE);
            pickupTimeLayout.setVisibility(View.GONE);
            if (meal.isTogether) {
                slidingTitle.setText(CacheUtil.getInstance().getTranslations(Text.SET_A_DELIVERY_TIME, "SET A DELIVERY TIME").toUpperCase());
            } else {
                slidingTitle.setText(CacheUtil.getInstance().getTranslations(Text.SET_A_DELIVERY_TIME, "SET A DELIVERY TIME").toUpperCase() + " " +
                        CacheUtil.getInstance().getTranslations(Text.FOR, "FOR").toUpperCase() + " " + meal.mealTitle.toUpperCase());
            }
            setDeliveryTime(meal);
        }
    }

    private void assignNewPickupSelectedBranch() {
        sList.get(currentSelectedPickup).pickupAddressDone = true;
        sList.get(currentSelectedPickup).pickupAddress = selectedPickupBranch;
        sList.get(currentSelectedPickup).pickupSelectedId = selectedPickupId;
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 102) {
                preMadePresenter.getAddresses();
            } else if (requestCode == 103) {
                selectedPickupId = data.getStringExtra("selectedId");
                selectedPickupBranch = data.getStringExtra("branchName");
                showSetDialog(false, false);
                assignNewPickupSelectedBranch();
            }
        }
    }

    private void setDeliveryAddress() {
        preMadePresenter.getAddresses();
        new Handler().postDelayed(() -> {
            expandPanel();
        }, 300);
    }

    private void setDeliveryTime(StubPreMadeModel meal) {
        currentSelectedMeal = meal;
        deliverTimeAdapter.selectedTime = -1;
        preMadePresenter.getDeliveryTime();
    }

    private void setPickUpTime(StubPreMadeModel meal) {
        if (!meal.isTogether) {
            slidingTitle.setText(CacheUtil.getInstance().getTranslations(Text.SET_PICKUP_TIME, "SET PICKUP TIME").toUpperCase() + " " +
                    CacheUtil.getInstance().getTranslations(Text.FOR, "FOR").toUpperCase() + " " + meal.mealTitle.toUpperCase());
        } else {
            slidingTitle.setText(CacheUtil.getInstance().getTranslations(Text.SET_PICKUP_TIME, "SET PICKUP TIME"));
        }
        deliveryTimeList.setVisibility(View.GONE);
        deliveryAddressLayout.setVisibility(View.GONE);
        pickupTimeLayout.setVisibility(View.VISIBLE);
        new Handler().postDelayed(() -> expandPanel(), 300);
    }

    private void expandPanel() {
        bottomSlidingLayout.setVisibility(View.VISIBLE);
        slidingPanelMain.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }

    private void hidePanel() {
        bottomSlidingLayout.setVisibility(View.GONE);
        slidingPanelMain.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    @Override
    public void setupFonts() {

    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {

    }

    @Override
    public void selectDeliveryTime(DeliveryTime time, int position) {
        deliverTimeAdapter.selectedTime = position;
        deliverTimeAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.cancel_button)
    void cancelButton() {
        currentPosition = -1;
        isTimeSelected = false;
        hidePanel();
    }

    @OnClick(R.id.add_address_from_delivery)
    void addAddressFrom() {
        Intent intent = new Intent(this, AddressActivity.class);
        intent.putExtra("fromSubs", true);
        startActivityForResult(intent, 102);
        overridePendingTransition(AnimUtil.inF(), AnimUtil.inS());
    }

    @OnClick(R.id.confirm_button)
    void confirmButton() {
        isTime = false;
        isAddress = false;
        if (isPickupTab) {
            if (isTimeSelected) {
                if (selectedDate != null) {
                    getDate(selectedDate);
                    hidePanel();
                    isTime = true;
                    isAddress = false;
                    isFromConfirm = true;
                }
            }
        } else {
            if (isTimeSelected) {
                if (deliverTimeAdapter.selectedTime != -1) {
                    sList.get(currentPosition).deliveryTime = dTimeList.get(deliverTimeAdapter.selectedTime).time;
                    sList.get(currentPosition).deliveryTimeDone = true;
                    adapter.notifyDataSetChanged();
                    hidePanel();
                    isTime = true;
                    isAddress = false;
                    isFromConfirm = true;
                }
            } else {
                if (!deliveryAddressAdapter.selectedId.isEmpty()) {
                    sList.get(currentPosition).deliveryAddress = currentSelectedName;
                    sList.get(currentPosition).deliveryAddressDone = true;
                    adapter.notifyDataSetChanged();
                    hidePanel();
                    isTime = false;
                    isAddress = true;
                    isFromConfirm = true;
                }
            }
        }
    }

    private void showSetDialog(boolean isTime, boolean isAddress) {
        SetDialogFragment fragment = SetDialogFragment.newInstance(isTime, isAddress);
        fragment.show(getSupportFragmentManager(), SetDialogFragment.class.getSimpleName());
    }

    @Override
    public void selectDeliveryAddress(Address address, int position, String name) {
        currentSelectedName = name;
        currentSelectedAddressId = address.getId();
        deliveryAddressAdapter.selectedId = currentSelectedAddressId;
        deliveryAddressAdapter.notifyDataSetChanged();
    }
}
