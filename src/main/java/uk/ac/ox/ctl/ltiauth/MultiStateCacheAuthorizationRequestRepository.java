package uk.ac.ox.ctl.ltiauth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.StateAuthorizationRequestRepository;

import java.time.Duration;
import java.util.function.BiConsumer;

/**
 * Stores authorization requests in an in-memory cache keyed by OAuth state so launches still work when browsers
 * block cookies. Requests expire quickly and the cache is capped to avoid unbounded growth.
 */
public class MultiStateCacheAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final int MAX_AUTHORIZATION_REQUESTS = 1000;

    private final Cache<String, OAuth2AuthorizationRequest> store;
    private boolean limitIpAddress = true;
    private BiConsumer<String, String> ipMismatchHandler = (a, b) -> {};

    public MultiStateCacheAuthorizationRequestRepository(Duration duration, int concurrencyLevel) {
        store = CacheBuilder.newBuilder()
                .expireAfterAccess(duration)
                .concurrencyLevel(concurrencyLevel)
                .maximumSize(MAX_AUTHORIZATION_REQUESTS)
                .build();
    }

    public void setLimitIpAddress(boolean limitIpAddress) {
        this.limitIpAddress = limitIpAddress;
    }

    public void setIpMismatchHandler(BiConsumer<String, String> ipMismatchHandler) {
        Assert.notNull(ipMismatchHandler, "ipMismatchHandler cannot be null");
        this.ipMismatchHandler = ipMismatchHandler;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) {
            return null;
        }

        OAuth2AuthorizationRequest authorizationRequest = store.getIfPresent(state);
        if (authorizationRequest == null) {
            return null;
        }

        String initialIp = authorizationRequest.getAttribute(StateAuthorizationRequestRepository.REMOTE_IP);
        if (initialIp != null) {
            String requestIp = request.getRemoteAddr();
            if (!initialIp.equals(requestIp)) {
                ipMismatchHandler.accept(initialIp, requestIp);
                if (limitIpAddress) {
                    store.invalidate(state);
                    return null;
                }
            }
        }

        return authorizationRequest;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        String state = authorizationRequest.getState();
        Assert.hasText(state, "authorizationRequest.state cannot be empty");
        store.put(state, authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) {
            return null;
        }

        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            store.invalidate(state);
        }
        return authorizationRequest;
    }

    long size() {
        store.cleanUp();
        return store.size();
    }
}
