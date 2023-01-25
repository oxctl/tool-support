package uk.ac.ox.ctl.ltiauth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.ac.ox.ctl.repository.ToolRepository;
import uk.ac.ox.ctl.service.ToolLtiClientRegistrationRepository;

@Configuration
public class ToolConfiguration {
    
    @Bean
    public ClientRegistrationService clientRegistrationService(ToolRepository toolRepository) {
        return new ToolClientRegistrationService(toolRepository);
    }
    
    @Bean
    public AllowedRolesService allowedRolesService(ToolRepository toolRepository) {
        return new AllowedRolesService(toolRepository);
    }

    @Bean
    @Qualifier("lti")
    public ClientRegistrationRepository ltiClientRegistrationRepository(ToolRepository toolRepository) {
        return new ToolLtiClientRegistrationRepository(toolRepository);
    }
}
