package uk.ac.ox.ctl.ltiauth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.ltiauth.LocalKeyPairLoadingService;
import uk.ac.ox.ctl.ltiauth.Lti13Configuration;
import uk.ac.ox.ctl.ltiauth.LtiWebSecurity;
import uk.ac.ox.ctl.ltiauth.TestClientRegistrationConfig;
import uk.ac.ox.ctl.ltiauth.pipelines.LtiLaunchEventConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JWKSController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.ac\\.ox\\.ctl\\.canvasproxy\\..*"))
@ImportAutoConfiguration(exclude = OAuth2ClientAutoConfiguration.class)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
@Import({Lti13Configuration.class, TestClientRegistrationConfig.class, LtiWebSecurity.class, LocalKeyPairLoadingService.class, LtiLaunchEventConfiguration.class})
class JWKSControllerTest {

    @MockBean
    @Qualifier("lti")
    private JwtDecoder jwtDecoder;
    
    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mvc;

    @Test
    public void testAllowed() throws Exception {
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys").isNotEmpty())
                .andExpect(header().string("cache-control", "max-age=86400, public"));
    }

}