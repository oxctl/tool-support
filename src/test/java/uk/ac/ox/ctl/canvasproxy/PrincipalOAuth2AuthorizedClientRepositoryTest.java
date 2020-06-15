package uk.ac.ox.ctl.canvasproxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.test.context.TestPropertySource;
import uk.ac.ox.ctl.canvasproxy.repository.PrincipalTokensRepository;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.OAuth2AccessTokenRefresher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DataJpaTest
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})

class PrincipalOAuth2AuthorizedClientRepositoryTest {

    @Autowired
    private PrincipalTokensRepository principalTokensRepository;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OAuth2AccessTokenRefresher accessTokenRefresher;

    @MockBean
    private OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> accessTokenClient;

    private PrincipalOAuth2AuthorizedClientRepository clientRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;


    @TestConfiguration
    @EnableConfigurationProperties(OAuth2ClientProperties.class)
    public static class Config {
        @Bean
        InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
            List<ClientRegistration> registrations = new ArrayList<>(
                    OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());
            return new InMemoryClientRegistrationRepository(registrations);
        }

        @Bean
        OAuth2AccessTokenRefresher accessTokenRefresher(OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> accessTokenClient) {
            OAuth2AccessTokenRefresher auth2AccessTokenRefresher = new OAuth2AccessTokenRefresher();
            auth2AccessTokenRefresher.setAccessTokenResponseClient(accessTokenClient);
            return auth2AccessTokenRefresher;
        }
    }

    @BeforeEach
    public void beforeAll() {
        clientRepository = new PrincipalOAuth2AuthorizedClientRepository(principalTokensRepository, clientRegistrationRepository, accessTokenRefresher);
    }

    @Test
    public void testLoadNotFound() {
        Authentication auth = new TestingAuthenticationToken("principal", "credentials");
        OAuth2AuthorizedClient test = clientRepository.loadAuthorizedClient("test", auth, request);
        assertNull(test);
    }

    @Test
    public void testSaveLoadNoRefresh() {
        Authentication auth = new TestingAuthenticationToken("principal", "credentials");
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("test");
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "value", Instant.now(), Instant.now().plus(Duration.ofHours(1)));

        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientRegistration, "principal", accessToken);
        clientRepository.saveAuthorizedClient(client, auth, request, response);

        entityManager.flush();
        entityManager.clear();

        OAuth2AuthorizedClient loaded = clientRepository.loadAuthorizedClient("test", auth, request);
        assertNotNull(loaded);
    }


    @Test
    public void testSaveRefreshNoResponse() {
        Authentication auth = new TestingAuthenticationToken("principal", "credentials");
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("test");
        // An expired token
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "value", Instant.now().minus(Duration.ofHours(2)), Instant.now().minus(Duration.ofHours(1)));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("value", Instant.now());

        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientRegistration, "principal", accessToken, refreshToken);
        clientRepository.saveAuthorizedClient(client, auth, request, response);

        entityManager.flush();
        entityManager.clear();

        assertTrue(clientRepository.needsRenewal(client));

        // We aren't returning a response so it should be null.
        OAuth2AuthorizedClient refreshed = clientRepository.renewAccessToken("test", auth, request, response);
        assertNull(refreshed);
    }

    @Test
    public void testSaveRefreshGoodResponse() {
        Authentication auth = new TestingAuthenticationToken("principal", "credentials");
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("test");
        // An expired token
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "value", Instant.now().minus(Duration.ofHours(2)), Instant.now().minus(Duration.ofHours(1)));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("value", Instant.now());

        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(clientRegistration, "principal", accessToken, refreshToken);

        OAuth2AccessTokenResponse accessTokenResponse = OAuth2AccessTokenResponse
                .withToken("value")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .build();
        Mockito.when(accessTokenClient.getTokenResponse(Mockito.any())).thenReturn(accessTokenResponse);

        clientRepository.saveAuthorizedClient(client, auth, request, response);

        entityManager.flush();
        entityManager.clear();

        assertTrue(clientRepository.needsRenewal(client));

        // We aren't returning a response so it should be null.
        OAuth2AuthorizedClient refreshed = clientRepository.renewAccessToken("test", auth, request, response);
        assertNotNull(refreshed);
    }

}