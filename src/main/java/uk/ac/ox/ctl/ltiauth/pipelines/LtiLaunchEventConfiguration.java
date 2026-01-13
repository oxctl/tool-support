package uk.ac.ox.ctl.ltiauth.pipelines;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Cloudflare Pipelines integrations.
 */
@Configuration
public class LtiLaunchEventConfiguration {

    @Bean
    public LtiLaunchEventService ltiLaunchEventService(RestTemplate restTemplate, CloudflarePipelinesProperties properties) {
        return new LtiLaunchEventService(restTemplate, properties);
    }
}
