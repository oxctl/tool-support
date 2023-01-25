package uk.ac.ox.ctl.ltiauth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ox.ctl.ltiauth.JWTService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * This controller allows the frontend to get a JWT that can be used to make further requests.
 * Here we also perform some validation based on custom LTI launch values.
 */
@RestController
public class TokenController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JWTService jwtService;

    public TokenController(JWTService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * This is a public endpoint that uses a key to prevent people accessing the wrong session.
     * We use a POST so that we don't end up with the keys being logged, although the actual risk here is low because the tokens are one time use.
     */
    @PostMapping("/token")
    public Object token(@RequestParam String key) {
        Object retrieve = jwtService.retrieve(key);
        // Originally we returned the whole token. This included the encoded JWT but also all the claims.
        if (retrieve instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken)retrieve;
            OAuth2User principal = token.getPrincipal();
            if (principal instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser)principal;
                validate(oidcUser);
                return oidcUser.getIdToken();
            }
            return token;
        }
        // When we sign our own token we just want to return the JWT.
        if (retrieve instanceof String) {
            return Map.of("jwt", retrieve);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token could not be found");
    }

    private void validate(OidcUser oidcUser) {
        // We can't use the LTI roles as they aren't fine grained enough to control who can access the tool.
        Map customClaims = (Map) oidcUser.getClaims().get("https://purl.imsglobal.org/spec/lti/claim/custom");
        if (customClaims != null) {
            Collection<String> allowedRoles = parseRoles(customClaims.get("allowed_roles"));
            if (!allowedRoles.isEmpty()) {
                Collection<String> roles = parseRoles(customClaims.get("canvas_membership_roles"));
                if (roles.isEmpty()) {
                    log.info("Mis-configured tool {}, it's not sending canvas_membership_roles.", oidcUser.getAudience());
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tool is mis-configured and not sending roles.");
                }
                if (Collections.disjoint(allowedRoles, roles)) {
                    log.debug("Didn't find any of your roles ({}) in the allowed roles({})", String.join(":", roles), String.join(":", allowedRoles));
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to use this tool.");
                }
            }
        } else {
            log.debug("Cannot validate user as no custom claims in launch from {}.", oidcUser.getAudience());
        }
    }

    private Collection<String> parseRoles(Object object) {
        if (object instanceof String) {
            String roles = (String) object;
            // We only have a small number of roles so not worth using a Set.
            return Arrays.asList(roles.split(","));
        }
        return Collections.emptySet();
    }

}
