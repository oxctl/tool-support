package uk.ac.ox.ctl.canvasproxy;

import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;
import uk.ac.ox.ctl.canvasproxy.jwt.JwtConfig;
import uk.ac.ox.ctl.canvasproxy.test.security.WithMockClaims;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WebMvdTest doesn't pull the OAuth configuration in by default
@WebMvcTest(controllers = TokenController.class, properties = "proxy.origins=https://localhost:3000")
@ImportAutoConfiguration({OAuth2ClientAutoConfiguration.class, WebSecurity.class, JwtConfig.class })
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
class TokenControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

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
        SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt = SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder.audience(Collections.singleton("unknown")));
        // We should actually have a custom exception here and a better error message to the user.
        assertThrows(NestedServletException.class, () -> mvc.perform(post("/tokens/check").with(jwt)));
    }

    @Test
    public void testOAuthNoToken() throws Exception {
        SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt = SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder.audience(Collections.singleton("1234")));
        mvc.perform(post("/tokens/check").with(jwt))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testOAuthWithToken() throws Exception {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("test");

        when(client.getClientRegistration()).thenReturn(registration);
        when(authorizedClientRepository.loadAuthorizedClient(eq("test"), any(), any())).thenReturn(client);

        SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt = SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(builder -> builder
                        .audience(Collections.singleton("1234"))
                        .claim("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "http://test/")
                );
        mvc.perform(post("/tokens/check").with(jwt))
                .andExpect(status().isOk());
    }


}