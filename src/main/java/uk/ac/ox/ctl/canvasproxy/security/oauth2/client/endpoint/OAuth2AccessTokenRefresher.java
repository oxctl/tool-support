package uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

/**
 * This class just handles the refreshing of a OAuth2AuthorizedClient.
 */
@Service
@Primary
public class OAuth2AccessTokenRefresher {

    private final Logger log = LoggerFactory.getLogger(OAuth2AccessTokenRefresher.class);

    // This is how many minutes before a token expires that we renew it using an access token.
    // We may want to make this configurable in the future
    @Value("${proxy.eager-token-renewal:PT5M}")
    private Duration eagerTokenRenewal;

    private OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> accessTokenResponseClient = new DefaultRefreshTokenTokenResponseClient();

    public void setEagerTokenRenewal(Duration eagerTokenRenewal) {
        this.eagerTokenRenewal = eagerTokenRenewal;
    }

    public void setAccessTokenResponseClient(OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> accessTokenResponseClient) {
        this.accessTokenResponseClient = accessTokenResponseClient;
    }

    public OAuth2AccessTokenResponse refresh(OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient.getRefreshToken() == null) {
            throw new IllegalArgumentException("Client supplied doesn't have a refresh token to use.");
        }
        OAuth2RefreshTokenGrantRequest refreshTokenGrantRequest = new OAuth2RefreshTokenGrantRequest(
                authorizedClient.getClientRegistration(), authorizedClient.getAccessToken(),
                authorizedClient.getRefreshToken(), Collections.emptySet());
        try {
            OAuth2AccessTokenResponse tokenResponse =
                    this.accessTokenResponseClient.getTokenResponse(refreshTokenGrantRequest);
            log.debug("Successfully refreshed token on {} for {}",
                    authorizedClient.getClientRegistration().getClientId(), authorizedClient.getPrincipalName());
            return tokenResponse;
        } catch (OAuth2AuthorizationException e) {
            // This should fall through.
            log.debug("Failed to refresh token for on {} for {}",
                authorizedClient.getClientRegistration().getClientId(), authorizedClient.getPrincipalName());
        }
        return null;
    }

    /**
     * Check if the supplied refresh access token is about to expire.
     * @param authorizedClient The authorized client to check.
     * @return true if the token should be refreshed.
     */
    public boolean needsRefresh(OAuth2AuthorizedClient authorizedClient) {
        if (authorizedClient.getAccessToken().getExpiresAt() == null) {
            log.warn("No expiry time on {} token for {} will never be detected as expired.",
                authorizedClient.getClientRegistration().getClientId(), authorizedClient.getPrincipalName());
            return false;
        }
        return authorizedClient.getAccessToken().getExpiresAt().isBefore(Instant.now().plus(eagerTokenRenewal));
    }
}
