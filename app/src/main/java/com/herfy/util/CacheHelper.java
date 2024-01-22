package com.herfy.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CacheHelper {

    public static String getCacheDirectory(Context context){

        return context.getCacheDir().getPath();
    }

    public static void save(Context context, String key, String value) {

        try {

            File cache = new File(getCacheDirectory(context), key + ".json");

            try (FileOutputStream stream = new FileOutputStream(cache)) {
                stream.write(value.getBytes());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save(Context context, String key, String value, String identifier) {
        save(context, key + identifier, value);
    }

    public static String retrieve(Context context, String key, String identifier) {
        return retrieve(context, key + identifier);
    }


    public static String retrieve(Context context, String key) {
        String value = "";

        File cache = new File(getCacheDirectory(context), key + ".json");

        if (cache.exists()) {

            try {
                int length = (int) cache.length();

                byte[] bytes = new byte[length];

                try (FileInputStream in = new FileInputStream(cache)) {
                    in.read(bytes);
                }

                value = new String(bytes);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return value;
    }

}