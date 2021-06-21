package uk.ac.ox.ctl.canvasproxy.jwt;

import com.nimbusds.jose.proc.SecurityContext;

import java.util.List;

/**
 * A Security Context containing the Issuer so that we can lookup the JWKS URL for the issuer.
 * This allows us to have different keys depending on the issuer.
 */
public class IssuerAndAudienceSecurityContext implements SecurityContext {

    private final String issuer;
    private final List<String> audience;

    public IssuerAndAudienceSecurityContext(String issuer, List<String> audience) {
        this.issuer = issuer;
        this.audience = audience;
    }

    public String getIssuer() {
        return issuer;
    }
    
    public List<String> getAudience() {
        return audience;
    }
}
