package com.herfy.utils.helper;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class GsonUtil {

    public static String getGsonString(Object object) {
        String ret = "";
        try {
            ret = new Gson().toJson(object);
        } catch (JsonParseException e) {
            if (!(object instanceof String))
                e.getStackTrace();
        }
        return ret;
    }

    public static <T> T getGson(String json, Class<T> clazz) {
        Gson gson = new Gson();
        T object = null;
        try {
            object = gson.fromJson(json, clazz);
        } catch (Exception e) {
            // do nothing
            e.printStackTrace();
        }
        return object;
    }

    public static <T> T getGson(Object object, Class<T> clazz){
        return getGson(object.toString(), clazz);
    }
}
