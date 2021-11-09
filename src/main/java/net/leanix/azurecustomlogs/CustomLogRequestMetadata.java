package net.leanix.azurecustomlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class CustomLogRequestMetadata extends BaseMetadata {

    @JsonProperty("UserAgent")
    private String userAgent;

    @JsonProperty("UserAgentShort")
    private String userAgentShort;

    @JsonProperty("DurationSec")
    private int durationSec;

    public static class Builder extends BaseMetadataBuilder<CustomLogRequestMetadata, Builder> {

        public Builder() {
            super(CustomLogRequestMetadata.class);
        }

        public Builder userAgent(String userAgent) {
            this.obj.userAgent = stripNullSafe(userAgent);
            this.obj.userAgentShort = normalizeUserAgent(this.obj.userAgent);
            return this;
        }

        public Builder durationSec(int durationSec) {
            this.obj.durationSec = durationSec;
            return this;
        }
    }
}
