package uk.ac.ox.ctl;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.net.URL;

public class Issuer {

    @NotEmpty
    private final String issuer;
    @NotNull
    private final URL jwksUrl;

    @ConstructorBinding
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
