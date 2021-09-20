package net.leanix.aclutil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BaseMetadataTest {

    @SuppressWarnings("unused")
    public static Stream<Arguments> normalizeUserAgentTestData() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("", null),
            Arguments.of("  ", null), // Tab character
            Arguments.of("  /1.5     (  )", null),
            Arguments.of("Integration API", "Integration API"),
            Arguments.of("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:78.0) Gecko/20100101 Firefox/78.0", "Mozilla"),
            Arguments.of("Swagger-Codegen/1.0.0/java", "Swagger-Codegen"),
            Arguments.of("python-requests/2.24.0", "python-requests"),
            Arguments.of("node-fetch/1.0 (+https://github.com/bitinn/node-fetch)	", "node-fetch"),
            Arguments.of("Jakarta Commons-HttpClient/3.1", "Jakarta Commons-HttpClient")
        );
    }

    @ParameterizedTest
    @MethodSource("normalizeUserAgentTestData")
    void normalizeUserAgent(final String inputString, final String expectedOutput) {
        final String output = BaseMetadata.normalizeUserAgent(inputString);
        assertThat(output).isEqualTo(expectedOutput);
    }
}
