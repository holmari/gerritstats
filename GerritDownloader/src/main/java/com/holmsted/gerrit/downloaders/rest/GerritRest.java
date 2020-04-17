package com.holmsted.gerrit.downloaders.rest;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.json.JSONObject;

import com.holmsted.gerrit.GerritVersion;
import com.holmsted.gerrit.downloaders.AbstractGerritMetadataFetcher;
import com.holmsted.json.JsonUtils;

public final class GerritRest extends AbstractGerritMetadataFetcher {

    public GerritRest(@Nonnull GerritRestServer gerritServer) {
        super(gerritServer);
    }

    
    @Override
    public List<String> listProjects() {
        GerritRestCommand command = new GerritRestCommand((GerritRestServer) getGerritServer());
        String output = command.exec("/r/projects/?d");
        List<String> projectList = new ArrayList<>();
        JSONObject projects = JsonUtils.readJsonString(output);
        for (Object key : projects.keySet())
            projectList.add(key.toString());
        return projectList;
    }

	@Override
    public GerritVersion getVersion() {
		GerritRestCommand command = new GerritRestCommand((GerritRestServer)getGerritServer());
        String output = command.exec("/r/config/server/version");
        if (output != null) {
            // answers (with quotes):
            //    "2.14.20"
            return GerritVersion.fromString(output.substring(1, output.length() - 2));
        } else {
            return null;
        }
    }
}
