package uk.ac.ox.ctl.canvasproxy.jwt;

import com.nimbusds.jose.proc.SecurityContext;

/**
 * A Security Context containing the Issuer so that we can lookup the JWKS URL for the issuer.
 */
public class IssuerSecurityContext implements SecurityContext {

    private final String issuer;


    public IssuerSecurityContext(String issuer) {
        this.issuer = issuer;
    }

    public String getIssuer() {
        return issuer;
    }
}
