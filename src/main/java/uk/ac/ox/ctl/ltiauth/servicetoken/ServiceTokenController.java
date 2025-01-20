package uk.ac.ox.ctl.ltiauth.servicetoken;

import com.nimbusds.jose.JOSEException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.ctl.lti13.TokenRetriever;
import uk.ac.ox.ctl.ltiauth.ClientRegistrationService;

import java.util.Map;

/**
 * This is for retrieving a LTI token from Canvas using the secret shared between tool support and the tool.
 */
@Slf4j
@RestController
public class ServiceTokenController {

    private final ClientRegistrationService clientRegistrationService;
    
    private final TokenRetriever tokenRetriever;

    public ServiceTokenController(ClientRegistrationService clientRegistrationService, TokenRetriever tokenRetriever) {
        this.clientRegistrationService = clientRegistrationService;
        this.tokenRetriever = tokenRetriever;
    }

    @PostMapping("/service-token")
    public ResponseEntity<Map<String, Object>> getLtiToken(JwtAuthenticationToken token, @RequestParam String[] scopes) throws JOSEException {
        Object principal = token.getPrincipal();
        if (principal instanceof Jwt jwt) {
            ClientRegistration clientRegistration = null;
            for (String aud : jwt.getAudience()) {
                clientRegistration = clientRegistrationService.findByClientId(aud);
                if (clientRegistration != null) {
                    break;
                }
            }
            if (clientRegistration == null) {
                throw new IllegalStateException("Failed find client registration for: " + String.join(", ", jwt.getAudience()));
            }
            OAuth2AccessTokenResponse tokenResponse = tokenRetriever.getToken(clientRegistration, scopes);
            return new ResponseEntity<>(Map.of(
                    "jwt", tokenResponse.getAccessToken().getTokenValue(),
                    "expires", tokenResponse.getAccessToken().getExpiresAt()), HttpStatus.OK
            );
        }
        return null;
    }

}
