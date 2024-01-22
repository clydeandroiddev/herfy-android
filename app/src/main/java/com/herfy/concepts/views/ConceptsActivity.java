package com.herfy.concepts.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.concepts.ConceptsContract;
import com.herfy.concepts.ConceptsPresenter;
import com.herfy.concepts.ConceptsRecyclerViewAdapter;
import com.herfy.constants.Text;
import com.herfy.font.FontHandler;
import com.herfy.splash.SplashActivity;
import com.herfy.util.CacheUtil;
import com.herfy.util.EnvUtil;
import com.herfy.util.JsonUtil;
import com.skylinedynamics.solosdk.api.SoloApi;
import com.skylinedynamics.solosdk.api.models.objects.Concept;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ConceptsActivity extends BaseActivity implements ConceptsContract.View {

    private ConceptsContract.Presenter conceptsPresenter;

    @BindView(R.id.back) ImageButton back;

    @BindView(R.id.select_store_label) TextView selectStoreLabel;

    @BindView(R.id.stores) RecyclerView stores;

    @BindView(R.id.save) MaterialButton save;

    private ConceptsRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concepts);
        ButterKnife.bind(this);
        conceptsPresenter = new ConceptsPresenter(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        conceptsPresenter.start();
        conceptsPresenter.getConcepts(JsonUtil.getJsonObjectAsset(this, "concepts.json"));
    }

    @Override
    public void setPresenter(ConceptsContract.Presenter presenter) {
        conceptsPresenter = presenter;
    }

    @Override
    public void logEvent(String eventName, HashMap<String, String> eventAttributes, String eventMetricName, double eventMetricValue) {
        logEventAttributesMetric(eventName, eventAttributes, eventMetricName, eventMetricValue);
    }

    @Override
    public void setupViews() {
        back.setOnClickListener(view -> onBackPressed());
        stores.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConceptsRecyclerViewAdapter(this, conceptsPresenter);
        stores.setAdapter(adapter);
        save.setOnClickListener(view -> {
            showLoadingDialog();
            conceptsPresenter.getConcept(adapter.getSelected());
        });
    }


    @Override
    public void setupFonts() {
        selectStoreLabel.setTypeface(FontHandler.getExtraboldTypeface());
    }

    @Override
    public void setupTranslations() {
        selectStoreLabel.setText(CacheUtil.getInstance().getTranslations(Text.SELECT_FRANCHISE));
        save.setText(CacheUtil.getInstance().getTranslations(Text.SAVE_CHANGES));
    }

    @Override
    public void setConcept(Concept concept, boolean selected) {

    }

    @Override
    public void selectConcept(int selected, boolean newConcept) {
        adapter.setSelected(selected);
        if(newConcept) {
            save.setAlpha(1f);
        } else {
            save.setAlpha(0.7f);
        }
        save.setEnabled(newConcept);
    }

    @Override
    public void showError(String error) {
        dismissDialogs();
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void initSolo() {
        SoloApi.init(this, EnvUtil.getEnvironmentUrl(), CacheUtil.getInstance().getConceptKey());
    }

    @Override
    public void showSplash() {
        dismissDialogs();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("concepts", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

}
