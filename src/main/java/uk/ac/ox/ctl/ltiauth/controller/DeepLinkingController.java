package uk.ac.ox.ctl.ltiauth.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ox.ctl.ltiauth.ClientRegistrationService;
import uk.ac.ox.ctl.ltiauth.JWTSigner;

import java.net.URL;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static uk.ac.ox.ctl.lti13.lti.Claims.*;

/**
 * This can be used by tools wanting to support deep linking. An authenticated request can be made against
 * it along with some items to return and a signed JWT will be returned. The application can then send this
 * back to the platform (using a form POST).
 */
@RestController
public class DeepLinkingController {

    public static final String DL_CLAIM_DATA = "https://purl.imsglobal.org/spec/lti-dl/claim/data";
    public static final String DL_CLAIM_CONTENT_ITEMS = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items";
    public static final String DL_CLAIM_MSG = "https://purl.imsglobal.org/spec/lti-dl/claim/msg";
    public static final String DL_CLAIM_LOG = "https://purl.imsglobal.org/spec/lti-dl/claim/log";
    public static final String DL_CLAIM_ERRORMSG = "https://purl.imsglobal.org/spec/lti-dl/claim/errormsg";
    public static final String DL_CLAIM_ERRORLOG = "https://purl.imsglobal.org/spec/lti-dl/claim/errorlog";

    private final ClientRegistrationService clientRegistrationService;
    private final JWTSigner jwtSigner;
    private final Duration expiration = Duration.of(5, ChronoUnit.MINUTES);


    public DeepLinkingController(ClientRegistrationService clientRegistrationService, JWTSigner jwtSigner) {
        this.clientRegistrationService = clientRegistrationService;
        this.jwtSigner = jwtSigner;
    }

    // If we can control the response when there isn't a token for the current user we may want to make the token required.
    @PostMapping("/deep-linking/**")
    public Map<String, Object> proxy(JwtAuthenticationToken token, @RequestBody String contentItems) throws ParseException, JOSEException {
        Object principal = token.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            String messageType = jwt.getClaimAsString(MESSAGE_TYPE);
            if (!"LtiDeepLinkingRequest".equals(messageType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint can only be used by JWT tokens for deep linking flow");
            }
            // Need to map to from client ID/audience to registration ID.
            ClientRegistration clientRegistration = null;
            for (String aud : jwt.getAudience()) {
                clientRegistration = clientRegistrationService.findByClientId(aud);
                if (clientRegistration != null) {
                    break;
                }
            }
            if (clientRegistration == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed find client registration for: " + String.join(", ", jwt.getAudience()));
            }
            JWTClaimsSet claims;
            try {
                claims = JWTClaimsSet.parse(contentItems);
            } catch(ParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse: "+ e.getMessage());
            }
            String aud = jwt.getIssuer().toExternalForm();
            // If we re-signed the token then we need to use the original issuer.
            URL originalIssuer = jwt.getClaimAsURL("iss-orig");
            if (originalIssuer != null) {
                aud = originalIssuer.toExternalForm();
            }
            JWTClaimsSet cleanSet = new JWTClaimsSet.Builder()
                    .issuer(clientRegistration.getClientId())
                    .audience(aud)
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plus(expiration)))
                    .claim("azp", clientRegistration.getClientId())
                    .claim("nonce", UUID.randomUUID().toString())
                    .claim(MESSAGE_TYPE, "LtiDeepLinkingResponse")
                    .claim(LTI_VERSION, "1.3.0")
                    .claim(LTI_DEPLOYMENT_ID, jwt.getClaimAsString(LTI_DEPLOYMENT_ID))
                    .claim(DL_CLAIM_DATA, jwt.getClaimAsString(DL_CLAIM_DATA))
                    // Although we could copy everything across we want to be more careful about the claims so we are explicit in the claims we copy across.
                    .claim(DL_CLAIM_CONTENT_ITEMS, claims.getClaim(DL_CLAIM_CONTENT_ITEMS))

                    // And the messages
                    .claim(DL_CLAIM_MSG, claims.getStringClaim(DL_CLAIM_MSG))
                    .claim(DL_CLAIM_LOG, claims.getStringClaim(DL_CLAIM_LOG))
                    .claim(DL_CLAIM_ERRORMSG, claims.getStringClaim(DL_CLAIM_ERRORMSG))
                    .claim(DL_CLAIM_ERRORLOG, claims.getStringClaim(DL_CLAIM_ERRORLOG))

                    .build();

            SignedJWT signedJWT = jwtSigner.signJWT(clientRegistration.getRegistrationId(), cleanSet);
            return Collections.singletonMap("jwt", signedJWT.serialize());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not find required data");
    }


}
