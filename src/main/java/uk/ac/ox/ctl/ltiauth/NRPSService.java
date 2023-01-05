package uk.ac.ox.ctl.ltiauth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.ac.ox.ctl.lti13.TokenRetriever;
import uk.ac.ox.ctl.lti13.nrps.LtiScopes;
import uk.ac.ox.ctl.ltiauth.controller.LinksParser;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static uk.ac.ox.ctl.ltiauth.controller.LinksParser.Relation.NEXT;

/**
 * A small service that caches OAuth2 tokens and then allows them to be used to retrieve a list of members.
 */
@Service
public class NRPSService {

    private final Logger log = LoggerFactory.getLogger(NRPSService.class);

    private final TokenRetriever tokenRetriever;

    private final LinksParser linksParser = new LinksParser();
    // We cache things longer than token expiry so that we are always testing renewal detection.
    // This cache isn't expected to grow very large as we will have one token per client ID.
    // Cache of Client ID -> OAuth2AccessToken
    private final Cache<String, OAuth2AccessToken> tokenCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(3))
            .build();

    /**
     * Maximum duration of loading the members.
     */
    @Value("${lti.nrps.timeout:60s}")
    private Duration timeout = Duration.ofSeconds(100);

    /**
     * How long before the access token expires should we renew it, we don't want to leave it too long or a request
     * may fail as the token becomes expired.
     */
    @Value("${lti.nrps.renewal:PT5M}")
    private Duration earlyRenewal;

    public NRPSService(TokenRetriever tokenRetriever) {
        this.tokenRetriever = tokenRetriever;
    }

    /**
     * Simple method that fails if it's not a 2xx code response.
     */
    private static ClientResponse throwNonSuccess(ClientResponse clientResponse) {
        // exchange() doesn't check for good/bad responses
        if (!clientResponse.statusCode().is2xxSuccessful()) {
            // TODO We should have  better error handling here.
            throw new IllegalStateException("Bad status from remote: " + clientResponse.statusCode());
        }
        return clientResponse;
    }

    public OAuth2AccessToken getToken(ClientRegistration clientRegistration) {
        try {
            // This is not thread safe, however it appears you can have multiple access tokens so it doesn't matter.
            OAuth2AccessToken accessToken = tokenCache.get(clientRegistration.getClientId(), () -> {
                log.debug("No token found for {} so retrieving.", clientRegistration.getClientId());
                OAuth2AccessTokenResponse newToken = tokenRetriever.getToken(clientRegistration, LtiScopes.LTI_NRPS_SCOPE);
                return newToken.getAccessToken();
            });
            if (accessToken.getExpiresAt().isBefore(Instant.now().plus(earlyRenewal))) {
                log.debug("Token for {} is expired so renewing.", clientRegistration.getClientId());
                // Expired, so renew.
                tokenCache.invalidate(clientRegistration.getClientId());
                accessToken = tokenCache.get(clientRegistration.getClientId(), () -> {
                    OAuth2AccessTokenResponse newToken = tokenRetriever.getToken(clientRegistration, LtiScopes.LTI_NRPS_SCOPE);
                    return newToken.getAccessToken();
                });
            }
            return accessToken;
        } catch (RestClientException | ExecutionException e) {
            log.warn("Failed to get token for " + clientRegistration.getClientId(), e);
        }
        return null;

    }

    public Map<String, Object> loadMembers(OAuth2AccessToken accessToken, String url) {
        WebClient webClient = WebClient.builder()
                .defaultHeader("Accept", "application/vnd.ims.lti-nrps.v2.membershipcontainer+json")
                .defaultHeader("Authorization", "Bearer " + accessToken.getTokenValue())
                .build();
        Mono<Map<String, Object>> membersGet = webClient.get()
                .uri(url)//Used one to test
                .exchange()
                .map(NRPSService::throwNonSuccess)
                .expand(clientResponse -> {
                    List<String> links = clientResponse.headers().asHttpHeaders().getValuesAsList("Link");
                    return links.stream()
                            .map(linksParser::parseLink)
                            .filter(Objects::nonNull)
                            .filter(link -> NEXT.equals(link.getRelation()))
                            .findFirst()
                            .map(link -> webClient.get().uri(link.getUrl()).exchange().map(NRPSService::throwNonSuccess))
                            .orElse(Mono.empty());
                })
                .flatMap(clientResponse -> clientResponse.bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
                }))
                .reduce((accumulator, map) -> {
                    // These won't be "correct", as we are merbing multiple requests but are good enough.
                    accumulator.putIfAbsent("id", map.get("id"));
                    accumulator.putIfAbsent("context", map.get("context"));
                    accumulator.compute("members", (key, value) -> {
                        if (value == null) {
                            return map.get("members");
                        } else {
                            ((List) value).addAll((Collection) map.get("members"));
                        }
                        return value;
                    });
                    return accumulator;
                });

        return membersGet.block(timeout);
    }


}
