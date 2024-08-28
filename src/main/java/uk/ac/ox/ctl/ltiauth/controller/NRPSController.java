package uk.ac.ox.ctl.ltiauth.controller;

import net.minidev.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ox.ctl.lti13.lti.Claims;
import uk.ac.ox.ctl.lti13.nrps.LtiScopes;
import uk.ac.ox.ctl.ltiauth.AllowedRolesService;
import uk.ac.ox.ctl.ltiauth.ClientRegistrationService;
import uk.ac.ox.ctl.ltiauth.NRPSService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This proxy just sends requests on to Canvas for the NRPS. This should be protected as a OAuth2 resources
 * server endpoint.
 */
@RestController
public class NRPSController {

    private final ClientRegistrationService clientRegistrationService;

    private final NRPSService nrpsService;

    private final AllowedRolesService allowedRolesService;

    public NRPSController(ClientRegistrationService clientRegistrationService, NRPSService nrpsService, AllowedRolesService allowedRolesService) {
        this.clientRegistrationService = clientRegistrationService;
        this.nrpsService = nrpsService;
        this.allowedRolesService = allowedRolesService;
    }

    // If we can control the response when there isn't a token for the current user we may want to make the token required.
    // the one bit of data we can trust when using server-signed tokens is the audience
    @GetMapping("/nrps/**")
    public Map<String, Object> proxy(JwtAuthenticationToken token) {
        Object principal = token.getPrincipal();
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            Object o = jwt.getClaims().get(LtiScopes.LTI_NRPS_CLAIM);
            if (o instanceof JSONObject) {
                JSONObject json = (JSONObject) o;
                String contextMembershipsUrl = json.getAsString("context_memberships_url");
                if (contextMembershipsUrl != null && !contextMembershipsUrl.isEmpty()) {
                    // Got a URL to go to.
                    Object r = jwt.getClaims().get(Claims.RESOURCE_LINK);
                    String resourceLinkId = null;
                    if (r instanceof JSONObject) {
                        JSONObject resourceJson = (JSONObject) r;
                        resourceLinkId = resourceJson.getAsString("id");
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
                        throw new IllegalStateException("Failed find client registration for: " + String.join(", ", jwt.getAudience()));
                    }

                    // This validates that the user should be allowed to use the tool
                    Object roleClaim = jwt.getClaims().get(Claims.ROLES);

                    // if subject and audience are the same it is self-signed so don't look at roles
                    if (!jwt.getAudience().contains(jwt.getSubject())) {
                        if (roleClaim instanceof List) {
                            if (!allowedRolesService.isNRPSAllowed(clientRegistration.getClientId(), (List<String>) roleClaim)) {
                                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to use this tool.");
                            }
                        } else {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No roles in JWT");
                        }
                    }

                    // The retrieval is cached.
                    OAuth2AccessToken ltiToken = nrpsService.getToken(clientRegistration);
                    if (ltiToken == null) {
                        throw new IllegalStateException("Failed to get NRPS token for: " + clientRegistration.getClientId());
                    }

                    String url = contextMembershipsUrl;
                    if (resourceLinkId != null) {
                        url = url + "?rlid=" + URLEncoder.encode(resourceLinkId, StandardCharsets.UTF_8);
                        url = url + "&per_page=100";
                    }
                    return nrpsService.loadMembers(ltiToken, url);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No resource link claim in JWT");
                }

            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No NRPS claim in JWT");
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not find required data");
    }

}
