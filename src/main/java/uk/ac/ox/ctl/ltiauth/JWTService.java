package uk.ac.ox.ctl.ltiauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JWTService {
    
    private final Logger log = LoggerFactory.getLogger(JWTService.class);
    
    private final JWTSigner signer;
    private final JWTStore store;
    private final LtiSettings ltiSettings;

    public JWTService(JWTSigner signer, JWTStore store, LtiSettings ltiSettings) {
        this.signer = signer;
        this.store = store;
        this.ltiSettings = ltiSettings;
    }

    /**
     * Create a new JWT signed by us based on the claims passed.
     * @param token The original token to base the clamis on.
     * @return A serialised JWT we've signed.
     */
    public String createJWT(OAuth2AuthenticationToken token, String toolSupportUrl) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        token.getPrincipal().getAttributes().forEach(builder::claim);
        builder
                // Save the original issuer, this is needed for deep linking where we need to know who the audience is.
                .claim("iss-orig", token.getPrincipal().getAttributes().get("iss"))
                .issuer(ltiSettings.getIssuer())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plus(ltiSettings.getExpiration())))
                .claim("tool_support_endpoint", toolSupportUrl);
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
