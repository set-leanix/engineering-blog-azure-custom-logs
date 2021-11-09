package net.leanix.azurecustomlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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
