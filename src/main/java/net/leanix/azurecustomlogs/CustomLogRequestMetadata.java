package net.leanix.azurecustomlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A custom log for analysing requests with custom attributes. In this example it is of course very simple and
 * the duration and user agent is usually built-in to other APM solutions or Azure's AppInsights. But as this is custom
 * code you can extend it with whatever you want to trace, especially data that is inaccessible to APM solutions.
 */
@JsonInclude(Include.NON_NULL)
public class CustomLogRequestMetadata extends BaseMetadata {

    @JsonProperty("UserAgent")
    private String userAgent;

    @JsonProperty("DurationSec")
    private int durationSec;

    public static class Builder extends BaseMetadataBuilder<CustomLogRequestMetadata, Builder> {

        public Builder() {
            super(CustomLogRequestMetadata.class);
        }

        public Builder userAgent(String userAgent) {
            this.obj.userAgent = stripNullSafe(userAgent);
            return this;
        }

        public Builder durationSec(int durationSec) {
            this.obj.durationSec = durationSec;
            return this;
        }
    }
}
