package com.herfy.browser;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.data.constants.Text;
import com.herfy.util.CacheUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BrowserActivity extends BaseActivity {

    @BindView(R.id.back) MaterialButton back;

    @BindView(R.id.title) TextView title;

    @BindView(R.id.cart) ConstraintLayout cart;

    @BindView(R.id.webView) WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void setupViews() {

        showLoadingDialog();

        back.setText(CacheUtil.getInstance().getTranslations(Text.BACK, "Back"));
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        cart.setVisibility(View.INVISIBLE);

        Bundle bundle = getIntent().getExtras();

        if(bundle != null && bundle.containsKey("title") && bundle.containsKey("url")) {

            title.setText(bundle.getString("title"));

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    dismissDialogs();
                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl(bundle.getString("url"));

        } else {
            showAlertDialog("", CacheUtil.getInstance().getTranslations(Text.AN_ERROR_OCCURRED), CacheUtil.getInstance().getTranslations(Text.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onBackPressed();
                }
            });
        }

    }

}
