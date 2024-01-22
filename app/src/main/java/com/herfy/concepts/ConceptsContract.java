package com.herfy.concepts;

import com.herfy.base.BasePresenter;
import com.herfy.base.BaseView;
import com.herfy.concepts.views.ConceptViewHolder;
import com.skylinedynamics.solosdk.api.models.objects.Concept;

import org.json.JSONObject;

public interface ConceptsContract {

    interface View extends BaseView<ConceptsContract.Presenter> {

        void setConcept(Concept concept, boolean selected);

        void selectConcept(int selected, boolean newConcept);

        void showError(String error);

        void initSolo();

        void showSplash();

    }

    interface Presenter extends BasePresenter {

        void getConcepts(JSONObject conceptsObject);

        void onBindConceptViewAtPosition(ConceptViewHolder conceptViewHolder, int position, boolean selected);

        int getConceptsCount();

        void selectConcept(int position);

        void getConcept(int selected);

        void updatePushNotifications();

    }
}
