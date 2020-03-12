package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


// WebMvdTest doesn't pull the OAuth configuration in by default
@WebMvcTest()
@ImportAutoConfiguration({OAuth2ClientAutoConfiguration.class, WebSecurity.class, JwtConfiguration.class })
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
public class ProxyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void testCORS() throws Exception {
        mvc.perform(options("/api")
                .header("Origin", "https://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type")
            ).andExpect(status().isOk());
    }

    @Test
    public void testCORSBadOrigin() throws Exception {
        mvc.perform(options("/api")
                .header("Origin", "https://evil.test:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type")
        ).andExpect(status().is4xxClientError());
    }
}
