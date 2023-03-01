package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.ac.ox.ctl.canvasproxy.repository.PrincipalTokensRepository;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.OAuth2AccessTokenRefresher;
import uk.ac.ox.ctl.repository.ToolRepository;
import uk.ac.ox.ctl.service.ToolProxyClientRegistrationRepository;

@Configuration
public class ProxyConfiguration {

    @Bean
    @Qualifier("proxy")
    public ClientRegistrationRepository proxyClientRegistrationRepository(ToolRepository toolRepository) {
        return new ToolProxyClientRegistrationRepository(toolRepository);
    }

    @Bean
    PrincipalOAuth2AuthorizedClientRepository principalOAuth2AuthorizedClientRepository(PrincipalTokensRepository principalTokensRepository, @Qualifier("proxy") ClientRegistrationRepository clientRegistrationRepository, OAuth2AccessTokenRefresher auth2AccessTokenRefresher) {
        return new PrincipalOAuth2AuthorizedClientRepository(principalTokensRepository, clientRegistrationRepository, auth2AccessTokenRefresher);
    }
}
