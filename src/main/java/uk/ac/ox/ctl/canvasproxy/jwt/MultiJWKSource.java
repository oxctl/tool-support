package uk.ac.ox.ctl.canvasproxy.jwt;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows the JWKs to be looked up in multiple sources depending on the issuer.
 */
public class MultiJWKSource implements JWKSource<IssuerSecurityContext> {

    private final Map<String, JWKSource<SecurityContext>> sources;

    public MultiJWKSource(Map<String, JWKSource<SecurityContext>> sources) {
        this.sources = new HashMap<>(sources);
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, IssuerSecurityContext context) throws KeySourceException {
        JWKSource<SecurityContext> securityContextJWKSource = sources.get(context.getIssuer());
        if (securityContextJWKSource != null) {
            return securityContextJWKSource.get(jwkSelector, context);
        }
        return Collections.emptyList();
    }
}
