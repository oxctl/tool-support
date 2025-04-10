package uk.ac.ox.ctl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
class ToolApplicationTests {

    @Test
    void contextLoads() {
    }
    
    @Test
    public void testNoCookies(@Autowired WebTestClient webClient)  {
        // We shouldn't be setting cookies on this URL as it doesn't need or use a session
        webClient
                .get().uri("/admin/tools/")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectCookie().doesNotExist("JSESSIONID");
    }

}
