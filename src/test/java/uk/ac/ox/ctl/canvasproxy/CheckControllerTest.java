package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.WebSecurityConfiguration;
import uk.ac.ox.ctl.canvasproxy.jwt.ProxyJwtConfig;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.RefreshOAuth2AuthorizedClient;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WebMvdTest doesn't pull the OAuth configuration in by default
@WebMvcTest(controllers = CheckController.class, properties = "tool.origins=https://localhost:3000")
@Import({TestClientRegistrationConfig.class, OAuth2Configuration.class, ProxyWebSecurity.class, ProxyJwtConfig.class, WebSecurityConfiguration.class})
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
class CheckControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @TestConfiguration
    public static class Config {
        @Bean
        public RefreshOAuth2AuthorizedClient refreshOAuth2AuthorizedClient() {
            // Mockbean annotation doesn't prevent autoconfiguration from creating a bean as well.
            return Mockito.mock(RefreshOAuth2AuthorizedClient.class);
        }
    }

    @MockBean
    private ToolRepository toolRepository;

    @Test
    public void testNoToken() throws Exception {
        // No token so this shouldn't be allowed.
        mvc.perform(post("/tokens/check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testInvalidToken() throws Exception {
        mvc.perform(post("/tokens/check").param("access_token", "not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOAuthUnknown() throws Exception {
        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder.audience(Collections.singleton("unknown")));
        mvc.perform(post("/tokens/check").with(jwt))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testOAuthNoToken() throws Exception {
        Tool tool = new Tool();
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setRegistrationId("test");
        tool.setProxy(proxy);
        when(toolRepository.findToolByLtiClientId(anyString())).thenReturn(Optional.of(tool));

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder.audience(Collections.singleton("1")));
        mvc.perform(post("/tokens/check").with(jwt))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testOAuthWithToken() throws Exception {
        Tool tool = new Tool();
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setRegistrationId("test");
        tool.setProxy(proxy);
        when(toolRepository.findToolByLtiClientId(anyString())).thenReturn(Optional.of(tool));

        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("test");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("test"), any(), any())).thenReturn(client);

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("1"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );
        mvc.perform(post("/tokens/check").with(jwt))
                .andExpect(status().is3xxRedirection());
        verify(authorizedClientRepository).removeAuthorizedClient(eq("1"), any(), any(), any());
    }

    @Test
    public void testOAuthReturnNoToken() throws Exception {
        Tool tool = new Tool();
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setRegistrationId("test");
        tool.setProxy(proxy);
        when(toolRepository.findToolByLtiClientId(anyString())).thenReturn(Optional.of(tool));

        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("1"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );
        // The JWT would normally be stored in the session between the first request and this one.
        // We get redirected here to the authorization URL because we don't have an authorized client
        mvc.perform(get("/tokens/check").with(jwt))
                .andExpect(status().is3xxRedirection());
    }
    
    @Test
    public void testOAuthReturn() throws Exception {
        Tool tool = new Tool();
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setRegistrationId("test");
        tool.setProxy(proxy);
        when(toolRepository.findToolByLtiClientId(anyString())).thenReturn(Optional.of(tool));

        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("test");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("test"), any(), any())).thenReturn(client);
        PersistableJwtRequestPostProcessor jwt = new PersistableJwtRequestPostProcessor()
                .jwt(builder -> builder
                        .audience(Collections.singleton("1"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                ); 
        // The JWT would normally be stored in the session between the first request and this one.
        mvc.perform(get("/tokens/check").with(jwt))
                .andExpect(status().isOk());
    }
}