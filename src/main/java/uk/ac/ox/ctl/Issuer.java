package uk.ac.ox.ctl;

import org.springframework.boot.context.properties.ConstructorBinding;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URL;

@ConstructorBinding
public class Issuer {

    @NotEmpty
    private final String issuer;
    @NotNull
    private final URL jwksUrl;

    public Issuer(String issuer, URL jwksUrl) {
        this.issuer = issuer;
        this.jwksUrl = jwksUrl;
    }

    public String getIssuer() {
        return issuer;
    }

    public URL getJwksUrl() {
        return jwksUrl;
    }
}
