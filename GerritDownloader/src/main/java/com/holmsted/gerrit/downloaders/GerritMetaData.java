package com.holmsted.gerrit.downloaders;

import com.google.common.base.Strings;
import com.holmsted.gerrit.GerritVersion;
import com.holmsted.json.JsonUtils;
import org.json.JSONObject;

/**
 * Parses out some meta data from the Gerrit server output.
 */
public class GerritMetaData {
    private GerritVersion gerritVersion;
    private final int rowCount;
    private final int runTimeMilliseconds;
    private final boolean moreChanges;
    private final String resumeSortKey;

    static GerritMetaData fromJSONString(String output, GerritVersion gerritVersion) {
        JSONObject lastLineData = JsonUtils.readJsonString(output);
        if (lastLineData.get("rowCount") != null) {
            return new GerritMetaData(lastLineData, gerritVersion);
        } else {
            return null;
        }
    }

    private GerritMetaData(JSONObject metadata, GerritVersion gerritVersion) {
        this.gerritVersion = gerritVersion;
        moreChanges = metadata.optBoolean("moreChanges");
        rowCount = metadata.optInt("rowCount");
        runTimeMilliseconds = metadata.optInt("runTimeMilliseconds");
        resumeSortKey = metadata.optString("resumeSortKey");
    }


    public int getRowCount() {
        return rowCount;
    }

    public int getRunTimeMilliseconds() {
        return runTimeMilliseconds;
    }

    public boolean hasMoreChanges() {
        if (gerritVersion.isAtLeast(2, 9)) {
            return moreChanges;
        } else {
            return !Strings.isNullOrEmpty(resumeSortKey);
        }
    }

    public String getResumeSortKey() {
        return resumeSortKey;
    }
}