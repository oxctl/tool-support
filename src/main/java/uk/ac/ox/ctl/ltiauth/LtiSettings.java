package uk.ac.ox.ctl.ltiauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

@ConfigurationProperties(prefix="lti")
@ConstructorBinding
public class LtiSettings {

    private final Map<String, ClientSettings> client;
    // Are we signing by default?
    private final boolean sign;
    
    // How long to sign the token for by default.
    private final Duration expiration;
    
    private final String issuer;

    public LtiSettings(Map<String, ClientSettings> client, boolean sign, Duration expiration, String issuer) {
        this.client = client != null ? client : Collections.emptyMap();
        this.sign = sign;
        this.expiration = expiration != null ? expiration : Duration.of(8, ChronoUnit.HOURS);
        this.issuer = issuer != null ? issuer : "https://localhost";
    }


    /**
     * @deprecated This should be coming from the database once migration is complete.
     */
    public ClientSettings getClientSettings(String clientRegistrationId) {
        return client.get(clientRegistrationId);
    }
    
    public boolean isSign() {
        return sign;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public String getIssuer() {
        return issuer;
    }

    // This has been mapped onto a Tool model.
    public static class ClientSettings {
        private final boolean sign;

        public ClientSettings(boolean sign) {
            this.sign = sign;
        }

        public boolean isSign() {
            return sign;
        }
    }
}
