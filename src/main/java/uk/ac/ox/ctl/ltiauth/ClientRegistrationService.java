package uk.ac.ox.ctl.ltiauth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;

public interface ClientRegistrationService {
    /**
     * Allow lookups for client registrations by client IDs. Normally the name is used (which could be the client ID),
     * but this makes it explicit.
     * @param clientId The client ID.
     * @return The ClientRegistration or null.
     */
    ClientRegistration findByClientId(String clientId);
}
