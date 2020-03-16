package uk.ac.ox.ctl.canvasproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * This proxy just sends requests on to Canvas. All it does is add the bearer token for the user.
 * We should switch to use WebClient in the future.
 */
@RestController
public class ProxyController {

    // This is how many minutes before a token expires that we renew it using an access token.
    // We may want to make this configurable in the future
    public static final Duration EAGAR_TOKEN_RENEWAL = Duration.ofMinutes(5);

    private final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final RestTemplate restTemplate;

    private final OAuth2AuthorizedClientRepository clientRepository;
    private OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> accessTokenResponseClient = new DefaultRefreshTokenTokenResponseClient();

    public ProxyController(RestTemplate restTemplate, OAuth2AuthorizedClientRepository clientRepository) {
        this.restTemplate = restTemplate;
        this.clientRepository = clientRepository;
    }

    // If we can control the response when there isn't a token for the current user we may want to make the token required.
    @RequestMapping("/api/**")
    @ResponseBody
    public ResponseEntity<?> proxy(JwtAuthenticationToken principal, RequestEntity<byte[]> requestEntity, @RegisteredOAuth2AuthorizedClient() OAuth2AuthorizedClient client, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws URISyntaxException {
        String canvasApiBaseUrl = (String) ((Map) principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get("canvas_api_base_url");
        URI remoteService = URI.create(canvasApiBaseUrl);
        URI requestUrl = requestEntity.getUrl();
        URI localService = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), requestUrl.getHost(), requestUrl.getPort(), null, null, null);
        URI thirdPartyApi = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), remoteService.getHost(), remoteService.getPort(), requestUrl.getPath(), requestUrl.getQuery(), requestUrl.getFragment());

        if (client.getAccessToken().getExpiresAt().isBefore(Instant.now().minus(EAGAR_TOKEN_RENEWAL))) {
            // Need to refresh token.
            OAuth2RefreshTokenGrantRequest refreshTokenGrantRequest = new OAuth2RefreshTokenGrantRequest(
                    client.getClientRegistration(), client.getAccessToken(),
                    client.getRefreshToken(), Collections.emptySet());
            OAuth2AccessTokenResponse tokenResponse =
                    this.accessTokenResponseClient.getTokenResponse(refreshTokenGrantRequest);
            OAuth2AuthorizedClient oAuth2AuthorizedClient = new OAuth2AuthorizedClient(client.getClientRegistration(),
                    principal.getName(), tokenResponse.getAccessToken(), tokenResponse.getRefreshToken());
            client = oAuth2AuthorizedClient;
            clientRepository.saveAuthorizedClient(oAuth2AuthorizedClient, principal, servletRequest, servletResponse);
        }

        String accessToken = client.getAccessToken().getTokenValue();
        String clientId = client.getClientRegistration().getClientId();

        try {
            return restTemplate.execute(thirdPartyApi, requestEntity.getMethod(), request -> {
                HttpHeaders requestHeaders = request.getHeaders();
                requestHeaders.addAll(requestEntity.getHeaders());
                // If we pass through the wrong host then canvas returns different information.
                requestHeaders.remove("Host");
                requestHeaders.setBearerAuth(accessToken);
                if (requestEntity.getBody() != null) {
                    request.getBody().write(requestEntity.getBody());
                }
            }, response -> {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.addAll(response.getHeaders());
                response.getHeaders().getOrEmpty("Link").stream().map(header -> header.replaceAll(remoteService.toString(), localService.toString())).forEach(header -> httpHeaders.set("Link", header));
                if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                    // Remove the token we have
                    log.info("Removed token for {} as we got an unauthorized response", principal.getName());
                    clientRepository.removeAuthorizedClient(clientId, principal, servletRequest, servletResponse);
                }
                // We don't want to pass through cookies from Canvas.
                httpHeaders.remove("Set-Cookie");
                return new ResponseEntity<>(toByteArray(response.getBody()), httpHeaders, response.getStatusCode());
            });
        } catch (ResourceAccessException e) {
            log.warn("Failed to load {} exception is: {}", thirdPartyApi, e.getMessage());
            // TODO - Need to fix this so we don't log exception as this will be expected in production
            //  We shouldn't fill the logs with this. We may want a metric on failures.
            throw new RuntimeException(e);
        }
    }

    byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
