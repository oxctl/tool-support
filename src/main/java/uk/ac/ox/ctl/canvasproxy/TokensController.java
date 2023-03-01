package uk.ac.ox.ctl.canvasproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.PrincipalClientIdResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller()
@RequestMapping("/tokens")
public class TokensController {

    private Logger log = LoggerFactory.getLogger(TokensController.class);

    @Autowired
    private OAuth2AuthorizedClientRepository clientRepository;

    @Autowired
    private PrincipalClientIdResolver clientIdResolver;

    /**
     * This is the endpoint the user returns to once they have successfully granted their token
     */
    @GetMapping("/check")
    public ModelAndView check(AbstractOAuth2TokenAuthenticationToken<Jwt> authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client) {
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
    public ModelAndView delete(AbstractOAuth2TokenAuthenticationToken<Jwt> authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        String clientRegistrationId = client.getClientRegistration().getClientId();
        // TODO We should maybe have an attribute on the annotation that forces the removal of the token and an exception.
        // That way we don't have the controller aware of the authentication to client ID mapping.
        clientRepository.removeAuthorizedClient(clientRegistrationId, authenticationToken, servletRequest, servletResponse);
        log.info("Removed token for {}", authenticationToken.getName());
        String clientId = clientIdResolver.findClientId(authenticationToken);
        throw new ClientAuthorizationRequiredException(clientId);
    }

}
