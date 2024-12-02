package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@SpringBootTest
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
class CanvasProxyApplicationTests {

    @MockBean
    private SecretsManagerClient secretsManagerClient;

    @Test
    void contextLoads() {
    }

}
