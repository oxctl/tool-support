package uk.ac.ox.ctl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import uk.ac.ox.ctl.ltiauth.controller.AllowedRoles;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * This imports configuration defined in properties into the database. This was initially developed to allow the
 * migration of storing configuration in the database, but it's also useful for development setups where the database
 * needs to be wiped, but the configuration needs to be put back in.
 */
@Service
@EnableConfigurationProperties({
        ConfigurationImporter.LtiClientProperties.class,
        ConfigurationImporter.ProxyClientProperties.class
})
public class ConfigurationImporter {

    private final Logger log = LoggerFactory.getLogger(ConfigurationImporter.class);

    private final LtiClientProperties ltiProperties;
    private final ProxyClientProperties proxyProperties;
    private final ToolRepository toolRepository;
    private final AllowedRoles allowedRoles;


    public ConfigurationImporter(
            ConfigurationImporter.LtiClientProperties ltiProperties,
            ConfigurationImporter.ProxyClientProperties proxyProperties,
            ToolRepository toolRepository,
            AllowedRoles allowedRoles) {
        this.ltiProperties = ltiProperties;
        this.proxyProperties = proxyProperties;
        this.toolRepository = toolRepository;
        this.allowedRoles = allowedRoles;
    }

    @PostConstruct
    public void init() {
        int lti = 0;
        int proxy = 0;
        for (Map.Entry<String, ClientRegistration> entry : OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(ltiProperties).entrySet()) {
            String key = entry.getKey();
            ClientRegistration registration = entry.getValue();
            if (toolRepository.findToolByLtiRegistrationId(key).isEmpty()) {
                ToolRegistrationLti registrationLti = ToolRegistrationUtilities.toToolRegistrationLti(registration);
                Tool tool = new Tool();
                tool.setLti(registrationLti);
                toolRepository.save(tool);
                lti++;
            }
        }
        for (Map.Entry<String, ClientRegistration> entry : OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(proxyProperties).entrySet()) {
            String key = entry.getKey();
            ClientRegistration registration = entry.getValue();
            if (toolRepository.findToolByProxyRegistrationId(key).isEmpty()) {
                ToolRegistrationProxy registrationProxy = ToolRegistrationUtilities.toToolRegistrationProxy(registration);
                Tool tool = new Tool();
                tool.setProxy(registrationProxy);
                toolRepository.save(tool);
                proxy++;
            }
        }

        log.info("Imported {}/{} lti configurations, {}/{} proxy configurations.",
                lti, ltiProperties.getRegistration().size(), proxy, proxyProperties.getRegistration().size());
    }

    /**
     * This allows us to have both LTI and Proxy configuration in the same file and loaded at the same time.
     */
    @ConfigurationProperties(prefix = "lti.client")
    public static class LtiClientProperties extends OAuth2ClientProperties{
        
    }

    /**
     * This allows us to have both LTI and Proxy configuration in the same file and loaded at the same time.
     */
    @ConfigurationProperties(prefix = "proxy.client")
    public static class ProxyClientProperties extends OAuth2ClientProperties{

    }
    
}
