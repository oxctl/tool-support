package uk.ac.ox.ctl.ltiauth;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.OptimisticAuthorizationRequestRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenPassingUriAuthenticationSuccessHandlerTest {

    @Mock
    private OptimisticAuthorizationRequestRepository optimisticRepository;

    @Mock
    private JWTService jwtService;
    private TokenPassingUriAuthenticationSuccessHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new TokenPassingUriAuthenticationSuccessHandler(optimisticRepository, jwtService);
        when(jwtService.store(any())).thenReturn("value");
    }


    @Test
    public void good() {
        HttpServletRequest request = mockHttpServletRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        OidcAuthenticationToken authentication = tokenOf("http://server.test");

        assertEquals("http://server.test?token=value&server=http%253A%252F%252Flti.server.test", handler.determineTargetUrl(request, response, authentication));
    }


    @Test
    public void withPort() {
        HttpServletRequest request = mockHttpServletRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        OidcAuthenticationToken authentication = tokenOf("http://server.test:3000/");

        assertEquals("http://server.test:3000/?token=value&server=http%253A%252F%252Flti.server.test", handler.determineTargetUrl(request, response, authentication));
    }

    @Test
    public void existingParameters() {
        HttpServletRequest request = mockHttpServletRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        OidcAuthenticationToken authentication = tokenOf("http://server.test/file.html?param=true");

        assertEquals("http://server.test/file.html?param=true&token=value&server=http%253A%252F%252Flti.server.test", handler.determineTargetUrl(request, response, authentication));
    }


    @Test
    public void notUrl() {
        HttpServletRequest request = mockHttpServletRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        OidcAuthenticationToken authentication = tokenOf("Not a URL");

        assertEquals("Not a URL?token=value", handler.determineTargetUrl(request, response, authentication));
    }

    @Test
    public void relative() {
        HttpServletRequest request = mockHttpServletRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        OidcAuthenticationToken authentication = tokenOf("/path");

        assertEquals("/path?token=value&server=http%253A%252F%252Flti.server.test", handler.determineTargetUrl(request, response, authentication));
    }

    @Test
    public void withPath() {
        HttpServletRequest request = mockHttpServletRequest();
        HttpServletResponse response = mock(HttpServletResponse.class);
        OidcAuthenticationToken authentication = tokenOf("http://server.test/path");

        assertEquals("http://server.test/path?token=value&server=http%253A%252F%252Flti.server.test", handler.determineTargetUrl(request, response, authentication));
    }

    @Test
    public void withPortOnRequest() {
        MockHttpServletRequest request = mockHttpServletRequest();
        // Check that we include port when it's non-standard.
        request.setServerPort(8080);
        HttpServletResponse response = mock(HttpServletResponse.class);
        OidcAuthenticationToken authentication = tokenOf("http://server.test/path");

        assertEquals("http://server.test/path?token=value&server=http%253A%252F%252Flti.server.test%253A8080", handler.determineTargetUrl(request, response, authentication));
    }


    @NotNull
    private OidcAuthenticationToken tokenOf(String url) {
        OidcIdToken token = new OidcIdToken("value", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), Map.of(
                "sub", "1234",
                "https://purl.imsglobal.org/spec/lti/claim/target_link_uri", url
        ));
        OAuth2User user = new DefaultOidcUser(Set.of(), token);
        OidcAuthenticationToken authentication = new OidcAuthenticationToken(user, Set.of(), "reg-id", "state");
        return authentication;
    }

    private MockHttpServletRequest mockHttpServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("lti.server.test");
        request.setServerPort(443);
        request.setServletPath("/login/lti");
        return request;
    } 

}