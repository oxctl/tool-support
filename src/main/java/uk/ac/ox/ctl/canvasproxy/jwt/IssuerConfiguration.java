package uk.ac.ox.ctl.canvasproxy.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.Map;

@ConfigurationProperties("jwt")
@ConstructorBinding
@Validated
public class IssuerConfiguration {

    @Valid
    private final Map<String, Issuer> issuer;

    public IssuerConfiguration(Map<String, Issuer> issuer) {
        this.issuer = issuer;
    }

    public Map<String, Issuer> getIssuer() {
        return issuer;
    }
}
