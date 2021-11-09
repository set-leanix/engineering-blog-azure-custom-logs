package net.leanix.azurecustomlogs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import java.time.Instant;

@JsonInclude(Include.NON_NULL)
public abstract class BaseMetadata {

    public static final String TIMESTAMP_FIELD_NAME = "Timestamp";
    public static final int AZURE_MAX_FIELD_LIMIT_CHARS = 32766;

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

    @SuppressWarnings("unchecked")  // return (B) this;
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

    protected static String stripNullSafe(String string) {
        if (string == null) {
            return null;
        }
        return string.strip();
    }

    protected static String normalizeUserAgent(String userAgent) {
        if (userAgent == null) {
            return userAgent;
        }
        userAgent = userAgent.strip();
        userAgent = userAgent.replaceAll(" \\(.*$", ""); // remove User-Agent details
        userAgent = userAgent.replaceAll("/.*$", ""); // remove User-Agent versions
        userAgent = userAgent.replaceAll("\\s+", " "); // remove surplus whitespaces
        userAgent = userAgent.strip(); // strip again
        return userAgent.isEmpty() ? null : userAgent; // Don't set empty string values
    }

    public String getLogType() {
        return logType;
    }
}
