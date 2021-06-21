package uk.ac.ox.ctl.canvasproxy;

import com.nimbusds.jose.util.Base64URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.PrincipalClientIdResolver;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This takes a JWT authentication token and attempts to find which client ID we should try and get a token for.
 */
@ConstructorBinding
@ConfigurationProperties("proxy")
@Validated
public class AudienceConfiguration implements PrincipalClientIdResolver {

    private final Map<String, LtiAudience> mapping;

    public AudienceConfiguration(Map<String, LtiAudience> mapping) {
        this.mapping = mapping;
    }
    
    public byte[] findHmacSecret(String audience) {
        LtiAudience ltiAudience = mapping.get(audience);
        if (ltiAudience != null) {
            return ltiAudience.secret;
        }
        return null;
    }

    /**
     * Find the issuer who should be 
     * @param audience The audience to be looked up.
     * @return The issuer that should be signing the JWT.
     */
    public String findIssuer(String audience) {
        LtiAudience ltiAudience = mapping.get(audience);
        return ltiAudience != null ? ltiAudience.getIssuer() : null;
    }

    @Override
    public String findClientId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
            List<String> audiences = jwt.getToken().getAudience();
            Set<String> clientNames = audiences.stream().map(mapping::get).filter(Objects::nonNull).map(LtiAudience::getClientName).collect(Collectors.toSet());
            Iterator<String> iterator = clientNames.iterator();
            if (iterator.hasNext()) {
                String clientName = iterator.next();
                if (iterator.hasNext()) {
                    throw new IllegalStateException("We found multiple possible client IDs for the audiences");
                }
                return clientName;
            }
        }
        return null;
    }

    /**
     * This is the mapping of an LTI tool to a proxy configuration.
     * It also contains some supporting configuration.
     */
    @ConstructorBinding
    public static class LtiAudience {
        
        @NotNull
        private final String clientName;

        private final String issuer;
        @Size(min=20)
        private final byte[] secret;

        public LtiAudience(String clientName, String issuer, String secret) {
            this.clientName = clientName;
            this.issuer = issuer;
            this.secret = secret != null ? Base64URL.from(secret).decode() : null;
        }

        /**
         * The name of the OAuth2 client configuration this LTI audience should map to.
         * @return The name of a client configuration.
         */
        public String getClientName() {
            return clientName;
        }

        /**
         * If we are accepting HMAC signed JWTs what should the issuer be?
         * @return The issuer to verify.
         */
        public String getIssuer() {
            return issuer;
        }

        /**
         * The shared secret used to sign JWTs from the application (not through the LTI launch).
         * @return The HMAC secret.
         */
        public byte[] getSecret() {
            return secret;
        }
    }
}
