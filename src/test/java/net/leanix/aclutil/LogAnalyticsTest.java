package net.leanix.aclutil;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LogAnalyticsTest {

    private static List<CustomLogMetadata> generateRequestMetadata(int n) {
        final String workspaceId = UUID.randomUUID().toString();
        final String workspaceName = "workspace";
        return IntStream.range(0, n).mapToObj((i) -> new CustomLogMetadata.Builder()
            .workspaceId(workspaceId)
            .workspaceName(workspaceName)
            .build()).collect(Collectors.toList());
    }

    private static LogAnalytics initTestLogAnalyticsSender() {
        LogAnalyticsWorkspaceConfiguration config = new LogAnalyticsWorkspaceConfiguration(
            System.getenv("AZURE_LOG_ANALYTICS_WORKSPACE"),
            System.getenv("AZURE_LOG_ANALYTICS_KEY"));
        return new LogAnalytics(config, null);
    }

    @Test
    void manualTest_sendSingleEntryForPFRequests() {
        initTestLogAnalyticsSender()
            .sendBatch(Collections.singletonList(generateRequestMetadata(1).get(0)));
    }
}
