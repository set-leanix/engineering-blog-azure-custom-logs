package net.leanix.azurecustomlogs;

/**
 * Global configuration for your entire application instance. This might contain information about your pods,
 * the region, a server it is running on, etc. This information will be appended to any custom log you're sending to Azure.
 */
public class ApplicationConfiguration {

    private final String serverName;
    private final String regionName;

    public ApplicationConfiguration(final String serverName, final String regionName) {
        this.serverName = serverName;
        this.regionName = regionName;
    }

    public String getServerName() {
        return serverName;
    }

    public String getRegionName() {
        return regionName;
    }
}
