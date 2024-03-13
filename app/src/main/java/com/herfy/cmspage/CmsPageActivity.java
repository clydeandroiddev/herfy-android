package com.herfy.cmspage;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.common.toolbar.TitleToolbar;
import com.herfy.utils.constants.Text;
import com.herfy.utils.helper.CacheUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CmsPageActivity extends BaseActivity {

    @BindView(R.id.toolbar) TitleToolbar toolbar;

    @BindView(R.id.tvContent) TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cms_page);
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

        if(bundle != null && bundle.containsKey("title") && bundle.containsKey("content")) {

            String title = bundle.getString("title");
            if(title != null) {
                toolbar.setTitle(title);
            }

            tvContent.setText(bundle.getString("content"));

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
