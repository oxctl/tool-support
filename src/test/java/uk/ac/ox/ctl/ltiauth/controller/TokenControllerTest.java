package uk.ac.ox.ctl.ltiauth.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.ltiauth.JWTService;
import uk.ac.ox.ctl.ltiauth.WebSecurityConfig;
import uk.ac.ox.ctl.ltiauth.controller.TokenController;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TokenController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.ac\\.ox\\.ctl\\.canvasproxy\\..*"))
@Import(WebSecurityConfig.class)
class TokenControllerTest {

    OidcIdToken idToken = new OidcIdToken("value", Instant.now(), Instant.now().plus(Duration.ofMinutes(10)), Collections.singletonMap("sub", "1234"));
    @Autowired
    private MockMvc mvc;
    @MockBean
    @Qualifier("lti")
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockBean
    private JWTService jwtService;
    @MockBean
    @Qualifier("lti")
    private JwtDecoder jwtDecoder;
    @Mock
    private OAuth2AuthenticationToken token;
    @Mock
    private OidcUser user;

    @Test
    public void testTokenNotGet() throws Exception {
        mvc.perform(get("/token"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testPostNoToken() throws Exception {
        mvc.perform(post("/token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidToken() throws Exception {
        mvc.perform(post("/token").param("access_token", "not.a.valid.token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testTokenUnknownKey() throws Exception {
        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isNotFound());
        verify(jwtService).retrieve("1234");
    }

    @Test
    public void testTokenGood() throws Exception {
        when(jwtService.retrieve("1234")).thenReturn(token);
        when(token.getPrincipal()).thenReturn(user);
        when(user.getIdToken()).thenReturn(idToken);

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_value").exists());
    }

    @Test
    public void testNoPrincipal() {
        when(jwtService.retrieve("1234")).thenReturn(token);
        when(token.getPrincipal()).thenReturn(null);

        assertThrows(Exception.class, () -> mvc.perform(post("/token").param("key", "1234")));
    }

    @Test
    public void testNoUser() throws Exception {
        when(jwtService.retrieve("1234")).thenReturn(token);
        when(token.getPrincipal()).thenReturn(user);
        when(user.getIdToken()).thenReturn(null);

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isOk());
    }

    @Test
    public void testTokenString() throws Exception {
        when(jwtService.retrieve("1234")).thenReturn("tokenAsString");

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(content().string("{\"jwt\":\"tokenAsString\"}"));
    }

    @Test
    public void testTokenUnknownType() throws Exception {
        when(jwtService.retrieve("1234")).thenReturn(1234L);

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isNotFound());
    }


    @Test
    public void testTokenWrongRole() throws Exception {
        Map<String, String> custom = new HashMap<>();
        custom.put("allowed_roles", "Admin");
        when(jwtService.retrieve("1234")).thenReturn(token);
        when(token.getPrincipal()).thenReturn(user);
        when(user.getIdToken()).thenReturn(idToken);
        when(user.getClaims())
                .thenReturn(Collections.singletonMap("https://purl.imsglobal.org/spec/lti/claim/custom", custom));

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testTokenDisjointRoles() throws Exception {
        Map<String, String> custom = new HashMap<>();
        custom.put("allowed_roles", "Admin");
        custom.put("canvas_membership_roles", "Teacher");

        when(jwtService.retrieve("1234")).thenReturn(token);
        when(token.getPrincipal()).thenReturn(user);
        when(user.getIdToken()).thenReturn(idToken);
        when(user.getClaims())
                .thenReturn(Collections.singletonMap("https://purl.imsglobal.org/spec/lti/claim/custom", custom));

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testTokenNoRoles() throws Exception {
        Map<String, String> custom = new HashMap<>();

        when(jwtService.retrieve("1234")).thenReturn(token);
        when(token.getPrincipal()).thenReturn(user);
        when(user.getIdToken()).thenReturn(idToken);
        when(user.getClaims())
                .thenReturn(Collections.singletonMap("https://purl.imsglobal.org/spec/lti/claim/custom", custom));

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isOk());
    }

    @Test
    public void testTokenRightRole() throws Exception {
        Map<String, String> custom = new HashMap<>();
        custom.put("allowed_roles", "Admin");
        custom.put("canvas_membership_roles", "Admin,Teacher");
        when(jwtService.retrieve("1234")).thenReturn(token);
        when(token.getPrincipal()).thenReturn(user);
        when(user.getIdToken()).thenReturn(idToken);
        when(user.getClaims())
                .thenReturn(Collections.singletonMap("https://purl.imsglobal.org/spec/lti/claim/custom", custom));

        mvc.perform(post("/token").param("key", "1234"))
                .andExpect(status().isOk());
    }
}