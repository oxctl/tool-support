package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import uk.ac.ox.ctl.WebSecurityConfiguration;
import uk.ac.ox.ctl.canvasproxy.jwt.ProxyJwtConfig;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WebMvdTest doesn't pull the OAuth configuration in by default
@WebMvcTest(controllers = RefreshController.class, properties = "tool.origins=https://localhost:3000")
@Import({TestClientRegistrationConfig.class, OAuth2Configuration.class, ProxyWebSecurity.class, ProxyJwtConfig.class, WebSecurityConfiguration.class})
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
class RefreshControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Autowired
    private PrincipalOAuth2AuthorizedClientRepository principalOAuth2AuthorizedClientRepository;

    @TestConfiguration
    public static class Config {
        @Bean
        public PrincipalOAuth2AuthorizedClientRepository principalOAuth2AuthorizedClientRepository() {
            return mock(PrincipalOAuth2AuthorizedClientRepository.class);
        }
    }

    @MockBean
    private ToolRepository toolRepository;

    @BeforeEach
    public void setUp() {
        Mockito.reset(principalOAuth2AuthorizedClientRepository);
    }

    @Test
    public void testNoToken() throws Exception {
        // No token so this shouldn't be allowed.
        mvc.perform(get("/tokens/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testInvalidToken() {
        Exception exception = assertThrows(AuthenticationServiceException.class, () ->
            mvc.perform(get("/tokens/refresh").param("access_token", "not.a.valid.token"))
                    .andExpect(status().isUnauthorized())
        );
        assertNotNull(exception.getMessage());
    }

    @Test
    public void testOAuthUnknown() {
        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder.audience(Collections.singleton("unknown")));
        // We should actually have a custom exception here and a better error message to the user.
        assertThrows(NestedServletException.class, () -> mvc.perform(get("/tokens/refresh").with(jwt)));
    }

    @Test
    public void testOAuthNoToken() throws Exception {
        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder.audience(Collections.singleton("1")));
        // It's possible that we should change this behaviour so that we just return unauthorized in this situation
        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testOAuthWithToken() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("test");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("test"), any(), any())).thenReturn(client);

        when(principalOAuth2AuthorizedClientRepository.renewAccessToken(eq("test"), any(), any(),any())).thenReturn(client);

        when(client.getAccessToken()).thenReturn(token);
        when(token.getScopes()).thenReturn(Set.of("test_scope_1"));

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("1"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );
        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().isOk());
    }

    @Test
    public void testOAuthIncreaseClientScopes() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("twoScopes");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("twoScopes"), any(), any())).thenReturn(client);

        when(client.getAccessToken()).thenReturn(token);

        when(principalOAuth2AuthorizedClientRepository.renewAccessToken(eq("twoScopes"), any(), any(),any())).thenReturn(client);

        when(token.getScopes()).thenReturn(Set.of("test_scope_1"));

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("2"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );

        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOAuthIncreaseTokenScopes() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("test");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("test"), any(), any())).thenReturn(client);

        when(client.getAccessToken()).thenReturn(token);

        when(principalOAuth2AuthorizedClientRepository.renewAccessToken(eq("test"), any(), any(),any())).thenReturn(client);

        when(token.getScopes()).thenReturn(Set.of("test_scope_1", "test_scope_2"));

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("1"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );

        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOAuthNoClientScopes() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("noScopes");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("noScopes"), any(), any())).thenReturn(client);

        when(client.getAccessToken()).thenReturn(token);

        when(principalOAuth2AuthorizedClientRepository.renewAccessToken(eq("noScopes"), any(), any(),any())).thenReturn(client);

        when(token.getScopes()).thenReturn(Set.of("test_scope_1"));

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("3"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );

        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOAuthNoTokenScopes() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("test");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("test"), any(), any())).thenReturn(client);

        when(client.getAccessToken()).thenReturn(token);

        when(principalOAuth2AuthorizedClientRepository.renewAccessToken(eq("test"), any(), any(),any())).thenReturn(client);

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("1"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );

        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOAuthNoScopes() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("noScopes");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("noScopes"), any(), any())).thenReturn(client);

        when(client.getAccessToken()).thenReturn(token);

        when(principalOAuth2AuthorizedClientRepository.renewAccessToken(eq("noScopes"), any(), any(),any())).thenReturn(client);

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("3"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );

        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().isOk());
    }

    @Test
    public void testOAuthScopesMatchDifferentOrder() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("twoScopes");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("twoScopes"), any(), any())).thenReturn(client);

        when(client.getAccessToken()).thenReturn(token);

        when(principalOAuth2AuthorizedClientRepository.renewAccessToken(eq("twoScopes"), any(), any(),any())).thenReturn(client);

        when(token.getScopes()).thenReturn(Set.of("test_scope_2", "test_scope_1"));

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("2"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );

        mvc.perform(get("/tokens/refresh").with(jwt))
                .andExpect(status().isOk());
    }

}