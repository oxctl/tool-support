package uk.ac.ox.ctl.ltiauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Date;

public class JWTService {
    
    private final JWTSigner signer;
    private final JWTStore store;
    private final LtiSettings ltiSettings;

    private @Autowired HttpServletRequest request;

    public static final String PROTOCOL_SEP = "://";

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
    public String createJWT(OAuth2AuthenticationToken token) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
        token.getPrincipal().getAttributes().forEach(builder::claim);
        builder
                // Save the original issuer, this is needed for deep linking where we need to know who the audience is.
                .claim("iss-orig", token.getPrincipal().getAttributes().get("iss"))
                .issuer(ltiSettings.getIssuer())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plus(ltiSettings.getExpiration())))
                .claim("tool_support_endpoint", removeLocalPart(request.getRequestURL().toString()));
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

    private String removeLocalPart(String url) {
        int hostnameStart = url.indexOf(PROTOCOL_SEP);
        if (hostnameStart == -1) {
            throw new IllegalArgumentException("Failed to find " + PROTOCOL_SEP + " in " + url);
        }
        int endHostname = url.indexOf("/", hostnameStart + PROTOCOL_SEP.length());
        if (endHostname == -1) {
            return url;
        }
        return url.substring(0, endHostname);
    }

}
