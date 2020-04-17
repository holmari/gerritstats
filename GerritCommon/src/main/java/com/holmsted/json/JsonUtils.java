package com.holmsted.json;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public final class JsonUtils {

    @Nonnull
    public static JSONObject readJsonString(String jsonString) {
        return new JSONObject(new JSONTokener(jsonString));
    }

    @Nonnull
    public static List<String> readStringArray(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            list.add(array.getString(i));
        }
        return list;
    }

    private JsonUtils() {
    }
}
