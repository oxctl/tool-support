package uk.ac.ox.ctl.admin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.LTIAuthorizationGrantType;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.security.user.name=user", "spring.security.user.password=pass1234"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdminControllerIntTest {

    @TestConfiguration
    static class Configuration {
        @Bean
        public TestRestTemplate testRestTemplate() {
            return new TestRestTemplate("user", "pass1234");
        }
    }

    @Autowired
    private TestRestTemplate testRestTemplate;

    private String baseUrl = "http://localhost:";

    @LocalServerPort
    private String localServerPort;

    private Tool createTool() {
        Tool tool = new Tool();
        ToolRegistrationLti lti = new ToolRegistrationLti();
        lti.setClientId(UUID.randomUUID().toString());
        lti.setRegistrationId(UUID.randomUUID().toString());
        lti.setRedirectUri("http://server.test");
        lti.setClientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        lti.setAuthorizationGrantType(LTIAuthorizationGrantType.IMPLICIT);
        tool.setLti(lti);
        ToolRegistrationProxy proxy = new ToolRegistrationProxy();
        proxy.setClientId(UUID.randomUUID().toString());
        proxy.setRegistrationId(UUID.randomUUID().toString());
        proxy.setRedirectUri("http://server.test");
        proxy.setClientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        proxy.setAuthorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        tool.setProxy(proxy);
        return tool;
    }
    
    private HttpEntity<Tool> createRequest(Tool tool) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<Tool>(tool, headers);
    }

    @BeforeAll
    public void createInitialData() throws Exception {
        
        Tool tool = createTool();
        HttpEntity request = createRequest(tool);
        // Make all the IDs fixed
        tool.getLti().setClientId("existing-lti-client-id");
        tool.getLti().setRegistrationId("existing-lti-registration-id");
        tool.getProxy().setClientId("existing-proxy-client-id");
        tool.getProxy().setRegistrationId("existing-proxy-registration-id");
        {
            final ResponseEntity<Tool> toolResponseEntity = testRestTemplate.postForEntity(baseUrl + localServerPort + "/admin/tools", request, Tool.class);
            assertEquals(200, toolResponseEntity.getStatusCode().value());
        }
    }

    @Test
    public void testClashingLtiRegId() throws Exception {
        Tool tool = createTool();
        tool.getLti().setRegistrationId("existing-lti-registration-id");
        HttpEntity request = createRequest(tool);
        final ResponseEntity<Void> toolResponseEntity = testRestTemplate.postForEntity(baseUrl + localServerPort + "/admin/tools", request, Void.class);
        assertEquals(409, toolResponseEntity.getStatusCode().value());
    }

    @Test
    public void testClashingLtiClientId() throws Exception {
        Tool tool = createTool();
        tool.getLti().setClientId("existing-lti-client-id");
        HttpEntity request = createRequest(tool);
        final ResponseEntity<Void> toolResponseEntity = testRestTemplate.postForEntity(baseUrl + localServerPort + "/admin/tools", request, Void.class);
        assertEquals(200, toolResponseEntity.getStatusCode().value());
    }

    @Test
    public void testClashingProxyRegId() throws Exception {
        Tool tool = createTool();
        tool.getProxy().setRegistrationId("existing-proxy-registration-id");
        HttpEntity request = createRequest(tool);
        final ResponseEntity<Void> toolResponseEntity = testRestTemplate.postForEntity(baseUrl + localServerPort + "/admin/tools", request, Void.class);
        assertEquals(409, toolResponseEntity.getStatusCode().value());
    }

    @Test
    public void testClashingProxyClientId() throws Exception {
        Tool tool = createTool();
        tool.getProxy().setClientId("existing-proxy-client-id");
        HttpEntity request = createRequest(tool);
        final ResponseEntity<Void> toolResponseEntity = testRestTemplate.postForEntity(baseUrl + localServerPort + "/admin/tools", request, Void.class);
        assertEquals(200, toolResponseEntity.getStatusCode().value());
    }
}
