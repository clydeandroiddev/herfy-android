package com.herfy.loyalty;

import android.annotation.SuppressLint;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.herfy.data.constants.Text;
import com.herfy.loyalty.objects.DateItem;
import com.herfy.loyalty.objects.ListItem;
import com.herfy.loyalty.objects.LoyaltyTransactionItem;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.calls.CustomersApiCall;
import com.skylinedynamics.solosdk.api.models.objects.Customer;
import com.skylinedynamics.solosdk.api.models.objects.Loyalty;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyAttributes;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyPointsPosRedemption;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTier;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransaction;
import com.skylinedynamics.solosdk.api.models.objects.LoyaltyTransactionDetails;
import com.skylinedynamics.solosdk.api.models.objects.QrData;
import com.skylinedynamics.solosdk.api.models.objects.Tier;
import com.skylinedynamics.solosdk.api.models.requestbodies.PosPointsRedemptionBody;
import com.skylinedynamics.solosdk.util.GsonHelper;
import com.herfy.util.AppLogger;
import com.herfy.util.AuthUtil;
import com.herfy.util.CacheUtil;
import com.herfy.util.ErrorHandler;
import com.herfy.util.TimeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
public class LoyaltyPresenter implements LoyaltyContract.Presenter {

    private final LoyaltyContract.View loyaltyView;

    ArrayList<LoyaltyTransaction> transactionsList = new ArrayList<>();
    List<Tier> tiers = new ArrayList<>();
    private boolean newestToOldest = true;
    private CompositeDisposable disposables = new CompositeDisposable();

    ArrayList<ListItem> consolidatedList = new ArrayList<>();

    public LoyaltyPresenter(LoyaltyContract.View loyaltyView) {
        this.loyaltyView = loyaltyView;
        this.loyaltyView.setPresenter(this);
    }

    @Override
    public void start() {
        loyaltyView.setupViews();
        loyaltyView.setupFonts();
        loyaltyView.setupTranslations();
    }

