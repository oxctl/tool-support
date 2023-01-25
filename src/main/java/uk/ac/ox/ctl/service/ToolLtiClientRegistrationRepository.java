package uk.ac.ox.ctl.service;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.ac.ox.ctl.repository.ToolRepository;

public class ToolLtiClientRegistrationRepository implements ClientRegistrationRepository {

    private final ToolRepository toolRepository;

    public ToolLtiClientRegistrationRepository(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return toolRepository.findToolByLtiRegistrationId(registrationId)
                .map(tool -> ToolRegistrationUtilities.toClientRegistration(tool.getLti())).orElse(null);
    }
}
