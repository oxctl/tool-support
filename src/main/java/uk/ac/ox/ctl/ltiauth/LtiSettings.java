package uk.ac.ox.ctl.ltiauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix="lti")
@ConstructorBinding
public class LtiSettings {
    
    // How long to sign the token for by default.
    private final Duration expiration;
    
    private final String issuer;

    public LtiSettings(Duration expiration, String issuer) {
        this.expiration = expiration != null ? expiration : Duration.of(8, ChronoUnit.HOURS);
        this.issuer = issuer != null ? issuer : "https://localhost";
    }

    public Duration getExpiration() {
        return expiration;
    }

    public String getIssuer() {
        return issuer;
    }
}
