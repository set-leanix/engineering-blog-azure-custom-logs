package net.leanix.azurecustomlogs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogAnalyticsWorkspaceConfiguration {

    @JsonProperty("id")
    private final String id;

    @JsonProperty("key")
    private final String key;

    public LogAnalyticsWorkspaceConfiguration(String id, String key) {
        this.id = id;
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public boolean isEnabled() {
        return id != null && key != null;
    }
}
