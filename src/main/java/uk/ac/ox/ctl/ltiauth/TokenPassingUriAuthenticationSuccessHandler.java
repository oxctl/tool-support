package uk.ac.ox.ctl.ltiauth;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OptimisticAuthorizationRequestRepository;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.StateCheckingAuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.ac.ox.ctl.ltiauth.pipelines.LtiLaunchEventService;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * This handler takes the URL and adds a token onto it. This is so that a static HTML frontend can use the token
 * to retrieve the full JWT.
 */
public class TokenPassingUriAuthenticationSuccessHandler extends StateCheckingAuthenticationSuccessHandler {
    
    private final Logger log = LoggerFactory.getLogger(TokenPassingUriAuthenticationSuccessHandler.class);

    private final JWTService jwtService;
    private final LtiLaunchEventService ltiLaunchEventService;

    public TokenPassingUriAuthenticationSuccessHandler(OptimisticAuthorizationRequestRepository authorizationRequestRepository, JWTService jwtService, LtiLaunchEventService ltiLaunchEventService) {
        super(authorizationRequestRepository);
        this.jwtService = jwtService;
        this.ltiLaunchEventService = ltiLaunchEventService;
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            // Because we are just giving the token out to this URL we need to trust that this URL can't be messed with.
            String targetLink = token.getPrincipal().getAttribute("https://purl.imsglobal.org/spec/lti/claim/target_link_uri");
            if (targetLink != null && !targetLink.isEmpty()) {
                String origin = getUrl(request);
                ltiLaunchEventService.publishLaunchEvent(token, targetLink, origin);

                Object obj = jwtService.createJWT(token, origin);
                String key = jwtService.store(obj);
                
                String tokenParam = "token="+key;
                // We include the server in the parameters so that the client can use this
                // to request the token, this means the client doesn't have to know the Tool Support server used
                // at build time.
                String serverParam = "server="+ URLEncoder.encode(origin, StandardCharsets.UTF_8);
                try {
                    URI uri = URI.create(targetLink);
                    String query = uri.getQuery();
                    if (query != null) {
                        query += "&"+tokenParam;
                    } else {
                        query = tokenParam;
                    }
                    query += "&"+serverParam;
                    uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
                    return uri.toString();
                } catch (URISyntaxException | IllegalArgumentException e) {
                    log.warn("Unable to parse {}", targetLink);
                } 
                // Fallback to assuming we can just append the token as a string
                return targetLink + "?"+tokenParam;
            }
        }

        return super.determineTargetUrl(request, response, authentication);
    }

    /**
     * Gets the URL origin from the request.
     * @param request The request
     * @return The URL without anything after the port.
     */
    public String getUrl(HttpServletRequest request) {

        String scheme = request.getScheme();             // http / https
        String serverName = request.getServerName();     // hostname.com
        int serverPort = request.getServerPort();        // 80

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://");
        url.append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }
        return url.toString();
    }
}
