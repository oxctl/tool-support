package uk.ac.ox.ctl.ltiauth.pipelines;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LtiLaunchEventServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void publishesLaunchEventWhenEndpointConfigured() {
        CloudflarePipelinesProperties properties = new CloudflarePipelinesProperties(URI.create("https://pipeline.example/events"), "api-token", 10);
        Clock clock = Clock.fixed(Instant.parse("2024-01-02T03:04:05Z"), ZoneOffset.UTC);
        LtiLaunchEventService service = new LtiLaunchEventService(restTemplate, properties, clock, synchronousExecutor());
        OidcAuthenticationToken token = token();

        server.expect(requestTo(properties.getLaunchEndpoint().get()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer api-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.event_type").value("lti_launch"))
                .andExpect(jsonPath("$.timestamp").value(1704164645000L))
                .andExpect(jsonPath("$.target_link_uri").value("https://target.example/launch"))
                .andExpect(jsonPath("$.tool_support_url").value("https://tool.support"))
                .andExpect(jsonPath("$.registration_id").value("reg-id"))
                .andExpect(jsonPath("$.claims.iss").value("issuer"))
                .andExpect(jsonPath("$.claims.sub").value("sub"))
                .andExpect(jsonPath("$.claims['https://purl.imsglobal.org/spec/lti/claim/deployment_id']").value("deploy-1"))
                .andExpect(jsonPath("$.claims['https://purl.imsglobal.org/spec/lti/claim/context'].id").value("course-1"))
                .andExpect(jsonPath("$.claims['https://purl.imsglobal.org/spec/lti/claim/resource_link'].id").value("resource-1"))
                .andExpect(jsonPath("$.claims['https://purl.imsglobal.org/spec/lti/claim/roles'][0]").value("Instructor"))
                .andExpect(jsonPath("$.claims['https://purl.imsglobal.org/spec/lti/claim/target_link_uri']").value("https://target.example/launch"))
                .andRespond(withSuccess());

        service.publishLaunchEvent(token, "https://target.example/launch", "https://tool.support");
        server.verify();
    }

    @Test
    void skipsWhenNoEndpointConfigured() {
        CloudflarePipelinesProperties properties = new CloudflarePipelinesProperties(null, null, 10);
        LtiLaunchEventService service = new LtiLaunchEventService(restTemplate, properties, Clock.systemUTC(), synchronousExecutor());

        service.publishLaunchEvent(token(), "https://target.example/launch", "https://tool.support");
        server.verify();
    }

    @Test
    void skipsWhenNoAuthTokenConfigured() {
        CloudflarePipelinesProperties properties = new CloudflarePipelinesProperties(URI.create("https://pipeline.example/events"), null, 10);
        LtiLaunchEventService service = new LtiLaunchEventService(restTemplate, properties, Clock.systemUTC(), synchronousExecutor());

        service.publishLaunchEvent(token(), "https://target.example/launch", "https://tool.support");
        server.verify();
    }

    @Test
    void logsWhenForbidden() {
        CloudflarePipelinesProperties properties = new CloudflarePipelinesProperties(URI.create("https://pipeline.example/events"), "api-token", 10);
        LtiLaunchEventService service = new LtiLaunchEventService(restTemplate, properties, Clock.systemUTC(), synchronousExecutor());
        server.expect(requestTo(properties.getLaunchEndpoint().get()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        service.publishLaunchEvent(token(), "https://target.example/launch", "https://tool.support");
        server.verify();
    }

    private Executor synchronousExecutor() {
        return Runnable::run;
    }

    private OidcAuthenticationToken token() {
        Map<String, Object> claims = Map.ofEntries(
                Map.entry("sub", "sub"),
                Map.entry("iss", "issuer"),
                Map.entry("aud", List.of("client-id")),
                Map.entry("email", "user@example.com"),
                Map.entry("name", "Example User"),
                Map.entry("given_name", "Example"),
                Map.entry("family_name", "User"),
                Map.entry("https://purl.imsglobal.org/spec/lti/claim/deployment_id", "deploy-1"),
                Map.entry("https://purl.imsglobal.org/spec/lti/claim/context", Map.of("id", "course-1", "title", "Course 1")),
                Map.entry("https://purl.imsglobal.org/spec/lti/claim/resource_link", Map.of("id", "resource-1")),
                Map.entry("https://purl.imsglobal.org/spec/lti/claim/roles", List.of("Instructor")),
                Map.entry("https://purl.imsglobal.org/spec/lti/claim/target_link_uri", "https://target.example/launch")
        );
        OidcIdToken token = new OidcIdToken("value", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS), claims);
        OAuth2User user = new DefaultOidcUser(Set.of(), token);
        return new OidcAuthenticationToken(user, Set.of(), "reg-id", "state");
    }
}
