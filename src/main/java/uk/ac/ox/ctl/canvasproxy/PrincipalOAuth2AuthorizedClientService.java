package uk.ac.ox.ctl.canvasproxy;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import uk.ac.ox.ctl.canvasproxy.model.PrincipalTokens;
import uk.ac.ox.ctl.canvasproxy.repository.PrincipalTokensRepository;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationToken;

import java.util.List;

public class PrincipalOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {
    private final PrincipalTokensRepository principalTokensRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public PrincipalOAuth2AuthorizedClientService(PrincipalTokensRepository principalTokensRepository, ClientRegistrationRepository clientRegistrationRepository) {
        this.principalTokensRepository = principalTokensRepository;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = null;
        ClientRegistration clientRegistration =
                clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (clientRegistration != null) {
            String principal = principalName;
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
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        PrincipalTokens userTokens = new PrincipalTokens(toPrincipal(principal), authorizedClient);
        principalTokensRepository.save(userTokens);

    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        principalTokensRepository.deleteById(principalName);
    }

    private String toPrincipal(Authentication authentication) {
        if (authentication instanceof PersistableJwtAuthenticationToken) {
            PersistableJwtAuthenticationToken persistableJwtAuthenticationToken = (PersistableJwtAuthenticationToken) authentication;
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
