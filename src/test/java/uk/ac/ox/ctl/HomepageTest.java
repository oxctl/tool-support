package uk.ac.ox.ctl;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.htmlunit.LocalHostWebClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import uk.ac.ox.ctl.ltiauth.LocalKeyPairLoadingService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
@Import({LocalKeyPairLoadingService.class})

public class HomepageTest {

    private LocalHostWebClient localHostWebClient;

    @Autowired
    private Environment environment;

    @MockBean
    private SecretsManagerClient secretsManagerClient;

    @BeforeEach
    public void setUp() {
        localHostWebClient = new LocalHostWebClient(environment);
    }

    @Test
    void frontPageWorks() throws IOException {
        // Check that everything starts up and we respond to a request for the homepage (doesn't need authentication)
        HtmlPage page = localHostWebClient.getPage("/");
        assertEquals("Tool Support", page.getTitleText());
    }

}
