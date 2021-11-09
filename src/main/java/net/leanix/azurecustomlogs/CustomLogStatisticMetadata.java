package net.leanix.azurecustomlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A custom log that might be calculated through a cron job every other hour to gather in-depth statistics about different
 * scopes of your application, e.g., a per-customer dataset or other data.
 */
@JsonInclude(Include.NON_NULL)
public class CustomLogStatisticMetadata extends BaseMetadata {

    @JsonProperty("ObjectCount")
    private int objectCount;

    public static class Builder extends BaseMetadataBuilder<CustomLogStatisticMetadata, Builder> {

        public Builder() {
            super(CustomLogStatisticMetadata.class);
        }

        public Builder objectCount(int objectCount)
        {
            this.obj.objectCount = objectCount;
            return this;
        }
    }
}
