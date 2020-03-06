package uk.ac.ox.ctl.canvasproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * This proxy just sends requests on to Canvas. All it does is add the bearer token for the user.
 */
@RestController
public class ProxyController {

    private final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private RestTemplate restTemplate;
    private String token;

    public ProxyController(RestTemplate restTemplate, @Value("${token}") String token) {
        this.restTemplate = restTemplate;
        this.token = token;
    }

    @RequestMapping("/api/**")
    @ResponseBody
    public ResponseEntity<?> proxy(JwtAuthenticationToken principal, RequestEntity<?> requestEntity, @RegisteredOAuth2AuthorizedClient(registrationId = "canvas", required = false) OAuth2AuthorizedClient client) throws URISyntaxException {
        log.info("It's {}", principal.getName());
        if (client == null) {
            // This is how we signal to the client that we don't have a OAuth token for them.
            HttpHeaders httpHeaders = new HttpHeaders();
            // Ideally the client should use this URL to get the user to give consent.
            httpHeaders.add("Location", "/tokens/check");
            return new ResponseEntity<>(null, httpHeaders, HttpStatus.FORBIDDEN);
        }
        String canvasApiBaseUrl = (String)((Map) principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get("canvas_api_base_url");
        URI remoteService = URI.create(canvasApiBaseUrl);
        URI requestUrl = requestEntity.getUrl();
        URI localService = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), requestUrl.getHost(), requestUrl.getPort(), null, null, null);
        URI thirdPartyApi = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), remoteService.getHost(), remoteService.getPort(), requestUrl.getPath(), requestUrl.getQuery(), requestUrl.getFragment());
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(requestEntity.getHeaders());
        // If we pass through the wrong host then canvas returns different information.
        headers.remove("Host");
        headers.setBearerAuth(client.getAccessToken().getTokenValue());
        RequestEntity<?> proxyEntity = new RequestEntity<>(requestEntity.getBody(), new HttpHeaders(headers), requestEntity.getMethod(), thirdPartyApi);
        try {
            ResponseEntity<Object> exchange = restTemplate.exchange(proxyEntity, Object.class);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.addAll(exchange.getHeaders());
            exchange.getHeaders().getOrEmpty("Link").stream().map(header -> header.replaceAll(remoteService.toString(), localService.toString())).forEach(header -> httpHeaders.set("Link", header));
            // We don't want to pass through cookies from Canvas.
            httpHeaders.remove("Set-Cookie");

            return new ResponseEntity<>(exchange.getBody(), httpHeaders, exchange.getStatusCode());
        } catch (ResourceAccessException e) {
            log.warn("Failed to load {} exception is: {}", thirdPartyApi, e.getMessage());
            // TODO - Need to fix this so we don't log exception as this will be expected in production
            //  We shouldn't fill the logs with this. We may want a metric on failures.
            throw new RuntimeException(e);
        }
    }
}
