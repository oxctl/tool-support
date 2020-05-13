package uk.ac.ox.ctl.canvasproxy;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import uk.ac.ox.ctl.canvasproxy.model.PrincipalTokens;
import uk.ac.ox.ctl.canvasproxy.repository.PrincipalTokensRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * This persists the OAuth2 tokens in the DB, this means we don't have to get the user to
 * authenticate each time they use the tool.
 */
@Service
public class PrincipalOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    private final PrincipalTokensRepository principalTokensRepository;

    private final ClientRegistrationRepository clientRegistrationRepository;

    public PrincipalOAuth2AuthorizedClientRepository(
            PrincipalTokensRepository principalTokensRepository,
            ClientRegistrationRepository clientRegistrationRepository) {
        this.principalTokensRepository = principalTokensRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
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
        return (T) oAuth2AuthorizedClient;
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
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
            List<String> aud = jwtAuthenticationToken.getToken().getAudience();
            if (aud.isEmpty()) {
                throw new IllegalStateException("JWT must have an audience set.");
            }
            if (aud.size() > 1) {
                // At some point we should support this and use something other than the audience on the JWT so we can
                // support tokens with multiple audiences
                throw new IllegalStateException("JWT cannot have multiple audiences set.");
            }
            // We want to be double sure that we have a name.
            if (jwtAuthenticationToken.getName() == null) {
                throw new IllegalStateException("JWT name cannot be null");
            }
            // This is so that if we have multiple tools in the same Canvas instance each tool has a separate pool
            // of tokens it uses.
            return aud.get(0) + ":"+ jwtAuthenticationToken.getName();
        }
        return authentication.getName();
    }
}
