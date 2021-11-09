package net.leanix.azurecustomlogs;

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
