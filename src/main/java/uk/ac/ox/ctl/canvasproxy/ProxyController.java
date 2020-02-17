package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This proxy just sends requests on to Canvas. All it does is add the bearer token for the user.
 */
@RestController
public class ProxyController {

    private RestTemplate restTemplate;
    private String token;

    public ProxyController(RestTemplate restTemplate, @Value("${token}") String token) {
        this.restTemplate = restTemplate;
        this.token = token;
    }

    @RequestMapping("/api/**")
    @ResponseBody
    public ResponseEntity<?> proxy(RequestEntity<?> requestEntity) throws URISyntaxException {
        URI remoteService = URI.create("https://oxeval.instructure.com");
        URI requestUrl = requestEntity.getUrl();
        URI thirdPartyApi = new URI(requestUrl.getScheme(), requestUrl.getUserInfo(), remoteService.getHost(), remoteService.getPort(), requestUrl.getPath(), requestUrl.getQuery(), requestUrl.getFragment());
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(requestEntity.getHeaders());
        // If we pass through the wrong host then canvas returns different information.
        headers.remove("Host");
        headers.setBearerAuth(token);
        RequestEntity<?> proxyEntity = new RequestEntity<>(requestEntity.getBody(), new HttpHeaders(headers), requestEntity.getMethod(), thirdPartyApi);
        return restTemplate.exchange(proxyEntity, Object.class);
    }
}
