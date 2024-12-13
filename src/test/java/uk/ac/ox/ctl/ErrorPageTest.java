package uk.ac.ox.ctl;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import uk.ac.ox.ctl.ltiauth.Lti13Configuration;
import uk.ac.ox.ctl.ltiauth.LtiWebSecurity;
import uk.ac.ox.ctl.ltiauth.TestClientRegistrationConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
@Import({LocalKeyPairLoadingService.class})

public class ErrorPageTest {

    private LocalHostWebClient localHostWebClient;

    @Autowired
    private Environment environment;

    @BeforeEach
    public void setUp() {
        localHostWebClient = new LocalHostWebClient(environment);
        // The Instructure CSS has loads of errors in it so we should ignore them.
        localHostWebClient.setCssErrorHandler(new SilentCssErrorHandler());
    }

    @Test
    @DisplayName("Check that the error page renders correctly.")
    void errorPageWorks() throws IOException {
        // This is going to be a 404, but we still want to check the contents of the response.
        localHostWebClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        // Check that we generate a good 404 error page.
        Page page = localHostWebClient.getPage("/images/not-found.svg");
        assertEquals(404, page.getWebResponse().getStatusCode());
        assertFalse(page.isHtmlPage());
    }

}
