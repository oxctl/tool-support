package uk.ac.ox.ctl.ltiauth;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.ac.ox.ctl.lti13.Lti13Configurer;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcLaunchFlowAuthenticationProvider;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OAuth2AuthorizationRequestRedirectFilter;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OAuth2LoginAuthenticationFilter;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OIDCInitiatingLoginRequestResolver;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OptimisticAuthorizationRequestRepository;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.PathOIDCInitiationRegistrationResolver;

import java.time.Duration;

/**
 * This overrides the standard configurer to add our token passing redirect along with allowing client
 * registration lookups by client ID instead of requiring a custom path (although that's still supported).
 */
public class CustomLti13Configurer extends Lti13Configurer {
    
    private final JWTService jwtService;
    private final ClientRegistrationService clientRegistrationService;

    public CustomLti13Configurer(JWTService jwtService, ClientRegistrationService clientRegistrationService) {
        // An alternative is to look these up at configuration time, but that is slightly more messy and less
        // discoverable about what's happening
        this.jwtService = jwtService;
        this.clientRegistrationService = clientRegistrationService;
    }

    @Override
    protected OptimisticAuthorizationRequestRepository configureRequestRepository() {
        MultiStateCacheAuthorizationRequestRepository repository =
                new MultiStateCacheAuthorizationRequestRepository(Duration.ofMinutes(1), 2);
        repository.setLimitIpAddress(limitIpAddresses);
        return new StatelessOptimisticAuthorizationRequestRepository(repository);
    }

    @Override
    protected OAuth2LoginAuthenticationFilter configureLoginFilter(ClientRegistrationRepository clientRegistrationRepository, OidcLaunchFlowAuthenticationProvider oidcLaunchFlowAuthenticationProvider, OptimisticAuthorizationRequestRepository authorizationRequestRepository) {
        OAuth2LoginAuthenticationFilter loginFilter = super.configureLoginFilter(clientRegistrationRepository, oidcLaunchFlowAuthenticationProvider, authorizationRequestRepository);
        loginFilter.setAuthenticationSuccessHandler(new TokenPassingUriAuthenticationSuccessHandler(authorizationRequestRepository, jwtService));
        return loginFilter;
    }

    @Override
    protected OAuth2AuthorizationRequestRedirectFilter configureInitiationFilter(ClientRegistrationRepository clientRegistrationRepository, OptimisticAuthorizationRequestRepository authorizationRequestRepository) {
        PathOIDCInitiationRegistrationResolver pathResolver = new PathOIDCInitiationRegistrationResolver(this.ltiPath + this.loginInitiationPath);
        OIDCInitiatingLoginRequestResolver resolver = new OIDCInitiatingLoginRequestResolver(clientRegistrationRepository, new ClientIdOIDCInitiationRegistrationResolver(clientRegistrationService, pathResolver));
        OAuth2AuthorizationRequestRedirectFilter filter = new OAuth2AuthorizationRequestRedirectFilter(resolver);
        filter.setAuthorizationRequestRepository(authorizationRequestRepository);
        return filter;
    }

}
