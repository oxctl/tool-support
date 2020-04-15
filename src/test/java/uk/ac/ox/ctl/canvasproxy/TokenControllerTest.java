package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.canvasproxy.jwt.JwtConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WebMvdTest doesn't pull the OAuth configuration in by default
@WebMvcTest(controllers = TokenController.class, properties = "proxy.origins=https://localhost:3000")
@ImportAutoConfiguration({OAuth2ClientAutoConfiguration.class, WebSecurity.class, JwtConfig.class })
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
class TokenControllerTest {

    @Autowired
    private MockMvc mvc;

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

}