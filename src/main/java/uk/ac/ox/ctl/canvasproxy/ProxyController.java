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
import org.springframework.web.server.ResponseStatusException;
import uk.ac.ox.ctl.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public ProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RequestMapping("/api/**")
    @ResponseBody
    public ResponseEntity<?> proxy(JwtAuthenticationToken principal, RequestEntity<byte[]> requestEntity, @RegisteredOAuth2AuthorizedClient(registrationId = "canvas", required = false) OAuth2AuthorizedClient client) throws URISyntaxException {
        if (client == null) {
            // This is how we signal to the client that we don't have a OAuth token for them.
            HttpHeaders httpHeaders = new HttpHeaders();
            // Ideally the client should use this URL to get the user to give consent.
            httpHeaders.add("Location", "/tokens/check");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No JWT supplied");
        }
        String canvasApiBaseUrl = (String) ((Map) principal.getTokenAttributes().get("https://purl.imsglobal.org/spec/lti/claim/custom")).get("canvas_api_base_url");
        URI remoteService = URI.create(canvasApiBaseUrl);
        URI requestUrl = requestEntity.getUrl();
        URI localService = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), requestUrl.getHost(), requestUrl.getPort(), null, null, null);
        URI thirdPartyApi = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), remoteService.getHost(), remoteService.getPort(), requestUrl.getPath(), requestUrl.getQuery(), requestUrl.getFragment());
        try {
            ResponseEntity responseEntity = restTemplate.execute(thirdPartyApi, requestEntity.getMethod(), request -> {
                HttpHeaders requestHeaders = request.getHeaders();
                requestHeaders.addAll(requestEntity.getHeaders());
                // If we pass through the wrong host then canvas returns different information.
                requestHeaders.remove("Host");
                requestHeaders.setBearerAuth(client.getAccessToken().getTokenValue());
                if (requestEntity.getBody() != null) {
                    request.getBody().write(requestEntity.getBody());
                }
            }, response -> {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.addAll(response.getHeaders());
                response.getHeaders().getOrEmpty("Link").stream().map(header -> header.replaceAll(remoteService.toString(), localService.toString())).forEach(header -> httpHeaders.set("Link", header));
                // We don't want to pass through cookies from Canvas.
                httpHeaders.remove("Set-Cookie");
                return new ResponseEntity<>(toByteArray(response.getBody()), httpHeaders, response.getStatusCode());
            });
            return responseEntity;
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
