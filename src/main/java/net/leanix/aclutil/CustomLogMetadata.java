package net.leanix.aclutil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class CustomLogMetadata extends BaseMetadata {

    @JsonProperty("UserAgent")
    private String userAgent;

    @JsonProperty("UserAgentShort")
    private String userAgentShort;

    public static class Builder extends BaseMetadataBuilder<CustomLogMetadata, Builder> {

        public Builder() {
            super(CustomLogMetadata.class);
        }

        public Builder userAgent(String userAgent)
        {
            this.obj.userAgent = stripNullSafe(userAgent);
            this.obj.userAgentShort = normalizeUserAgent(this.obj.userAgent);
            return this;
        }
    }
}
