package net.leanix.azurecustomlogs;

import static java.util.stream.Collectors.groupingBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vavr.control.Try;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.leanix.azurecustomlogs.BaseMetadata.BaseMetadataBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * Core class that sends request logs to an Azure Log Analytics Workspace.
 * See https://docs.microsoft.com/en-us/azure/azure-monitor/platform/data-collector-api
 *
 * The incoming requests are asynchronously sent in order to avoid blocking IO on the request thread.
 * Batching reduces HTTP overhead and the number of signature calculations (HmacSHA256, see below).
 */
public final class LogAnalytics {

    // see: https://docs.microsoft.com/en-us/azure/azure-monitor/logs/data-collector-api#request-headers
    // Azure requires the RFC-7231 date format (despite what they have written in their docu). Which requires 2 digit days,
    // RFC-1123 uses single day digits if <10 which will result in errors with Azure Log Analytics
    public static final DateTimeFormatter RFC_7231_DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");
    // Object mapper required to prepare our internal objects for API sending */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String USER_AGENT = "OkHttp3 " + LogAnalytics.class.getName();
    // A circuit breaker for better resilience
    private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("Azure Log Analytics");
    private final OkHttpClient client;
    // Main Azure workspace configuration attributes
    private final String azureWorkspaceUrl;
    private final String workspaceId;
    private final String workspaceKey;
    private final ApplicationConfiguration applicationConfiguration;
    // Main Observer/Observable to take care of asynchronous log sending
    private final Subject<BaseMetadata> inputSubject = PublishSubject.<BaseMetadata>create().toSerialized();
    // Toggle to gracefully catch environments where logging is not configured
    private final boolean enabled;

    public LogAnalytics(LogAnalyticsWorkspaceConfiguration config, ApplicationConfiguration applicationConfiguration)
    {
        this.enabled = config.getWorkspaceId() != null && config.getPrimaryKey() != null;
        this.workspaceId = config.getWorkspaceId();
        this.workspaceKey = config.getPrimaryKey();
        this.applicationConfiguration = applicationConfiguration;
        this.azureWorkspaceUrl = "https://" + workspaceId + ".ods.opinsights.azure.com/api/logs?api-version=2016-04-01";
        this.inputSubject
            .buffer(1, TimeUnit.SECONDS, 100)
            .filter(batch -> !batch.isEmpty())
            .observeOn(Schedulers.io())
            .subscribe(batch -> {
                try {
                    // In not configured environments (such as a test instance) we simply log instead.
                    if (!enabled) {
                        fallbackToStandardLogger(batch);
                        return; // no-op
                    }
                    sendBatchWithCircuitBreaker(batch);
                } catch (Throwable t) {
                    // Ensures that subscribe() never throws an exception to not break the reactive stream.
                    // Ignore errors as they are already logged by sendBatch().
                }
            });

        // Changing the level will drastically increase log volume to stdout -> Logentries.
        // Only for local testing, not for use in production!
        HttpLoggingInterceptor clientLoggingInterceptor = new HttpLoggingInterceptor();
        clientLoggingInterceptor.setLevel(Level.NONE);

        this.client = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(clientLoggingInterceptor)
            .build();
    }

    private static Optional<String> toJson(BaseMetadata metadata) {
        try {
            return Optional.of(OBJECT_MAPPER.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            // ignored
            return Optional.empty();
        }
    }

    private static Optional<String> toJson(List<BaseMetadata> metadataBatch) {
        try {
            return Optional.of(OBJECT_MAPPER.writeValueAsString(metadataBatch));
        } catch (JsonProcessingException e) {
            // ignored
            return Optional.empty();
        }
    }

    /**
     * Sends a request log asynchronously to Azure Log Analytics Workspace.
     *
     * @param logAnalyticsMetadata the metadata to be sent based on the current builder.
     */
    public <A extends BaseMetadata, B extends BaseMetadataBuilder<?, ?>> void sendAsync(BaseMetadataBuilder<A, B> logAnalyticsMetadata) {
        inputSubject.onNext(logAnalyticsMetadata
            .appConfiguration(applicationConfiguration)
            .build());
    }

    /**
     * Core logic to send the logs to Azure.
     *
     * @param batch A batch of custom logs received during the last interval of our Subject
     * @return Void
     */
    private Void sendBatch(List<BaseMetadata> batch) {
        Optional<String> optionalElements = toJson(batch);
        if (optionalElements.isEmpty()) {
            // Ignored
            return null;
        }
        byte[] bodyBytes = optionalElements.get().getBytes(StandardCharsets.UTF_8);
        int bodyLength = bodyBytes.length;

        String nowRfc7231 = RFC_7231_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
        String authorization = createAuthorization(workspaceId, workspaceKey, bodyLength, nowRfc7231);

        // We know that this is true for all elements of the batch due to the grouping in #sendBatchWithCircuitBreaker
        final String logType = batch.get(0).getLogType();

        RequestBody requestBody = RequestBody.create(optionalElements.get(), okhttp3.MediaType.parse("application/json"));
        Request request = new Request.Builder()
            .url(azureWorkspaceUrl)
            .post(requestBody)
            .header("User-Agent", USER_AGENT)
            .header("Authorization", authorization)
            .header("Log-Type", logType)
            .header("x-ms-date", nowRfc7231)
            .header("time-generated-field", CustomLogRequestMetadata.TIMESTAMP_FIELD_NAME)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                final String error = String.format(
                    "Post of log batch not successful. batchSize=%s, bodyLength=%s, code=%s",
                    batch.size(), bodyLength, response.code());
                throw new Exception(error);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Unable to post log batch", e);
        }
    }

    private void sendBatchWithCircuitBreaker(List<BaseMetadata> batch) {
        batch.stream()
            // This is important: We need to batch the incoming logs by type otherwise we mix them up when sending
            // as we have to set the Log-Type header properly.
            .collect(groupingBy(BaseMetadata::getLogType))
            .forEach((type, typeBatch) -> {
                Supplier<Void> sender = () -> sendBatch(typeBatch);
                Supplier<Void> senderWithCircuitBreaker = CircuitBreaker.decorateSupplier(circuitBreaker, sender);
                // Fallback if circuit breaker is open or post was not successful
                Try.ofSupplier(senderWithCircuitBreaker).recover(throwable -> fallbackToStandardLogger(typeBatch));
            });
    }

    private String createAuthorization(String workspaceId, String workspaceKey, long contentLength, String rfc1123Date) {
        try {
            // See https://docs.microsoft.com/en-us/azure/azure-monitor/platform/data-collector-api#authorization
            String signature = String
                .format("POST\n%d\n%s\nx-ms-date:%s\n/api/logs", contentLength, "application/json; charset=utf-8", rfc1123Date);
            Mac mac = Mac.getInstance("HmacSHA256");
            Base64.Decoder decoder = Base64.getDecoder();
            SecretKeySpec secretKey = new SecretKeySpec(
                decoder.decode(workspaceKey.getBytes(StandardCharsets.UTF_8)), "HmacSHA256");
            mac.init(secretKey);
            Base64.Encoder encoder = Base64.getEncoder();
            String hashedSignature = new String(encoder.encode(mac.doFinal(signature.getBytes(StandardCharsets.UTF_8))));
            return String.format("SharedKey %s:%s", workspaceId, hashedSignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private Void fallbackToStandardLogger(List<BaseMetadata> batch) {
        batch.stream()
            .map(LogAnalytics::toJson)
            .flatMap(Optional::stream)
            .forEach(System.out::println);
        return null;
    }
}
