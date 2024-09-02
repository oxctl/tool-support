package uk.ac.ox.ctl.ltiauth.controller;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import uk.ac.ox.ctl.lti13.lti.Claims;
import uk.ac.ox.ctl.lti13.lti.Role;
import uk.ac.ox.ctl.lti13.nrps.LtiScopes;
import uk.ac.ox.ctl.ltiauth.AllowedRolesService;
import uk.ac.ox.ctl.ltiauth.Lti13Configuration;
import uk.ac.ox.ctl.ltiauth.NRPSService;
import uk.ac.ox.ctl.ltiauth.TestClientRegistrationConfig;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NRPSController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.ac\\.ox\\.ctl\\.canvasproxy\\..*"))
@ImportAutoConfiguration(exclude = OAuth2ClientAutoConfiguration.class)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
@Import({Lti13Configuration.class, TestClientRegistrationConfig.class})
class NRPSControllerTest {

    @MockBean
    private NRPSService nrpsService;
    
    @MockBean
    private AllowedRolesService allowedRolesService;

    @Autowired
    private MockMvc mvc;

    @Test
    void testDenied() throws Exception {
        mvc.perform(get("/nrps/test")).andExpect(status().isUnauthorized());
    }

    @Test
    void testBadJwt() throws Exception {
        // Just a random JWT
        String sampleJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0Ijo" +
                "xNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        mvc.perform(get("/nrps/test").header("Authorization", "Bearer " + sampleJwt)).andExpect(status().isUnauthorized());
    }

    @Test
    void testNRPSClaim() throws Exception {
        JwtRequestPostProcessor jwt = jwt()
                .jwt(builder -> builder.audience(Collections.singleton("unknown")));
        mvc.perform(get("/nrps/test").with(jwt)).andExpect(status().isBadRequest());
    }

    @Test
    void testNoClientConfig() {
        Map nrps = Collections.singletonMap("context_memberships_url", "http://example.com");
        JwtRequestPostProcessor jwt = jwt().jwt(builder ->
                builder.audience(Collections.singleton("does-not-exist"))
                        .claim(LtiScopes.LTI_NRPS_CLAIM, nrps)
        );
        // This should get mapped by the exception mapper in production
        assertThrows(ServletException.class, () ->
                mvc.perform(get("/nrps/test").with(jwt))
        );
    }

    @Test
    void testNoRoles() throws Exception {
        Map nrps = Collections.singletonMap("context_memberships_url", "http://example.com");
        JwtRequestPostProcessor jwt = jwt().jwt(builder ->
                builder.audience(Collections.singleton("1234"))
                        .claim(LtiScopes.LTI_NRPS_CLAIM, nrps)
        );
        mvc.perform(get("/nrps/test").with(jwt)).andExpect(status().isBadRequest());
    }

    @Test
    void testBadRole() throws Exception {
        Map nrps = Collections.singletonMap("context_memberships_url", "http://example.com");
        List<String> roles = Collections.singletonList(Role.System.NONE);
        JwtRequestPostProcessor jwt = jwt().jwt(builder ->
                builder.audience(Collections.singleton("1234"))
                        .claim(LtiScopes.LTI_NRPS_CLAIM, nrps)
                        .claim(Claims.ROLES, roles)
        );
        mvc.perform(get("/nrps/test").with(jwt)).andExpect(status().isForbidden());
    }

    @Test
    void testTokenRetrialFailed() throws Exception {
        Map nrps = Collections.singletonMap("context_memberships_url", "http://example.com");
        List<String> roles = Collections.singletonList(Role.Context.INSTRUCTOR);
        JwtRequestPostProcessor jwt = jwt().jwt(builder ->
                builder.audience(Collections.singleton("1234"))
                        .claim(LtiScopes.LTI_NRPS_CLAIM, nrps)
                        .claim(Claims.ROLES, roles)
        );
        Mockito.when(nrpsService.getToken(any())).thenThrow(RestClientException.class);
        mvc.perform(get("/nrps/test").with(jwt)).andExpect(status().isForbidden());
    }
}