package com.herfy.util;

import android.util.Log;

import com.herfy.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class AppLogger {

    private static final String TAG = BuildConfig.TAG;

    public static void info(String tag, String string) {

        if (BuildConfig.DEBUG) {
            if (!string.isEmpty()) {
                if (string.length() > 4000) {
                    Log.i(tag, string.substring(0, 4000));
                    info(tag, string.substring(4000));
                } else {
                    Log.i(tag, string);
                }
            } else {
                Log.e(TAG, "Nothing to log");
            }
        }

    }

    public static void info(String string) {

        if (BuildConfig.DEBUG) {
            if (!string.isEmpty()) {
                Log.i(TAG, string);
            } else {
                Log.e(TAG, "Nothing to log");
            }
        }
    }

    public static void error(String string) {
        if (BuildConfig.DEBUG) {
            if (!string.isEmpty()) {
                int maxLogSize = 4000;
                for (int i = 0; i <= string.length() / maxLogSize; i++) {
                    int start = i * maxLogSize;
                    int end = (i + 1) * maxLogSize;
                    end = end > string.length() ? string.length() : end;
                    Log.e(TAG, string.substring(start, end));
                }
            } else {
                Log.e(TAG, "Nothing to log");
            }
        }
    }

    public static void warn(String string) {
        if (BuildConfig.DEBUG) {
            if (!string.isEmpty()) {
                Log.w(TAG, string);
            } else {
                Log.e(TAG, "Nothing to log");
            }
        }
    }

    public static void longInfo(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            if (!msg.isEmpty()) {
                if (msg.length() > 4000) {
                    Log.i(tag, msg.substring(0, 4000));
                    longInfo(tag, msg.substring(4000));
                } else {
                    Log.i(tag, msg);
                }
            } else {
                error("LongInfo msg is empty");
            }
        }
    }

    public static void longInfo(String msg) {
        if (!msg.isEmpty()) {
            if (msg.length() > 4000) {
                info(msg.substring(0, 4000));
                longInfo(msg.substring(4000));
            } else {
                info(msg);
            }
        } else {
            error("LongInfo msg is empty");
        }
    }

    public static void longError(String msg) {
        if (!msg.isEmpty()) {
            if (msg.length() > 4000) {
                error(msg.substring(0, 4000));
                longError(msg.substring(4000));
            } else {
                error(msg);
            }
        } else {
            error("LongError msg is empty");
        }
    }

    public static void longWarn(String msg) {
        if (!msg.isEmpty()) {
            if (msg.length() > 4000) {
                warn(msg.substring(0, 4000));
                longWarn(msg.substring(4000));
            } else {
                warn(msg);
            }
        } else {
            error("LongWarn msg is empty");
        }
    }

    public static void prettyPrint(JSONObject object) {
        try {
            String text = object.toString(2);
            String[] temp = text.split("\n");
            for (String s : temp) {
                info(s);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void printJSONObject(String tag, JSONObject object) {
        if (BuildConfig.DEBUG) {
            try {
                String text = object.toString(2);
                String[] temp = text.split("\n");
                for (String s : temp) {
                    Log.i(tag, s);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                printJson(object);
            }
        }
    }

    public static void printJson(Object object) {
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            try {
                longInfo(jsonObject.toString(2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            try {
                longInfo(jsonArray.toString(2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (object != null) {
            longInfo(object.toString());
        } else {
            error("Nothing to Log");
        }
    }

    public static void printRetrofitError(Object error) {
        if (error instanceof JSONObject) {
            JSONObject errorObject = (JSONObject) error;
            try {
                longError(errorObject.toString(2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (error instanceof Exception) {
            if (BuildConfig.DEBUG) {
                error("Exception Stacktrace");
                ((Exception) error).printStackTrace();
            }
        } else if (error instanceof Throwable) {
            if (BuildConfig.DEBUG) {
                error("Throwable Stacktrace");
                ((Throwable) error).printStackTrace();
            }
        } else if (error != null) {
            longError(error.toString());
        } else {
            error("Nothing to log");
        }
    }

    public static void printMap(Map<String, String> map) {
        for (String key : map.keySet()) {
            longInfo(key + ": " + map.get(key));
        }
    }
}
