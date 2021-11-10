package net.leanix.azurecustomlogs;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * A simple test suite to create and send queries to Azure or, if not available, to standard-out.
 */
class ExampleTest {

    private static final Random RANDOM = new Random();

    @Test
    void sendingData() {
        // Please note that for this test to work with Azure it's expected to provide your Azure Log Analytics configuration
        // via environment variables. If not it will gracefully fall back to standard logging.
        LogAnalyticsWorkspaceConfiguration config = new LogAnalyticsWorkspaceConfiguration(
            System.getenv("AZURE_LOG_ANALYTICS_WORKSPACE"),
            System.getenv("AZURE_LOG_ANALYTICS_KEY"));
        // Set up some global configuration that is common for all of your logs
        final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration("server-1.mydomain.net", "EMEA");
        // Set up the LogAnalytics subsystem. This has to happen somewhere in your application.
        final LogAnalytics logAnalytics = new LogAnalytics(config, applicationConfiguration);

        // Create some test log data
        final String workspaceId = UUID.randomUUID().toString();
        final String workspaceName = "workspace";
        final List<CustomLogRequestMetadata.Builder> requestMetadata = IntStream.range(0, 25)
            .mapToObj((i) -> new CustomLogRequestMetadata.Builder()
                .workspaceId(workspaceId)
                .workspaceName(workspaceName)
                .durationSec(RANDOM.nextInt(60))
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:78.0) Gecko/20100101 Firefox/78.0")
            ).collect(Collectors.toList());
        final List<CustomLogStatisticMetadata.Builder> statisticMetadata = IntStream.range(0, 25)
            .mapToObj((i) -> new CustomLogStatisticMetadata.Builder()
                .workspaceId(workspaceId)
                .workspaceName(workspaceName)
                .objectCount(RANDOM.nextInt(100))
            ).collect(Collectors.toList());

        // This is the default interface to send custom logs to Azure
        requestMetadata.forEach(logAnalytics::sendAsync);
        statisticMetadata.forEach(logAnalytics::sendAsync);

        // We need to sleep a bit otherwise the test terminates before the asynchronous process sent out the logs.
        // In a real situation this is not necessary of course.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
