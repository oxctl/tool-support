package uk.ac.ox.ctl.ltiauth.pipelines;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes LTI launch events to Cloudflare Pipelines, this allows reporting on who is using our tools.
 * We send across lots of claims, but no directly PII.
 */
public class LtiLaunchEventService {

    private static final String CLAIM_CONTEXT = "https://purl.imsglobal.org/spec/lti/claim/context";
    private static final String CLAIM_DEPLOYMENT_ID = "https://purl.imsglobal.org/spec/lti/claim/deployment_id";
    private static final String CLAIM_LAUNCH_PRESENTATION = "https://purl.imsglobal.org/spec/lti/claim/launch_presentation";
    private static final String CLAIM_RESOURCE_LINK = "https://purl.imsglobal.org/spec/lti/claim/resource_link";
    private static final String CLAIM_ROLES = "https://purl.imsglobal.org/spec/lti/claim/roles";
    private static final String CLAIM_TARGET_LINK_URI = "https://purl.imsglobal.org/spec/lti/claim/target_link_uri";
    private static final String CLAIM_TOOL_PLATFORM = "https://purl.imsglobal.org/spec/lti/claim/tool_platform";

    private final Logger log = LoggerFactory.getLogger(LtiLaunchEventService.class);

    private final RestTemplate restTemplate;
    private final CloudflarePipelinesProperties properties;
    private final Clock clock;
    private final Executor executor;

    public LtiLaunchEventService(RestTemplate restTemplate, CloudflarePipelinesProperties properties) {
        this(restTemplate, properties, Clock.systemUTC(), null);
    }

    LtiLaunchEventService(RestTemplate restTemplate, CloudflarePipelinesProperties properties, Clock clock, Executor executor) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.clock = clock;
        this.executor = executor != null ? executor : createBoundedExecutor();
    }

    /**
     * Publish a launch event with a subset of useful LTI claims.
     * @param token The authenticated token containing LTI claims.
     * @param targetLinkUri The target link URI from the launch request.
     * @param toolSupportUrl The URL of this Tool Support instance.
     */
    public void publishLaunchEvent(OAuth2AuthenticationToken token, String targetLinkUri, String toolSupportUrl) {
        Optional<URI> maybeEndpoint = properties.getLaunchEndpoint();
        if (maybeEndpoint.isEmpty()) {
            return;
        }

        if (token.getAuthorizedClientRegistrationId() == null || token.getAuthorizedClientRegistrationId().isBlank()) {
            log.warn("Skipping LTI launch event publish because registration ID is missing.");
            return;
        }

        Map<String, Object> payload = buildPayload(token, targetLinkUri, toolSupportUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String authToken = properties.getAuthToken();
        if (authToken != null && !authToken.isBlank()) {
            log.debug("Setting auth token for Cloudflare Pipelines LTI launch event.");
            headers.setBearerAuth(authToken);
        }
        
        URI endpoint = maybeEndpoint.get();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        CompletableFuture.runAsync(() -> {
            try {
                ResponseEntity<Void> response = restTemplate.postForEntity(endpoint, entity, Void.class);
                if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                    if (HttpStatus.FORBIDDEN.equals(response.getStatusCode()) || HttpStatus.UNAUTHORIZED.equals(response.getStatusCode())) {
                        log.error("Cloudflare Pipelines ({}) rejected LTI launch event ({}). Check auth token and permissions.", endpoint, response.getStatusCode());
                    } else {
                        log.warn("Cloudflare Pipelines ({}) returned non-success status {} for LTI launch event.", endpoint, response.getStatusCode());
                    }
                }
            } catch (RestClientException e) {
                log.warn("Failed to post LTI launch event to Cloudflare Pipelines ({})", endpoint, e);
            }
        }, this.executor);
    }

    private Executor createBoundedExecutor() {
        int maxQueue = Math.max(1, properties.getMaxQueueSize());
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, // core pool size
                2, // max pool size
                30, // idle thread timeout
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(maxQueue),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("lti-launch-events-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                },
                (runnable, executor) -> log.warn("Dropping LTI launch event post to Cloudflare Pipelines because queue is full ({} items)", executor.getQueue().size())
        );
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    Map<String, Object> buildPayload(OAuth2AuthenticationToken token, String targetLinkUri, String toolSupportUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_type", "lti_launch");
        payload.put("timestamp", Instant.now(clock).toEpochMilli());
        payload.put("registration_id", token.getAuthorizedClientRegistrationId());
        if (toolSupportUrl != null) {
            payload.put("tool_support_url", toolSupportUrl);
        }
        if (targetLinkUri != null) {
            payload.put("target_link_uri", targetLinkUri);
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        copyClaim(token, claims, "iss");
        copyClaim(token, claims, "sub");
        copyClaim(token, claims, "aud");
        copyClaim(token, claims, CLAIM_ROLES);
        copyClaim(token, claims, CLAIM_DEPLOYMENT_ID);
        copyClaim(token, claims, CLAIM_RESOURCE_LINK);
        copyClaim(token, claims, CLAIM_CONTEXT);
        copyClaim(token, claims, CLAIM_LAUNCH_PRESENTATION);
        copyClaim(token, claims, CLAIM_TOOL_PLATFORM);
        copyClaim(token, claims, CLAIM_TARGET_LINK_URI);

        payload.put("claims", claims);
        return payload;
    }

    private void copyClaim(OAuth2AuthenticationToken token, Map<String, Object> claims, String claimName) {
        Object value = token.getPrincipal().getAttribute(claimName);
        if (value != null) {
            claims.put(claimName, value);
        }
    }
}
