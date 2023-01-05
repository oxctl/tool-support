package uk.ac.ox.ctl.ltiauth;


import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OptimisticAuthorizationRequestRepository;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.StateCheckingAuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This handler takes the URL and adds a token onto it. This is so that a static HTML frontend can use the token
 * to retrieve the full JWT.
 */
public class TokenPassingUriAuthenticationSuccessHandler extends StateCheckingAuthenticationSuccessHandler {

    private final JWTService jwtService;

    public TokenPassingUriAuthenticationSuccessHandler(OptimisticAuthorizationRequestRepository authorizationRequestRepository, JWTService jwtService) {
        super(authorizationRequestRepository);
        this.jwtService = jwtService;
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken)authentication;
            // Because we are just giving the token out to this URL we need to trust that this URL can't be messed with.
            String targetLink = token.getPrincipal().getAttribute("https://purl.imsglobal.org/spec/lti/claim/target_link_uri");
            if (targetLink != null && !targetLink.isEmpty()) {
                Object obj = token;
                if (jwtService.isSigning(token.getAuthorizedClientRegistrationId())) {
                    // We create our own signed version of the token.
                    obj = jwtService.createJWT(token);
                }
                String key = jwtService.store(obj);
                // TODO We should parse the link and add the additional parameter correctly so we can pass parameters
                return targetLink + "?token="+ key;
            }
        }

        return super.determineTargetUrl(request, response, authentication);
    }
}


