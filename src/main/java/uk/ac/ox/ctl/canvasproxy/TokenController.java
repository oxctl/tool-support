package uk.ac.ox.ctl.canvasproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller()
@RequestMapping("/tokens")
public class TokenController {

    private Logger log = LoggerFactory.getLogger(TokenController.class);

    @Autowired
    private OAuth2AuthorizedClientRepository clientRepository;

    @Autowired
    private AudienceConfiguration clientIdResolver;
    
    @Autowired
    private PrincipalOAuth2AuthorizedClientRepository principalOAuth2AuthorizedClientRepository;

    /**
     * This allows a tool to check to see if there is a valid refresh token for the current user.
     * This accepts a "force" parameter which if true forced the access token to be refreshed even if it is not near it's expiry
     * @return Unauthorized if there isn't and Ok if there is.
     */
    @GetMapping("/refresh")
    public ResponseEntity<Void> checkToken(JwtAuthenticationToken authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client, HttpServletRequest request, HttpServletResponse response) {
        final OAuth2AuthorizedClient oAuth2AuthorizedClient = principalOAuth2AuthorizedClientRepository.renewAccessToken(client.getClientRegistration().getRegistrationId(), authenticationToken, request, response);
        if (oAuth2AuthorizedClient == null) {
            // If we don't have a token or if it's no longer valid return unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check")
    public ModelAndView check(JwtAuthenticationToken authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client) {
        Map<String, Object> model = new HashMap<>();
        model.put("applicationName", client.getClientRegistration().getClientName());
        model.put("target", authenticationToken.getToken().getClaim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri"));
        model.put("error", "None");
        model.put("message", "None");
        return new ModelAndView("login-done", model);
    }

    /**
     * This removes the existing token for a user and then has them re-grant a token to the service.
     * @param authenticationToken The JWT token from the LTI launch.
     * @param client The existing client tokens.
     * @throws ClientAuthorizationRequiredException So that Spring starts the process of granting a token.
     */
    @PostMapping("/check")
    public ModelAndView delete(JwtAuthenticationToken authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String clientRegistrationId = client.getClientRegistration().getClientId();
        // TODO We should maybe have an attribute on the annotation that forces the removal of the token and an exception.
        // That way we don't have the controller aware of the authentication to client ID mapping.
        clientRepository.removeAuthorizedClient(clientRegistrationId, authenticationToken, servletRequest, servletResponse);
        log.info("Removed token for {}", authenticationToken.getName());
        String clientId = clientIdResolver.findClientId(authenticationToken);
        throw new ClientAuthorizationRequiredException(clientId);
    }

}
