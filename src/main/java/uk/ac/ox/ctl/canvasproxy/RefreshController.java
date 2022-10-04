package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationToken;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/tokens")
public class RefreshController {

    @Autowired
    private PrincipalOAuth2AuthorizedClientRepository principalOAuth2AuthorizedClientRepository;

    /**
     * This allows a tool to check to see if there is a valid refresh token for the current user.
     * This accepts a "force" parameter which if true forced the access token to be refreshed even if it is not near its expiry,
     * otherwise it will just assume a valid token in the DB is good enough.
     * @return Unauthorized if there isn't and Ok if there is.
     */
    @GetMapping("/refresh")
    public ResponseEntity<Void> checkToken(PersistableJwtAuthenticationToken authenticationToken, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client, HttpServletRequest request, HttpServletResponse response) {
        final OAuth2AuthorizedClient oAuth2AuthorizedClient = principalOAuth2AuthorizedClientRepository.renewAccessToken(client.getClientRegistration().getRegistrationId(), authenticationToken, request, response);
        if (oAuth2AuthorizedClient == null) {
            // If we don't have a token or if it's no longer valid return unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }
}
