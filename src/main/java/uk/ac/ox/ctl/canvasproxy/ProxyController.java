package uk.ac.ox.ctl.canvasproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.annotation.RegisteredOAuth2AccessToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;

/**
 * This proxy just sends requests on to Canvas. All it does is add the bearer token for the user.
 * We should switch to use WebClient in the future.
 */
@RestController
public class ProxyController {

    public static final String CANVAS_API_BASE_URL = "canvas_api_base_url";

    private final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final RestTemplate restTemplate;

    public ProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // If we can control the response when there isn't a token for the current user we may want to make the token required.
    @RequestMapping("/api/**")
    @ResponseBody
    public ResponseEntity<?> proxy(JwtAuthenticationToken principal, RequestEntity<byte[]> requestEntity, @RegisteredOAuth2AccessToken() OAuth2AccessToken accessToken) throws URISyntaxException {
        String canvasApiBaseUrl = (String) ((Map) principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get(CANVAS_API_BASE_URL);
        if (canvasApiBaseUrl == null || canvasApiBaseUrl.isEmpty()) {
            // The message doesn't make it into he HTTP status, but is in the JSON
            // https://bz.apache.org/bugzilla/show_bug.cgi?id=60362
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CANVAS_API_BASE_URL + " was not in LTI custom claims. Please update LTI configuration.");
        }
        URI remoteService = URI.create(canvasApiBaseUrl);
        URI requestUrl = requestEntity.getUrl();
        URI localService = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), requestUrl.getHost(), requestUrl.getPort(), null, null, null);
        URI thirdPartyApi = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), remoteService.getHost(), remoteService.getPort(), requestUrl.getPath(), requestUrl.getQuery(), requestUrl.getFragment());

        try {
            return restTemplate.execute(thirdPartyApi, requestEntity.getMethod(), request -> {
                HttpHeaders requestHeaders = request.getHeaders();
                requestHeaders.addAll(requestEntity.getHeaders());
                // If we pass through the wrong host then canvas returns different information.
                requestHeaders.remove("Host");
                requestHeaders.setBearerAuth(accessToken.getTokenValue());
                // Client browsers will say they accept brotli encoding, but we can't proxy that so we only allow gzip and deflate.
                // Really we shouldn't be trying to de-compress the response and should just pass it straight through.
                // This is a quick fix.
                requestHeaders.set("Accept-Encoding", "gzip");
                if (requestEntity.getBody() != null) {
                    request.getBody().write(requestEntity.getBody());
                }
            }, response -> {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.addAll(response.getHeaders());
                response.getHeaders().getOrEmpty("Link").stream().map(header -> header.replaceAll(remoteService.toString(), localService.toString())).forEach(header -> httpHeaders.set("Link", header));
                // We used to check the response status and if it was unauthorized assume that the token was no longer valid.
                // However Canvas returns unauthorized when a permission check has failed (and it should really be forbidden).

                // We don't want to pass through cookies from Canvas.
                httpHeaders.remove("Set-Cookie");
                return new ResponseEntity<>(toByteArray(response.getBody()), httpHeaders, response.getStatusCode());
            });
        } catch (ResourceAccessException e) {
            // When we get a timeout we should translate it to the correct HTTP message.
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT);
            }
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
