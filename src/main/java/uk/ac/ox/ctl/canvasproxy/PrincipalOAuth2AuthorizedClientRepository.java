package uk.ac.ox.ctl.canvasproxy;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ox.ctl.canvasproxy.model.PrincipalTokens;
import uk.ac.ox.ctl.canvasproxy.repository.PrincipalTokensRepository;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.OAuth2AccessTokenRefresher;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.RefreshOAuth2AuthorizedClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * This persists the OAuth2 tokens in the DB, this means we don't have to get the user to
 * authenticate each time they use the tool.
 */
public class PrincipalOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository, RefreshOAuth2AuthorizedClient {

    private final PrincipalTokensRepository principalTokensRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AccessTokenRefresher auth2AccessTokenRefresher;

    public PrincipalOAuth2AuthorizedClientRepository(
            PrincipalTokensRepository principalTokensRepository,
            ClientRegistrationRepository clientRegistrationRepository, 
            OAuth2AccessTokenRefresher auth2AccessTokenRefresher) {
        this.principalTokensRepository = principalTokensRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.auth2AccessTokenRefresher = auth2AccessTokenRefresher;
    }

    @SuppressWarnings("unchecked")
    @Override
    public OAuth2AuthorizedClient loadAuthorizedClient(
            String clientRegistrationId, Authentication authentication, HttpServletRequest request) {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = null;
        ClientRegistration clientRegistration =
                clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (clientRegistration != null) {
            String principal = toPrincipal(authentication);
            oAuth2AuthorizedClient =
                    // Second level caching should catch this lookup by ID.
                    principalTokensRepository
                            .findById(principal)
                            .map(userTokens -> userTokens.toOAuth2AuthorizedClient(clientRegistration, principal))
                            .orElse(null);
        }
        return oAuth2AuthorizedClient;
    }

    /**
     * This refreshes the access token associated with the authentication.
     *
     * @param clientRegistrationId The client registration ID.
     * @param authentication The authentication for the current user.
     * @param request The HTTP Servlet Request.
     * @param response The HTTP Servlet Response.
     * @return The refreshed Oauth2AuthorizedClient or null if the refresh fails.
     */
    @SuppressWarnings("unchecked")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OAuth2AuthorizedClient renewAccessToken(
            String clientRegistrationId, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizedClient oAuth2AuthorizedClient;
        ClientRegistration clientRegistration =
                clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (clientRegistration != null) {
            String principal = toPrincipal(authentication);
            oAuth2AuthorizedClient =
                    // Second level caching should catch this lookup by ID.
                    principalTokensRepository
                            // This should aquire a lock which prevents another
                            .lockById(principal)
                            .map(userTokens -> userTokens.toOAuth2AuthorizedClient(clientRegistration, principal))
                            .orElse(null);

            if (oAuth2AuthorizedClient != null) {
                // We want a way to force a refresh, so that we can be sure that we have a valid token.
                boolean forced = "true".equals(request.getParameter("force"));
                // Now we have the lock we check again if it needs refreshing
                if (forced || needsRenewal(oAuth2AuthorizedClient)) {
                    OAuth2AccessTokenResponse refresh = auth2AccessTokenRefresher.refresh(oAuth2AuthorizedClient);
                    if (refresh != null) {
                        OAuth2AuthorizedClient renewed = new OAuth2AuthorizedClient(oAuth2AuthorizedClient.getClientRegistration(),
                                principal, refresh.getAccessToken(), refresh.getRefreshToken());
                        saveAuthorizedClient(renewed, authentication, request, response);
                        return renewed;
                    }
                    // Should we clear out the access token at this point as we didn't manage to refresh?
                } else {
                    // Looks like another thread renewed the token so just return the new one.
                    return oAuth2AuthorizedClient;
                }
            }
        }
        return null;
    }

    public boolean needsRenewal(OAuth2AuthorizedClient oAuth2AuthorizedClient) {
        return auth2AccessTokenRefresher.needsRefresh(oAuth2AuthorizedClient);
    }


    @Override
    public void saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        PrincipalTokens userTokens = new PrincipalTokens(toPrincipal(authentication), authorizedClient);
        principalTokensRepository.save(userTokens);
    }

    @Override
    public void removeAuthorizedClient(
            String clientRegistrationId,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        principalTokensRepository.deleteById(toPrincipal(authentication));
    }

    private String toPrincipal(Authentication authentication) {
        if (authentication instanceof AbstractOAuth2TokenAuthenticationToken) {
            AbstractOAuth2TokenAuthenticationToken<Jwt> persistableJwtAuthenticationToken = (AbstractOAuth2TokenAuthenticationToken<Jwt>) authentication;
            List<String> aud = persistableJwtAuthenticationToken.getToken().getAudience();
            if (aud.isEmpty()) {
                throw new IllegalStateException("JWT must have an audience set.");
            }
            if (aud.size() > 1) {
                // At some point we should support this and use something other than the audience on the JWT so we can
                // support tokens with multiple audiences
                throw new IllegalStateException("JWT cannot have multiple audiences set.");
            }
            // We want to be double sure that we have a name.
            if (persistableJwtAuthenticationToken.getName() == null) {
                throw new IllegalStateException("JWT name cannot be null");
            }
            // This is so that if we have multiple tools in the same Canvas instance each tool has a separate pool
            // of tokens it uses.
            return aud.get(0) + ":"+ persistableJwtAuthenticationToken.getName();
        }
        return authentication.getName();
    }
}
