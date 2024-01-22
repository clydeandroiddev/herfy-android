package com.herfy.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JsonUtil {

    public static JSONObject getJsonObjectAsset(Context context, String filename) {

        JSONObject object = new JSONObject();

        try {

            InputStream is = context.getAssets().open(filename);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);

            object = new JSONObject(json);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
  
        return object;
    }

}
