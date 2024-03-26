package com.herfy.concepts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.herfy.concepts.views.ConceptViewHolder;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.AuthUtil;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.calls.ConceptsService;
import com.skylinedynamics.solosdk.api.calls.UtilitiesApiCall;
import com.skylinedynamics.solosdk.api.models.kotlin.SoloResponse;
import com.skylinedynamics.solosdk.api.models.kotlin.SoloResponseData;
import com.skylinedynamics.solosdk.util.BooleanTypeAdapter;
import com.herfy.util.AppLogger;
import com.skylinedynamics.solosdk.api.ApiRequestListener;
import com.skylinedynamics.solosdk.api.models.objects.Concept;
import com.herfy.utils.helper.DeviceUtil;
import com.herfy.utils.helper.LocaleUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConceptsPresenter implements ConceptsContract.Presenter {

    private final ConceptsContract.View conceptsView;

    private ArrayList<Concept> concepts = new ArrayList<>();

    public ConceptsPresenter(ConceptsContract.View conceptsView) {

        this.conceptsView = conceptsView;
        this.conceptsView.setPresenter(this);

    }

    @Override
    public void start() {
        conceptsView.setupViews();
        conceptsView.setupFonts();
        conceptsView.setupTranslations();
    }

    @Override
    public void getConcepts(JSONObject conceptsObject) {
        int selected = -1;
        concepts.clear();
        try {
            JSONArray conceptsArray = conceptsObject.getJSONArray("data");
            GsonBuilder builder = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(boolean.class, new BooleanTypeAdapter());
            Gson gson = builder.create();
            for(int i = 0; i < conceptsArray.length(); i++) {
                // TODO: Inject this
                Concept concept = gson.fromJson(conceptsArray.getJSONObject(i).getJSONObject("attributes").toString(), Concept.class);
                concept.setId(conceptsArray.getJSONObject(i).getString("id"));
                concepts.add(concept);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for(Concept concept : concepts) {
            if(concept.getKey().equals(CacheUtil.getInstance().getConceptKey())) {
                selected = concepts.indexOf(concept);
                break;
            }
        }
        conceptsView.selectConcept(selected, false);
    }

    @Override
    public void onBindConceptViewAtPosition(ConceptViewHolder conceptViewHolder, int position, boolean selected) {
        conceptViewHolder.setConcept(concepts.get(position), selected);
    }

    @Override
    public int getConceptsCount() {
        return concepts.size();
    }

    @Override
    public void selectConcept(int position) {
        conceptsView.selectConcept(position, !concepts.get(position).getKey().equals(CacheUtil.getInstance().getConceptKey()));
    }

    @Override
    public void getConcept(int selected) {
        if(selected < concepts.size()) {
            ConceptsService.INSTANCE.getConcept(concepts.get(selected).getKey(), new Callback<SoloResponse<SoloResponseData<Concept>>>() {
                @Override
                public void onResponse(Call<SoloResponse<SoloResponseData<Concept>>> call, Response<SoloResponse<SoloResponseData<Concept>>> response) {
                    if (response.isSuccessful()) {
                        Concept concept = response.body().getData().getAttributes();
                        concept.setId(response.body().getData().getId());

                        CacheUtil.getInstance().setConceptKey(concepts.get(selected).getKey());
                        CacheUtil.getInstance().clearOrder();
                        CacheUtil.getInstance().setConcept(concept);
                        CacheUtil.getInstance().setMenuId(concept.getDefaultMenuId());

                        conceptsView.initSolo();

                        updatePushNotifications();

                        conceptsView.showSplash();
                    } else {
                        String detail = CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED);
                        try {
                            if (response.errorBody() != null) {
                                JsonElement error = JsonParser.parseString(response.errorBody().toString());
                                if (error.isJsonObject()) {
                                    detail = error.getAsJsonObject().get("detail").getAsString();
                                } else if (error.isJsonArray()) {
                                    detail = error.getAsJsonArray().get(0).getAsJsonObject().get("detail").getAsString();
                                }
                            }
                        } catch (Throwable t) {

                        }
                        conceptsView.showError(detail);
                    }
                }

                @Override
                public void onFailure(Call<SoloResponse<SoloResponseData<Concept>>> call, Throwable t) {
                    AppLogger.printRetrofitError(t);
                    conceptsView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
                }
            });
        } else {
            int newSelected = selected;
            for(Concept concept : concepts) {
                if(concept.getKey().equals(CacheUtil.getInstance().getConceptKey())) {
                    newSelected = concepts.indexOf(concept);
                    break;
                }
            }
            conceptsView.selectConcept(selected, selected == newSelected);
            conceptsView.showError(CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED));
        }

    }

    @Override
    public void updatePushNotifications() {

        String token = AuthUtil.getInstance().getFirebase();

        if(token != null && !token.isEmpty() && !token.equalsIgnoreCase("null")) {
            String uniqueID = AuthUtil.getInstance().getUniqueId();

            if (uniqueID.isEmpty()) {
                uniqueID = UUID.randomUUID().toString();
                AuthUtil.getInstance().setUniqueId(uniqueID);
            }

            String customerId = null;
            String customerMobileNumber = null;
            if (AuthUtil.getInstance().isSignedIn()) {
                customerId = AuthUtil.getInstance().getCustomer().getId();
                String key = CacheUtil.getInstance().getConceptKey();
                if(key.equalsIgnoreCase("CQcFAwgLDA0")) {
                    customerMobileNumber = CacheUtil.getInstance().getConcept().getDialingCode() + AuthUtil.getInstance().getCustomer().getAttributes().getMobile().substring(1);
                } else {
                    customerMobileNumber = AuthUtil.getInstance().getCustomer().getAttributes().getMobile();
                }
            }

            UtilitiesApiCall.getInstance().postIdentify(token, uniqueID, customerMobileNumber, customerId,
                new ApiRequestListener() {
                    @Override
                    public void onSuccess(Object object) {
                        DeviceUtil.save(object.toString());
                        if (AuthUtil.getInstance().isSignedIn() && !AuthUtil.getInstance().getDeviceId().isEmpty()) {

                            UtilitiesApiCall.getInstance().changeLanguage(AuthUtil.getInstance().getDeviceId(), LocaleUtil.getInstance().getLanguage(), new ApiRequestListener() {
                                @Override
                                public void onSuccess(Object obj) {
                                    DeviceUtil.save(obj.toString());
                                }

                                @Override
                                public void onError(Object error) {
                                    AppLogger.printRetrofitError(error);
                                }
                            });

                        }
                    }

                    @Override
                    public void onError(Object error) {
                        AppLogger.printRetrofitError(error);
                    }
                });

        }

    }

}
