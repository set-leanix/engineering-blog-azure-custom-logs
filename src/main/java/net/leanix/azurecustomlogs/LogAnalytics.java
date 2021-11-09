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
 * Sends request logs to an Azure Log Analytics Workspace.
 * See https://docs.microsoft.com/en-us/azure/azure-monitor/platform/data-collector-api
 *
 * The incoming requests are asynchronously sent in order to avoid blocking IO on the request thread.
 * Batching reduces HTTP overhead and the number of signature calculations (HmacSHA256, see below).
 */
public final class LogAnalytics {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // The Java built-in DateTimeFormatter.RFC_1123_DATE_TIME encodes day-of-month as a single digit, which the API doesn't like
    public static final DateTimeFormatter RFC_1123_DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");
    private static final String USER_AGENT = "OkHttp3 " + LogAnalytics.class.getName();

    private final OkHttpClient client;
    private final CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("Azure Log Analytics");
    private final String workspaceUrl;
    private final boolean enabled;
    private final String workspaceId;
    private final String workspaceKey;
    private final ApplicationConfiguration applicationConfiguration;
    private final Subject<BaseMetadata> inputSubject = PublishSubject.<BaseMetadata>create().toSerialized();

    public LogAnalytics(LogAnalyticsWorkspaceConfiguration config, ApplicationConfiguration applicationConfiguration)
    {

        enabled = config.isEnabled();
        workspaceId = config.getId();
        workspaceKey = config.getKey();
        this.applicationConfiguration = applicationConfiguration;
        workspaceUrl = "https://" + workspaceId + ".ods.opinsights.azure.com/api/logs?api-version=2016-04-01";
        inputSubject
            .buffer(1, TimeUnit.SECONDS, 100)
            .filter(batch -> !batch.isEmpty())
            .observeOn(Schedulers.io())
            .subscribe(batch -> {
                try {
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

        HttpLoggingInterceptor clientLoggingInterceptor = new HttpLoggingInterceptor();
        // Changing the level will drastically increase log volume to stdout -> Logentries.
        // Only for local testing, not for use in production!
        clientLoggingInterceptor.setLevel(Level.NONE);

        client = new OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(clientLoggingInterceptor)
            .build();
    }

    /**
     * Sends a request log asynchronously to Azure Log Analytics Workspace.
     *
     * @param logAnalyticsMetadata the metadata to be sent
     */
    public <A extends BaseMetadata, B extends BaseMetadataBuilder<?, ?>> void sendAsync(BaseMetadataBuilder<A, B> logAnalyticsMetadata) {
        inputSubject.onNext(logAnalyticsMetadata
            .appConfiguration(applicationConfiguration)
            .build());
    }

    private Void sendBatch(List<BaseMetadata> batch) {
        if (!this.enabled) {
            fallbackToStandardLogger(batch);
            return null;
        }
        Optional<String> optionalElements = toJson(batch);
        if (optionalElements.isEmpty()) {
            // Ignored
            return null;
        }
        byte[] bodyBytes = optionalElements.get().getBytes(StandardCharsets.UTF_8);
        int bodyLength = bodyBytes.length;

        String nowRfc1123 = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
        String authorization = createAuthorization(workspaceId, workspaceKey, bodyLength, nowRfc1123);

        // We know that this is true for all elements of the batch due to the grouping in #sendBatchWithCircuitBreaker
        final String logType = batch.get(0).getLogType();

        RequestBody requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json"), optionalElements.get());
        Request request = new Request.Builder()
            .url(workspaceUrl)
            .post(requestBody)
            .header("User-Agent", USER_AGENT)
            .header("Authorization", authorization)
            .header("Log-Type", logType)
            .header("x-ms-date", nowRfc1123)
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

    private static Optional<String> toJson(BaseMetadata requestMetadata) {
        try {
            return Optional.of(OBJECT_MAPPER.writeValueAsString(requestMetadata));
        } catch (JsonProcessingException e) {
            // ignored
            return Optional.empty();
        }
    }

    private Optional<String> toJson(List<BaseMetadata> batch) {
        try {
            return Optional.of(OBJECT_MAPPER.writeValueAsString(batch));
        } catch (JsonProcessingException e) {
            // ignored
            return Optional.empty();
        }
    }

    private Void fallbackToStandardLogger(List<BaseMetadata> batch) {
        batch.stream()
            .map(LogAnalytics::toJson)
            .flatMap(Optional::stream)
            .forEach(System.err::println);
        return null;
    }
}
