package uk.ac.ox.ctl.canvasproxy;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import java.util.HashMap;
import java.util.Map;

/**
 * This allows the tokens for a user to be renewed. If the user already has a token it will be removed before
 * getting them the re-authorize the application.
 */
@Controller()
@RequestMapping("/tokens")
public class CheckController {
    
    /**
     * This is the endpoint the user returns to once they have successfully granted their token
     */
    @GetMapping("/check")
    public ModelAndView doGet(AbstractOAuth2TokenAuthenticationToken<Jwt> authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client) {
        Map<String, Object> model = new HashMap<>();
        model.put("applicationName", client.getClientRegistration().getClientName());
        model.put("target", authenticationToken.getToken().getClaim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri"));
        model.put("error", "None");
        model.put("message", "None");
        return new ModelAndView("login-done", model);
    }

    /**
     * This removes the existing token for a user and then has them re-grant a token to the service.
     * This is badly named as it doesn't really check, but instead renews the refresh token.
     * 
     * @param client The existing client tokens.
     */
    @PostMapping("/check")
    public ModelAndView doPost(@RegisteredOAuth2AuthorizedClient(renew = true) OAuth2AuthorizedClient client) {
        // We never expect a request to make it here as the filter should redirect the client
        throw new IllegalStateException("We should never make it here.");
    }

}
