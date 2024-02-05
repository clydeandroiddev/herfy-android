package com.herfy.utils.helper;

import android.content.Context;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;


public class JsonAssetUtil
{
    public static @Nullable JSONObject getJsonObjectAsset(Context context, String filename)
    {
        try
        {

            InputStream is = context.getAssets().open(filename);

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");

            return new JSONObject(json);
        }
        catch (IOException | JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
