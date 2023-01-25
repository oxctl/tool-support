package uk.ac.ox.ctl.ltiauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.ac.ox.ctl.lti13.Lti13Configurer;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcLaunchFlowAuthenticationProvider;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OAuth2LoginAuthenticationFilter;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OptimisticAuthorizationRequestRepository;

/**
 * This overrides the standard configurer to add our token passing redirecter.
 */
public class CustomLti13Configurer extends Lti13Configurer {
    
    private final Logger log = LoggerFactory.getLogger(CustomLti13Configurer.class);

    private final JWTService jwtService;

    public CustomLti13Configurer(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected OAuth2LoginAuthenticationFilter configureLoginFilter(ClientRegistrationRepository clientRegistrationRepository, OidcLaunchFlowAuthenticationProvider oidcLaunchFlowAuthenticationProvider, OptimisticAuthorizationRequestRepository authorizationRequestRepository) {
        OAuth2LoginAuthenticationFilter loginFilter = super.configureLoginFilter(clientRegistrationRepository, oidcLaunchFlowAuthenticationProvider, authorizationRequestRepository);
        loginFilter.setAuthenticationSuccessHandler(new TokenPassingUriAuthenticationSuccessHandler(authorizationRequestRepository, jwtService));
        return loginFilter;
    }
}
