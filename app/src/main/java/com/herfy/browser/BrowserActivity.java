package com.herfy.browser;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.common.toolbar.TitleToolbar;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BrowserActivity extends BaseActivity {

    @BindView(R.id.toolbar) TitleToolbar toolbar;

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

        toolbar.setNavButtonAction(view -> onBackPressed());

        Bundle bundle = getIntent().getExtras();

        if(bundle != null && bundle.containsKey("title") && bundle.containsKey("url")) {

            String title = bundle.getString("title");
            if(title != null) {
                toolbar.setTitle(title);
            }

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
