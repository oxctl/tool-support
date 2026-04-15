package uk.ac.ox.ctl.ltiauth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.StateAuthorizationRequestRepository;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiStateCacheAuthorizationRequestRepositoryTest {

    private final MultiStateCacheAuthorizationRequestRepository repository =
            new MultiStateCacheAuthorizationRequestRepository(Duration.ofMinutes(1), 1);

    @Test
    void storesConcurrentRequestsByState() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest("state-1"), new MockHttpServletRequest(), response);
        repository.saveAuthorizationRequest(authorizationRequest("state-2"), new MockHttpServletRequest(), response);

        assertEquals("state-1", repository.loadAuthorizationRequest(callbackRequest("state-1")).getState());
        assertEquals("state-2", repository.loadAuthorizationRequest(callbackRequest("state-2")).getState());
    }

    @Test
    void removesOnlyMatchingState() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest("state-1"), new MockHttpServletRequest(), response);
        repository.saveAuthorizationRequest(authorizationRequest("state-2"), new MockHttpServletRequest(), response);

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(callbackRequest("state-1"), response);

        assertEquals("state-1", removed.getState());
        assertNull(repository.loadAuthorizationRequest(callbackRequest("state-1")));
        assertEquals("state-2", repository.loadAuthorizationRequest(callbackRequest("state-2")).getState());
    }

    @Test
    void storesAtMostThousandRequests() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 1; i <= 1001; i++) {
            repository.saveAuthorizationRequest(authorizationRequest("state-" + i), new MockHttpServletRequest(), response);
        }

        assertTrue(repository.size() < 1001);
        assertNull(repository.loadAuthorizationRequest(callbackRequest("state-1")));
        assertEquals("state-1001", repository.loadAuthorizationRequest(callbackRequest("state-1001")).getState());
    }

    @Test
    void expiresEntriesAfterTtl() throws InterruptedException {
        MultiStateCacheAuthorizationRequestRepository shortLivedRepository =
                new MultiStateCacheAuthorizationRequestRepository(Duration.ofMillis(20), 1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        shortLivedRepository.saveAuthorizationRequest(authorizationRequest("short-lived"), new MockHttpServletRequest(), response);
        Thread.sleep(50);

        assertEquals(0, shortLivedRepository.size());
        assertNull(shortLivedRepository.loadAuthorizationRequest(callbackRequest("short-lived")));
    }

    @Test
    void canLimitIpAddresses() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.setLimitIpAddress(true);

        MockHttpServletRequest initialRequest = new MockHttpServletRequest();
        initialRequest.setRemoteAddr("1.2.3.4");
        repository.saveAuthorizationRequest(authorizationRequest("state-ip", "1.2.3.4"), initialRequest, response);

        MockHttpServletRequest callbackRequest = callbackRequest("state-ip");
        callbackRequest.setRemoteAddr("9.8.7.6");

        assertNull(repository.loadAuthorizationRequest(callbackRequest));
    }

    @Test
    void canAllowIpAddressMismatch() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.setLimitIpAddress(false);

        MockHttpServletRequest initialRequest = new MockHttpServletRequest();
        initialRequest.setRemoteAddr("1.2.3.4");
        repository.saveAuthorizationRequest(authorizationRequest("state-ip", "1.2.3.4"), initialRequest, response);

        MockHttpServletRequest callbackRequest = callbackRequest("state-ip");
        callbackRequest.setRemoteAddr("9.8.7.6");

        assertEquals("state-ip", repository.loadAuthorizationRequest(callbackRequest).getState());
    }

    @Test
    void statelessOptimisticRepositoryNeverReportsWorkingSession() {
        StatelessOptimisticAuthorizationRequestRepository optimisticRepository =
                new StatelessOptimisticAuthorizationRequestRepository(repository);

        assertFalse(optimisticRepository.hasWorkingSession(new MockHttpServletRequest()));
    }

    private MockHttpServletRequest callbackRequest(String state) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(OAuth2ParameterNames.STATE, state);
        request.setRemoteAddr("1.2.3.4");
        return request;
    }

    private OAuth2AuthorizationRequest authorizationRequest(String state) {
        return authorizationRequest(state, null);
    }

    private OAuth2AuthorizationRequest authorizationRequest(String state, String remoteIp) {
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://canvas.example/authorize")
                .clientId("client-id")
                .redirectUri("https://tool.example/lti/login")
                .state(state)
                .authorizationRequestUri("https://canvas.example/authorize?state=" + state);
        if (remoteIp != null) {
            builder.attributes(attributes -> attributes.put(StateAuthorizationRequestRepository.REMOTE_IP, remoteIp));
        }
        return builder.build();
    }
}
