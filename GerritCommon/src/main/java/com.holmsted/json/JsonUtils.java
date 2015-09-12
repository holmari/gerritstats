package com.holmsted.json;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils {
    public static JSONObject readJsonString(String jsonString) {
        return new JSONObject(new JSONTokener(jsonString));
    }

    public static List<String> readStringArray(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); ++i) {
            list.add(array.getString(i));
        }
        return list;
    }
}
