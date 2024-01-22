package com.herfy.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.herfy.R;
import com.herfy.base.BaseActivity;
import com.herfy.util.CacheUtil;

public class GatewayActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        boolean forceLocation = false;
        if (CacheUtil.getInstance().getApplication().getAttributes().getForceLocation() != 0) {
            forceLocation = true;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("home", true);
        intent.putExtra("forceLocation", forceLocation);
        startActivity(intent);
        finish();
    }
}
