package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import uk.ac.ox.ctl.ltiauth.TestClientRegistrationService;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class TestClientRegistrationConfig {

    @Bean
    @Qualifier("proxy")
    public InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository(OAuth2ClientProperties properties) {
        List<ClientRegistration> registrations = new ArrayList<>(
                OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());
        return new InMemoryClientRegistrationRepository(registrations);
    }
}
