package net.leanix.azurecustomlogs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import java.time.Instant;

/**
 * Base metadata for all specialised custom logs. While this data structure also serves the purpose as interface to
 * be accessed in {@link LogAnalytics} it also provides a basic Builder for shared attributes (here for example
 * server, region, or workspaceId) and attributes you want to automatically generated once the event is ready to be sent,
 * i.e., first and foremost its timestamp when finally calling the build() method. This class could also provide
 * common helper functions such as the stripNullSafe() provided here.
 */
@JsonInclude(Include.NON_NULL)
public abstract class BaseMetadata {

    public static final String TIMESTAMP_FIELD_NAME = "Timestamp";

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty(TIMESTAMP_FIELD_NAME)
    protected Instant timestamp;

    @JsonProperty("WorkspaceId")
    protected String workspaceId;

    @JsonProperty("WorkspaceName")
    protected String workspaceName;

    @JsonProperty("Server")
    protected String server;

    @JsonProperty("Region")
    protected String region;

    @JsonIgnore
    protected String logType;

    protected static String stripNullSafe(String string) {
        if (string == null) {
            return null;
        }
        return string.strip();
    }

    public String getLogType() {
        return logType;
    }

    @SuppressWarnings("unchecked")
    public static class BaseMetadataBuilder<A extends BaseMetadata, B extends BaseMetadataBuilder<?, ?>> {

        protected A obj;

        public BaseMetadataBuilder(Class<A> clazz) {
            try {
                obj = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate class " + clazz.getName());
            }
        }

        public B appConfiguration(ApplicationConfiguration applicationConfiguration) {
            if (applicationConfiguration == null) {
                return (B) this;
            }
            this.obj.region = stripNullSafe(applicationConfiguration.getRegionName());
            this.obj.server = stripNullSafe(applicationConfiguration.getServerName());
            return (B) this;
        }

        public B workspaceId(String workspaceId) {
            this.obj.workspaceId = stripNullSafe(workspaceId);
            return (B) this;
        }

        public B workspaceName(String workspaceName) {
            this.obj.workspaceName = stripNullSafe(workspaceName);
            return (B) this;
        }

        public A build() {
            this.obj.timestamp = Instant.now();
            this.obj.logType = this.obj.getClass().getSimpleName();
            return obj;
        }
    }
}
