package uk.ac.ox.ctl.ltiauth;

import org.springframework.security.oauth2.client.registration.ClientRegistration;

public interface ClientRegistrationService {
    ClientRegistration findByClientId(String clientId);
}
