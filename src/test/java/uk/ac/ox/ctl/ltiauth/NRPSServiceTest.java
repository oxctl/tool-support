package uk.ac.ox.ctl.ltiauth;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import uk.ac.ox.ctl.lti13.TokenRetriever;
import uk.ac.ox.ctl.ltiauth.NRPSService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;

// Mocking the WebClient calls isn't good so we use a mock webserver.
@ExtendWith(MockitoExtension.class)
class NRPSServiceTest {

    private MockWebServer mockBackEnd;

    @BeforeEach
    void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        baseUrl = String.format("http://localhost:%s",
                mockBackEnd.getPort());
        nrpsService = new NRPSService(tokenRetriever);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Mock
    private TokenRetriever tokenRetriever;

    @Mock
    private OAuth2AccessToken accessToken;

    private NRPSService nrpsService;
    private String baseUrl;

    @Test
    void testLoad500Error() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(500));
        assertThrows(IllegalStateException.class, () -> nrpsService.loadMembers(accessToken, baseUrl+"/members"));
    }

    @Test
    void testLoad400Error() {
        mockBackEnd.enqueue(new MockResponse().setResponseCode(403));
        assertThrows(IllegalStateException.class, () -> nrpsService.loadMembers(accessToken, baseUrl+"/members"));
    }

    @Test
    void testSimple() throws InterruptedException {
        String json = qt("{'members': ['1', '2', '3']}");
        mockBackEnd.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(json));
        Map<String, Object> response = nrpsService.loadMembers(accessToken, baseUrl + "/members");
        assertNotNull(response);
        List<String> members = (List<String>) response.get("members");
        assertNotNull(members);
        assertThat(members, hasItems("1", "2", "3"));
        {
            RecordedRequest recordedRequest = mockBackEnd.takeRequest();
            assertEquals("/members", recordedRequest.getPath());
        }

    }

    @Test
    void testMultiple() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", qt("<"+ baseUrl+ "/members/2>; rel='next'"))
                .setBody(qt("{'members': ['1', '2', '3']}")));
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", qt("<"+ baseUrl+ "/members/3>; rel='next'"))
                .setBody(qt("{'members': ['4', '5', '6']}")));
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(qt("{'members': ['7', '8', '9']}")));
        Map<String, Object> response = nrpsService.loadMembers(accessToken, baseUrl + "/members/1");
        assertNotNull(response);
        List<String> members = (List<String>) response.get("members");
        assertNotNull(members);
        assertThat(members, hasItems("1", "2", "3", "4", "5", "6", "7", "8", "9"));
        {
            RecordedRequest recordedRequest = mockBackEnd.takeRequest();
            assertEquals("/members/1", recordedRequest.getPath());
        }
        {
            RecordedRequest recordedRequest = mockBackEnd.takeRequest();
            assertEquals("/members/2", recordedRequest.getPath());
        }
        {
            RecordedRequest recordedRequest = mockBackEnd.takeRequest();
            assertEquals("/members/3", recordedRequest.getPath());
        }
    }

    @Test
    void testMultipleFailSecond() throws InterruptedException {
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", qt("<"+ baseUrl+ "/members/2>; rel='next'"))
                .setBody(qt("{'members': ['1', '2', '3']}")));
        mockBackEnd.enqueue(new MockResponse()
                .setResponseCode(500));
        assertThrows(IllegalStateException.class, () -> nrpsService.loadMembers(accessToken, baseUrl+"/members/1"));
        {
            RecordedRequest recordedRequest = mockBackEnd.takeRequest();
            assertEquals("/members/1", recordedRequest.getPath());
        }
        {
            RecordedRequest recordedRequest = mockBackEnd.takeRequest();
            assertEquals("/members/2", recordedRequest.getPath());
        }
    }

    // Just makes it easier to write JSON in Java (removes excessive quoting).
    private String qt(String s) {
        return s.replaceAll("'", "\"");
    }

}