    public Single<List<Tier>> getTiersObservable() {
        return Single.create(source -> {
            CustomersApiCall.getInstance().getLoyaltyTiers(new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray tiersArray = object.getJSONArray("data");
                        Type type = new TypeToken<List<Tier>>() {
                        }.getType();
                        List<Tier> tiers = GsonHelper.gsonFinal().fromJson(tiersArray.toString(), type);
                        // TODO: Should set Tier as Comparable in SDK
                        Collections.sort(tiers, (Comparator<Tier>) (o1, o2) -> {
                            return o1.getStartPoint() - o2.getStartPoint();
                        });
                        source.onSuccess(tiers);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onError(Object o) {
                    if (o instanceof Throwable) {
                        source.onError((Throwable) o);
                    } else {
                        source.onError(new Throwable(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
                    }
                }
            });
        });
    }

    public Single<Loyalty> getLoyaltyObservable() {
        return Single.create(source -> {
            CustomersApiCall.getInstance().getCustomerLoyalty(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONObject data = object.getJSONObject("data");
                        if (data.getJSONObject("attributes").optJSONObject("tier") == null || data.getJSONObject("attributes").get("tier") instanceof JSONObject) {
                            Loyalty loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), Loyalty.class);
                            AuthUtil.getInstance().setCustomerLoyalty(loyalty);
                            source.onSuccess(loyalty);
                        } else {
                            source.onError(new Throwable(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);

                    if (o instanceof JSONObject) {
                        try {
                            JSONObject object = (JSONObject) o;
                            Object error = object.get("error");
                            String detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED);
                            if (error instanceof JSONObject) {
                                detail = ((JSONObject) error).getString("detail");
                            } else if (error instanceof JSONArray) {
                                JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                                detail = errorObject.getString("detail");
                            }
                            source.onError(new Throwable(detail));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            source.onError(new Throwable(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
                        }
                    } else {
                        if (o instanceof Throwable) {
                            source.onError((Throwable) o);
                        } else {
                            source.onError(new Throwable(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED)));
                        }
                    }
                }
            });
        });
    }

    @Override
    public void cleanup() {
        disposables.clear();
    }

    @Override
    public void redeemPoints(int points) {
        String customerId = AuthUtil.getInstance().getCustomer().getId();
        PosPointsRedemptionBody body = new PosPointsRedemptionBody(points);
        CustomersApiCall.getInstance().redeemLoyaltyPointsForPos(customerId, body, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                LoyaltyPointsPosRedemption data = null;
                try {
                    data = new Gson().fromJson(o.toString(), LoyaltyPointsPosRedemption.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                loyaltyView.showQRPage(data);
            }

            @Override
            public void onError(Object o) {
                AppLogger.error(o.toString());
                loyaltyView.showQRPage(null);
            }
        });
    }

    @Override
    public void getLoyalty() {
        // Ensures that there will always be a tier when loyalty is shown
        Single.zip(getTiersObservable(), getLoyaltyObservable(), (tiers, loyalty) -> new Pair<>(loyalty, tiers)).doOnSubscribe(disposable -> disposables.add(disposable)).subscribe(pair -> {
            List<Tier> tiers = pair.second;
            Loyalty loyalty = pair.first;
            this.tiers = tiers;

            int totalPointsAccumulated = loyalty.getAttributes().getCurrentBalance();
            List<Tier> eligibleTiers = Observable.fromIterable(tiers).filter(tier -> totalPointsAccumulated <= tier.getEndPoint()).toList().blockingGet();

            int pointsToNextTier = 0;

            loyaltyView.showLoyalty(loyalty, AuthUtil.getInstance().getCustomer().getAttributes().getFirstName() + " " + AuthUtil.getInstance().getCustomer().getAttributes().getLastName());
            Tier nextTier = null;

            if (eligibleTiers.size() <= 0 && tiers.size() >= 1) {
                eligibleTiers = new ArrayList<>();
                eligibleTiers.add(tiers.get(tiers.size() - 1));
            }

            if (eligibleTiers.size() >= 1) {
                nextTier = eligibleTiers.get(0);
            }

            if (eligibleTiers.size() > 1) {
                nextTier = eligibleTiers.get(1);
                pointsToNextTier = nextTier.getStartPoint() - loyalty.getAttributes().getCurrentBalance();
            }

            int startPoints = 0;
            if (nextTier != null) {
                startPoints = nextTier.getStartPoint();
            }
            
            loyaltyView.showNextTier(nextTier, totalPointsAccumulated, pointsToNextTier, startPoints);

        }, error -> {
            Loyalty loyalty = new Loyalty();
            LoyaltyAttributes loyaltyAttributes = new LoyaltyAttributes();
            LoyaltyTier loyaltyTier = new LoyaltyTier();
            loyaltyTier.setName("Classic");
            loyaltyAttributes.setLoyaltyTier(loyaltyTier);
            loyaltyAttributes.setCurrentBalance(0);

            Tier currentTier = new Tier();
            currentTier.setName("Classic");
            currentTier.setStartPoint(0);

            Tier nextTier = new Tier();
            nextTier.setName("Silver");
            nextTier.setStartPoint(100);

            loyaltyAttributes.setLoyaltyTier(loyaltyTier);
            loyaltyAttributes.setCurrentBalance(0);
            tiers = new ArrayList<>(Arrays.asList(currentTier));
            loyalty.setAttributes(loyaltyAttributes);
            loyaltyView.showLoyalty(loyalty, AuthUtil.getInstance().getCustomer().getAttributes().getFirstName() + " " + AuthUtil.getInstance().getCustomer().getAttributes().getLastName());
            loyaltyView.showNextTier(nextTier, 0, 100, 100);
        });
    }

    @Override
    public void getCustomerLoyalty() {
        String customerId = AuthUtil.getInstance().getCustomer().getId();
        CustomersApiCall.getInstance().getLoyaltyCustomer(customerId, new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONObject data = object.getJSONObject("data");
                    QrData qrData = new Gson().fromJson(data.getJSONObject("qr-data").toString(), QrData.class);
                    loyaltyView.showQrData(new Gson().toJson(qrData));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.error(o.toString());
            }
        });

    }

    @Override
    public void getCustomerDetails() {
        Customer customer = AuthUtil.getInstance().getCustomer();
        CustomersApiCall.getInstance().getCustomerById(String.valueOf(customer.getId()), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                if (o instanceof JSONObject) {

                    try {

                        JSONObject json = (JSONObject) o;

                        JSONObject dataObj = json.getJSONObject("data");

                        Customer customer = GsonHelper.gsonFinal().fromJson(dataObj.toString(), Customer.class);
                        if(customer.getAttributes().getLastOrderId().equals(CacheUtil.getInstance().getLastOrderId())){
                            getLoyalty();
                        }else{
                            fetchAndShowNextTier();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();

                    }

                } else {

                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                getLoyalty();
            }
        });
    }

    private void fetchAndShowNextTier() {
        CustomersApiCall.getInstance().getLoyaltyTiers(new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONArray tiersArray = object.getJSONArray("data");
                    Type type = new TypeToken<List<Tier>>() {}.getType();
                    List<Tier> tiers = GsonHelper.gsonFinal().fromJson(tiersArray.toString(), type);
                    Collections.sort(tiers, (Comparator<Tier>) (o1, o2) -> {
                        return o1.getStartPoint() - o2.getStartPoint();
                    });

                    Loyalty loyalty = AuthUtil.getInstance().getCustomerLoyalty();
                    int totalPointsAccumulated = loyalty.getAttributes().getPointsAccumulated();

                    List<Tier> eligibleTiers = Observable.fromIterable(tiers)
                            .filter(tier -> totalPointsAccumulated <= tier.getEndPoint())
                            .toList()
                            .blockingGet();

                    Tier nextTier = eligibleTiers.isEmpty() ? tiers.get(tiers.size() - 1) : eligibleTiers.get(0);

                    int pointsToNextTier = eligibleTiers.size() > 1 ? nextTier.getStartPoint() - totalPointsAccumulated : 0;
                    int startPoints = nextTier.getStartPoint();

                    loyaltyView.showNextTier(nextTier, totalPointsAccumulated, pointsToNextTier, startPoints);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onError(Object o) {
                LoyaltyAttributes loyaltyAttributes = new LoyaltyAttributes();
                LoyaltyTier loyaltyTier = new LoyaltyTier();
                loyaltyTier.setName("Classic");
                loyaltyAttributes.setLoyaltyTier(loyaltyTier);
                loyaltyAttributes.setCurrentBalance(0);

                Tier currentTier = new Tier();
                currentTier.setName("Classic");
                currentTier.setStartPoint(0);

                Tier nextTier = new Tier();
                nextTier.setName("Silver");
                nextTier.setStartPoint(100);

                loyaltyAttributes.setLoyaltyTier(loyaltyTier);
                loyaltyAttributes.setCurrentBalance(0);
                tiers = new ArrayList<>(Arrays.asList(currentTier));
                loyaltyView.showNextTier(nextTier, 0, 100, 100);
            }
        });

    }
    @Override
    public void getLoyaltyHistoryTransaction() {
        CustomersApiCall.getInstance().getCustomerLoyaltyTransactions(AuthUtil.getInstance().getCustomer().getId(), new ApiRequestListener() {
            @Override
            public void onSuccess(Object o) {
                try {
                    JSONObject object = new JSONObject(o.toString());
                    JSONObject data = object.getJSONObject("data");
                    JSONObject attributes = data.getJSONObject("attributes");
                    Object tier = attributes.get("tier");
                    if (!(tier instanceof JSONObject)) {
                        attributes.put("tier", new LoyaltyTier().toJSON());
                        data.put("attributes", attributes);
                    }
                    Loyalty loyalty = GsonHelper.gsonFinal().fromJson(data.toString(), Loyalty.class);
                    loyaltyView.showLoyalty(loyalty, AuthUtil.getInstance().getCustomer().getAttributes().getFirstName() + " " + AuthUtil.getInstance().getCustomer().getAttributes().getLastName());
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.onError(e);
                }
            }

            @Override
            public void onError(Object o) {
                AppLogger.printRetrofitError(o);
                loyaltyView.showLoyaltyError(ErrorHandler.getErrorMessage(o, CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), null));
            }
        });
    }

    @Override
    public ArrayList<ListItem> getConsolidatedList() {
        return consolidatedList;
    }

    @Override
    public void getLoyaltyHistoryTransaction2(int page) {
        getLoyaltyHistory(page).subscribeOn(Schedulers.io()).doOnSubscribe(disposable -> disposables.add(disposable)).subscribe(transactionsList -> {
            if (page == 1) {
                this.transactionsList.clear();
                consolidatedList.clear();
            }
            this.transactionsList.addAll(transactionsList);
            sortTransactionList();

        }, error -> {
            loyaltyView.showLoyaltyError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
        });
    }

    public Single<List<LoyaltyTransaction>> getLoyaltyHistory(int page) {
        return Single.create(source -> {
            CustomersApiCall.getInstance().getLoyaltyTransactions(AuthUtil.getInstance().getCustomer().getId(), page, new ApiRequestListener() {
                @Override
                public void onSuccess(Object o) {
                    try {
                        JSONObject object = new JSONObject(o.toString());
                        JSONArray array = object.getJSONArray("data");
                        List<LoyaltyTransaction> list = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject object1 = array.getJSONObject(i);
                            Object details = object1.opt("details");
                            if (!object1.has("details") || object1.isNull("details") || !(details instanceof JSONObject)) {
                                LoyaltyTransactionDetails loyaltyTransactionDetails = new LoyaltyTransactionDetails();
                                loyaltyTransactionDetails.setAmount(0.0);
                                object1.put("details", new JSONObject(new Gson().toJson(loyaltyTransactionDetails)));
                            }
                            LoyaltyTransaction transaction = GsonHelper.gsonFinal().fromJson(object1.toString(), LoyaltyTransaction.class);
                            list.add(transaction);
                        }
                        List<LoyaltyTransaction> filteredList = new ArrayList<>();
                        for (LoyaltyTransaction loyaltyTransaction : list) {
                            if (loyaltyTransaction.getDetails() != null && loyaltyTransaction.getDetails().getAmount() != null) {
                                filteredList.add(loyaltyTransaction);
                            }
                        }
                        source.onSuccess(filteredList);
                        loyaltyView.showLoyaltyPointsHistory(newestToOldest, transactionsList);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        this.onError(e);
                    }
                }

                @Override
                public void onError(Object o) {
                    AppLogger.printRetrofitError(o);
                    if (o instanceof JSONException) {
                        source.onError((Throwable) o);
                    } else if (o instanceof JSONObject) {
                        try {
                            JSONObject object = (JSONObject) o;
                            Object error = object.get("error");
                            String detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED);
                            if (error instanceof JSONObject) {
                                detail = ((JSONObject) error).getString("detail");
                            } else if (error instanceof JSONArray) {
                                JSONObject errorObject = ((JSONArray) error).getJSONObject(0);
                                detail = errorObject.getString("detail");
                            }
                            loyaltyView.showLoyaltyError(detail);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            source.onError(e);
                        }
                    } else {
                        source.onError(new Throwable("API Error"));
                    }
                }
            });
        });

    }

    @Override
    public void onBindLoyaltyHistoryViewAtPosition(int position, LoyaltyPointsViewHolder loyaltyPointsViewHolder) {
        LoyaltyTransactionItem item = (LoyaltyTransactionItem) consolidatedList.get(position);
        loyaltyPointsViewHolder.setLoyaltyHistory(item.getLoyaltyTransaction());
    }

    @Override
    public int getLoyaltyPointsHistoryCount() {
        return transactionsList.size();
    }

    private JsonObject dummyResponse() {
        String json = "{\n" + "  \"type\" : \"transactions\",\n" + "  \"data\" : [\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 49,\n" + "      \"details\" : [\n" + "\n" + "      ],\n" + "      \"id\" : \"9482e1b7-fb8f-463c-b7b6-f580469ae273\",\n" + "      \"expires_at\" : \"2021-10-29 06:55:35\",\n" + "      \"created-at\" : \"2021-09-29 06:55:35\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-09-29 06:55:35\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 1,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-05\",\n" + "        \"amount\" : 17.390000000000001,\n" + "        \"order-id\" : 1190911\n" + "      },\n" + "      \"id\" : \"948f38fe-338c-41f2-b8c7-5dc40b842d6d\",\n" + "      \"expires_at\" : \"2021-11-04 10:09:34\",\n" + "      \"created-at\" : \"2021-10-05 10:09:34\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-05 10:09:34\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 2,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-05\",\n" + "        \"amount\" : 27.829999999999998,\n" + "        \"order-id\" : 1190912\n" + "      },\n" + "      \"id\" : \"948f3d30-f926-497b-964f-24f2ee13ce60\",\n" + "      \"expires_at\" : \"2021-11-04 10:21:18\",\n" + "      \"created-at\" : \"2021-10-05 10:21:18\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-05 10:21:18\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 10,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-05\",\n" + "        \"amount\" : 103.48,\n" + "        \"order-id\" : 1190913\n" + "      },\n" + "      \"id\" : \"948f3ea2-82c7-48cc-9646-92817668794e\",\n" + "      \"expires_at\" : \"2021-11-04 10:25:20\",\n" + "      \"created-at\" : \"2021-10-05 10:25:20\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-05 10:25:20\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 29,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-05\",\n" + "        \"amount\" : 295.64999999999998,\n" + "        \"order-id\" : 1190914\n" + "      },\n" + "      \"id\" : \"948f4001-0947-47b4-ae2a-d7cf9d68bcc2\",\n" + "      \"expires_at\" : \"2021-11-04 10:29:10\",\n" + "      \"created-at\" : \"2021-10-05 10:29:10\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-05 10:29:10\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 11,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-05\",\n" + "        \"amount\" : 113.04000000000001,\n" + "        \"order-id\" : 1190915\n" + "      },\n" + "      \"id\" : \"948f40d5-74f3-49bd-9f98-cbd8cd4c82fb\",\n" + "      \"expires_at\" : \"2021-11-04 10:31:29\",\n" + "      \"created-at\" : \"2021-10-05 10:31:29\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-05 10:31:29\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 0,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-05\",\n" + "        \"amount\" : 0.20000000000000001,\n" + "        \"order-id\" : 1190930\n" + "      },\n" + "      \"id\" : \"948fbf97-8fe8-4cb5-be51-099e6545e0c8\",\n" + "      \"created-at\" : \"2021-10-05 16:25:56\",\n" + "      \"type\" : \"redemption\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-05 16:25:56\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 10,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-05\",\n" + "        \"amount\" : 103.48,\n" + "        \"order-id\" : 1190930\n" + "      },\n" + "      \"id\" : \"948fc05e-cd16-472e-b750-c0d5d177a3af\",\n" + "      \"expires_at\" : \"2021-11-04 16:28:06\",\n" + "      \"created-at\" : \"2021-10-05 16:28:06\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-05 16:28:06\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 869,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-11\",\n" + "        \"amount\" : 8695.6399999999994,\n" + "        \"order-id\" : 1191018\n" + "      },\n" + "      \"id\" : \"949bc203-3bab-4a26-9cb5-bebf447cf8c4\",\n" + "      \"expires_at\" : \"2021-11-10 15:42:38\",\n" + "      \"created-at\" : \"2021-10-11 15:42:38\",\n" + "      \"type\" : \"collection\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-11 15:42:38\"\n" + "    },\n" + "    {\n" + "      \"status\" : \"successful\",\n" + "      \"points\" : 0,\n" + "      \"details\" : {\n" + "        \"date\" : \"2021-10-13\",\n" + "        \"amount\" : 2,\n" + "        \"order-id\" : 1191058\n" + "      },\n" + "      \"id\" : \"949fb81c-695f-4554-8d74-a994887b9610\",\n" + "      \"created-at\" : \"2021-10-13 14:58:15\",\n" + "      \"type\" : \"redemption\",\n" + "      \"customer-id\" : \"9482e1aa-043a-4550-9264-248c3237437a\",\n" + "      \"updated-at\" : \"2021-10-13 14:58:15\"\n" + "    }\n" + "  ],\n" + "  \"meta\" : {\n" + "    \"total-pages\" : 16,\n" + "    \"count\" : 10,\n" + "    \"current-page\" : 1,\n" + "    \"total\" : 154,\n" + "    \"per-page\" : 10\n" + "  }\n" + "}\n";

        JsonObject jsonObject = GsonHelper.gsonFinal().fromJson(json, JsonObject.class);
        return jsonObject;
    }

    @Override
    public List<Tier> getTiers() {
        return tiers;
    }

    @Override
    public void sortHistory() {
        newestToOldest = !newestToOldest;
        sortTransactionList();
        loyaltyView.showLoyaltyPointsHistory(newestToOldest, transactionsList);
    }

    private void sortTransactionList() {
        Collections.sort(transactionsList, (lhs, rhs) -> {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                Date lhsDate = simpleDateFormat.parse(lhs.getCreatedAt());
                Date rhsDate = simpleDateFormat.parse(rhs.getCreatedAt());
                if (newestToOldest) {
                    return rhsDate.compareTo(lhsDate);
                } else {
                    return lhsDate.compareTo(rhsDate);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        });

        consolidatedList.clear();

        HashMap<String,List<LoyaltyTransaction>> groupedHashMap = groupDataIntoHashMap(transactionsList);

        List<String> keys = new ArrayList<>(groupedHashMap.keySet());

        Collections.sort(keys, (lhs, rhs) -> {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
                Date lhsDate = simpleDateFormat.parse(lhs);
                Date rhsDate = simpleDateFormat.parse(rhs);
                if (newestToOldest) {
                    return rhsDate.compareTo(lhsDate);
                } else {
                    return lhsDate.compareTo(rhsDate);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        });

        HashMap<String, List<LoyaltyTransaction>> sortedHashMap = new LinkedHashMap<>();
        for (String key : keys) {
            sortedHashMap.put(key, groupedHashMap.get(key));
        }

        for (String date : sortedHashMap.keySet()) {
            DateItem dateItem = new DateItem();
            dateItem.setDate(date);
            consolidatedList.add(dateItem);
            for (LoyaltyTransaction loyaltyTransaction : sortedHashMap.get(date)) {
                LoyaltyTransactionItem loyaltyTransactionItem = new LoyaltyTransactionItem();
                loyaltyTransactionItem.setLoyaltyTransaction(loyaltyTransaction);
                consolidatedList.add(loyaltyTransactionItem);
            }
        }


    }

    private HashMap<String,List<LoyaltyTransaction>> groupDataIntoHashMap(List<LoyaltyTransaction> loyaltyTransactions) {
        HashMap<String,List<LoyaltyTransaction>> groupedHashMap = new HashMap<>();
        for (LoyaltyTransaction loyaltyTransaction : loyaltyTransactions) {
            String hashMapKey =  TimeUtil.formatDate(loyaltyTransaction.getDetails().getDate(),"yyyy-MM-dd");
            if (groupedHashMap.containsKey(hashMapKey)) {
                groupedHashMap.get(hashMapKey).add(loyaltyTransaction);
            } else {
                List<LoyaltyTransaction> list = new ArrayList<>();
                list.add(loyaltyTransaction);
                groupedHashMap.put(hashMapKey, list);
            }
        }
        return groupedHashMap;
    }

}
