package uk.ac.ox.ctl.ltiauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import uk.ac.ox.ctl.model.Tool;
import uk.ac.ox.ctl.repository.ToolRepository;

import java.time.Instant;
import java.util.Date;

public class JWTService {
    
    private final JWTSigner signer;
    private final JWTStore store;
    private final LtiSettings ltiSettings;
    private final ToolRepository toolRepository;
    
    public JWTService(JWTSigner signer, JWTStore store, LtiSettings ltiSettings, ToolRepository toolRepository) {
        this.signer = signer;
        this.store = store;
        this.ltiSettings = ltiSettings;
        this.toolRepository = toolRepository;
    }

    /**
     * Are we signing our own JWT for this client?
     * 
     * @param clientRegistrationId The client registration ID.
     * @return true if we are.
     */
    public boolean isSigning(String clientRegistrationId) {
        return toolRepository.isSigningEnabled(clientRegistrationId)
                .orElse(ltiSettings.isSign());
    }

    /**
     * Create a new JWT signed by us based on the claims passed.
     * @param token The original token to base the clamis on.
     * @return A serialised JWT we've signed.
     */
    public String createJWT(OAuth2AuthenticationToken token) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        token.getPrincipal().getAttributes().forEach(builder::claim);
        builder
                // Save the original issuer, this is needed for deep linking where we need to know who the audience is.
                .claim("iss-orig", token.getPrincipal().getAttributes().get("iss"))
                .issuer(ltiSettings.getIssuer())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plus(ltiSettings.getExpiration())));
        try {
            final SignedJWT jwt = signer.signJWT(token.getAuthorizedClientRegistrationId(), builder.build());
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new BadCredentialsException("Failed to create new JWT");
        }
    }

    public String store(Object token) {
        return store.store(token);
    }

    public Object retrieve(String key) {
        return store.retrieve(key);
    }

}
