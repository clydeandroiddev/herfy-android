package com.herfy.selection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatTextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.herfy.BuildConfig;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.utils.constants.Text;
import com.herfy.main.MainActivity;
import com.herfy.utils.helper.CacheUtil;
import com.skylinedynamics.solosdk.api.models.objects.Concept;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectionActivity extends BaseActivity {

    @BindView(R.id.quick_text)
    AppCompatTextView quickText;
    @BindView(R.id.subscriptions_text)
    AppCompatTextView subscriptionText;
    @BindView(R.id.mode)
    AppCompatTextView mode;
    @BindView(R.id.image)
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);
        ButterKnife.bind(this);
        CacheUtil.getInstance().setUpMode(1);
        setupViews();
        setupTranslations();
    }

    @OnClick(R.id.quick_order)
    void quickOrder() {
        CacheUtil.getInstance().setUpMode(1);
        goToMain();
    }

    @OnClick(R.id.subscriptions)
    void subs() {
        CacheUtil.getInstance().setUpMode(2);
        goToMain();
    }

    private void setupViews() {
        Concept concept = CacheUtil.getInstance().getConcept();
        if (BuildConfig.FEATURE_PHASE >= 1.3f) {
            String conceptLogo = concept.getLogo();
            if (conceptLogo != null && !conceptLogo.isEmpty()) {
                Glide.with(this)
                        .load(conceptLogo)
                        .apply(RequestOptions.centerInsideTransform())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(image);
            }
        } else {
            image.setImageResource(R.drawable.header);
        }
    }

    private void setupTranslations() {
        subscriptionText.setText(CacheUtil.getInstance().getTranslations(Text.SUBSCRIPTIONS, "Subscriptions"));
        quickText.setText(CacheUtil.getInstance().getTranslations(Text.QUICK_ORDER, "Quick Order"));
        mode.setText(CacheUtil.getInstance().getTranslations(Text.PLEASE_PICK_A_MODE, "Please pick a mode"));
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("home", true);
        intent.putExtra("forceLocation", getIntent().getExtras().getBoolean("forceLocation", false));
        startActivity(intent);
        finish();
    }

}
