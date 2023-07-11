package uk.ac.ox.ctl.ltiauth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OIDCInitiationRegistrationResolver;

import javax.servlet.http.HttpServletRequest;

/**
 * Resolves the client registration when the login initiation endpoint is hit by using the client ID from the
 * request.
 */
public class ClientIdOIDCInitiationRegistrationResolver implements OIDCInitiationRegistrationResolver {

    private final ClientRegistrationService clientRegistrationService;
    private final OIDCInitiationRegistrationResolver firstResolver;

    public ClientIdOIDCInitiationRegistrationResolver(ClientRegistrationService clientRegistrationService, OIDCInitiationRegistrationResolver firstResolver) {
        this.clientRegistrationService = clientRegistrationService;
        this.firstResolver = firstResolver;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        // Try passed resolver first
        String registrationId = firstResolver.resolve(request);
        if (registrationId != null) {
            return registrationId;
        }

        String clientId = request.getParameter("client_id");
        if (clientId != null) {
            ClientRegistration registration = clientRegistrationService.findByClientId(clientId);
            if (registration != null) {
                // This is slightly suboptimal because we then look up the client registration by this ID.
                // but this fits with how the upstream API works.
                return registration.getRegistrationId();
            }
        }
        return null;
    }
}
