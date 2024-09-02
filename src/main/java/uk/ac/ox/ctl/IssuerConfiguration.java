package uk.ac.ox.ctl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.util.Map;

@ConfigurationProperties("jwt")
@Validated
public class IssuerConfiguration {

    @Valid
    private final Map<String, Issuer> issuer;

    @ConstructorBinding
    public IssuerConfiguration(Map<String, Issuer> issuer) {
        this.issuer = issuer;
    }

    public Map<String, Issuer> getIssuer() {
        return issuer;
    }
}
