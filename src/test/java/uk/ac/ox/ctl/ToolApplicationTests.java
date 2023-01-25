package uk.ac.ox.ctl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
class ToolApplicationTests {

    @Test
    void contextLoads() {
    }

}
