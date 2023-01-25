package uk.ac.ox.ctl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import uk.ac.ox.ctl.canvasproxy.AudienceConfiguration;
import uk.ac.ox.ctl.ltiauth.LtiSettings;
import uk.ac.ox.ctl.ltiauth.controller.AllowedRoles;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.Map;

/**
 * This imports configuration defined in properties into the database. This was initially developed to allow the
 * migration of storing configuration in the database, but it's also useful for development setups where the database
 * needs to be wiped, but the configuration needs to be put back in.
 */
@Service
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class ConfigurationImporter {

    private final Logger log = LoggerFactory.getLogger(ConfigurationImporter.class);

    private final OAuth2ClientProperties properties;
    private final ToolRepository toolRepository;
    private final AudienceConfiguration audienceConfiguration;
    private final LtiSettings ltiSettings;
    private final AllowedRoles allowedRoles;


    public ConfigurationImporter(OAuth2ClientProperties properties, ToolRepository toolRepository, AudienceConfiguration audienceConfiguration, LtiSettings ltiSettings, AllowedRoles allowedRoles) {
        this.properties = properties;
        this.toolRepository = toolRepository;
        this.audienceConfiguration = audienceConfiguration;
        this.ltiSettings = ltiSettings;
        this.allowedRoles = allowedRoles;
    }

    @PostConstruct
    public void init() {
        final Map<String, ClientRegistration> clientRegistrations = OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties);
        int lti = 0;
        int proxy = 0;
        for (Map.Entry<String, ClientRegistration> entry : clientRegistrations.entrySet()) {
            String key = entry.getKey();
            ClientRegistration registration = entry.getValue();
            if (registration.getClientSecret() == null || registration.getClientSecret().isEmpty()) {
                // This is a LTI registration.
                if (toolRepository.findToolByLtiRegistrationId(key).isEmpty()) {
                    ToolRegistrationLti registrationLti = ToolRegistrationUtilities.toToolRegistrationLti(registration);
                    Tool tool = new Tool();
                    tool.setLti(registrationLti);
                    registrationLti.setTool(tool);
                    toolRepository.save(tool);
                    lti++;
                }
            } else {
                if (toolRepository.findToolByProxyRegistrationId(key).isEmpty()) {
                    ToolRegistrationProxy registrationProxy = ToolRegistrationUtilities.toToolRegistrationProxy(registration);
                    Tool tool = new Tool();
                    tool.setProxy(registrationProxy);
                    registrationProxy.setTool(tool);
                    toolRepository.save(tool);
                    proxy++;
                }
            }
        }

        int linked = 0;
        // For all the LTI tools, check to see if there's a proxy tool we should link up.
        for (Tool ltiTool : toolRepository.findToolByLtiIdNotNull()) {
            AudienceConfiguration.LtiAudience ltiAudience = audienceConfiguration.getLtiAudience(ltiTool.getLti().getClientId());
            if (ltiAudience != null) {
                if (ltiAudience.getClientName() != null) {
                    if (ltiTool.getProxy() != null) {
                        log.warn("Proxy already set on {}, not merging.", ltiTool.getId());
                        continue;
                    }
                    Tool proxyTool = toolRepository.findToolByProxyRegistrationId(ltiAudience.getClientName()).orElse(null);
                    if (proxyTool != null) {
                        if (ltiAudience.getSecret() != null) {
                            String secret = Base64.getEncoder().encodeToString(ltiAudience.getSecret());
                            ltiTool.setSecret(secret);
                        }
                        if (ltiAudience.getIssuer() != null) {
                            String issuer = ltiAudience.getIssuer();
                            ltiTool.setIssuer(issuer);
                        }
                        ToolRegistrationProxy registrationProxy = proxyTool.getProxy();
                        toolRepository.delete(proxyTool);
                        // Allow it to be re-linked.
                        proxyTool.setId(null);
                        ltiTool.setProxy(registrationProxy);
                        registrationProxy.setTool(ltiTool);
                        toolRepository.save(ltiTool);
                        linked++;
                    } else {
                        log.warn("Failed to find proxy tool with registration of {} for LTI tool {}", ltiAudience.getClientName(), ltiTool.getId());
                    }
                }
            }
            LtiSettings.ClientSettings clientSettings = ltiSettings.getClientSettings(ltiTool.getLti().getRegistrationId());
            if (clientSettings != null) {
                ltiTool.setSign(clientSettings.isSign());
            }
            AllowedRoles.Roles roles = allowedRoles.getClient().get(ltiTool.getLti().getClientId());
            if (roles != null) {
                ltiTool.setNrpsAllowedRoles(roles.getRoles());
            }
        }
        log.info("Imported {} lti, {} proxy from {} configurations. Successfully linked {} proxy tools an LTI tool", lti, proxy, clientRegistrations.size(), linked);


    }
}
