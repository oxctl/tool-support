package uk.ac.ox.ctl.ltiauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides additional services for client registrations.
 * @see #findByClientId(String)
 */
public class TestClientRegistrationService implements ClientRegistrationService {
    
    private final Logger log = LoggerFactory.getLogger(ToolClientRegistrationService.class);

    private final Iterator<ClientRegistration> clientRegistrationsIt;

    private Map<String, ClientRegistration> clientIdLookup;

    public TestClientRegistrationService(Iterator<ClientRegistration> clientRegistrationsIt) {
        this.clientRegistrationsIt = clientRegistrationsIt;
    }

    @PostConstruct
    public void init() {
        clientIdLookup = new HashMap<>();
        for (Iterator<ClientRegistration> it = clientRegistrationsIt; it.hasNext(); ) {
            ClientRegistration clientRegistration = it.next();
            ClientRegistration old = clientIdLookup.put(clientRegistration.getClientId(), clientRegistration);
            if (old != null) {
                throw new IllegalStateException("There can be only one registration per client ID." +
                        "Multiple found for: "+ clientRegistration.getClientId());
            }
        }
        log.info("Loaded {} registrations.", clientIdLookup.size());
    }

    /**
     * Allow lookups for client registrations by client IDs. Normally the name is used (which could be the client ID),
     * but this makes it explicit.
     * @param clientId The client ID.
     * @return The ClientRegistration or null.
     */
    public ClientRegistration findByClientId(String clientId) {
        return clientIdLookup.get(clientId);
    }

}
