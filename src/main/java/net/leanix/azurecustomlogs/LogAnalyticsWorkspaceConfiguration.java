package net.leanix.azurecustomlogs;

/**
 * Main configuration to connect to your Azure Log Analytics workspace. Refer to
 * https://docs.microsoft.com/en-us/azure/azure-monitor/logs/quick-create-workspace to learn how to create one.
 */
public class LogAnalyticsWorkspaceConfiguration {

    private final String workspaceId;

    private final String primaryKey;

    public LogAnalyticsWorkspaceConfiguration(String workspaceId, String primaryKey) {
        this.workspaceId = workspaceId;
        this.primaryKey = primaryKey;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

}
