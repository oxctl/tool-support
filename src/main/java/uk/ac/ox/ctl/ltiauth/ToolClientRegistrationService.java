package uk.ac.ox.ctl.ltiauth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import uk.ac.ox.ctl.repository.ToolRepository;
import uk.ac.ox.ctl.service.ToolRegistrationUtilities;

/**
 * Provides additional services for client registrations.
 * @see #findByClientId(String)
 */
public class ToolClientRegistrationService implements ClientRegistrationService {
    
    private final ToolRepository toolRepository;

    public ToolClientRegistrationService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }


    /**
     * Allow lookups for client registrations by client IDs. Normally the name is used (which could be the client ID),
     * but this makes it explicit.
     * @param clientId The client ID.
     * @return The ClientRegistration or null.
     */
    @Override
    public ClientRegistration findByClientId(String clientId) {
        return toolRepository.findToolByLtiClientId(clientId)
                .map(tool -> ToolRegistrationUtilities.toClientRegistration(tool.getLti()))
                .orElse(null);
    }

}
