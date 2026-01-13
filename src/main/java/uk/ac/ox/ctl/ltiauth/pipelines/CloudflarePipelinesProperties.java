package uk.ac.ox.ctl.ltiauth.pipelines;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.net.URI;
import java.util.Optional;

/**
 * Configuration for sending events to Cloudflare Pipelines.
 */
@ConfigurationProperties(prefix = "cloudflare.pipelines")
public class CloudflarePipelinesProperties {

    private final URI launchEndpoint;
    private final String authToken;
    private final Integer maxQueueSize;

    @ConstructorBinding
    public CloudflarePipelinesProperties(URI launchEndpoint, String authToken, Integer maxQueueSize) {
        this.launchEndpoint = launchEndpoint;
        this.authToken = authToken;
        this.maxQueueSize = maxQueueSize;
    }

    public Optional<URI> getLaunchEndpoint() {
        return Optional.ofNullable(launchEndpoint);
    }

    public String getAuthToken() {
        return authToken;
    }

    public int getMaxQueueSize() {
        return maxQueueSize != null ? maxQueueSize : 100;
    }
}
