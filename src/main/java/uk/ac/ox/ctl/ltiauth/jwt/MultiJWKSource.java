package uk.ac.ox.ctl.ltiauth.jwt;

import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows the JWKs to be looked up in multiple sources depending on the issuer.
 */
public class MultiJWKSource implements JWKSource<IssuerSecurityContext> {

    private final Map<String, JWKSource<SecurityContext>> sources;

    // These are our internal keys.
    private final JWKSet jwkSet;


    public MultiJWKSource(Map<String, JWKSource<SecurityContext>> sources, JWKSet jwkSet) {
        this.sources = new HashMap<>(sources);
        this.jwkSet = jwkSet;
    }

    @Override
    public List<JWK> get(JWKSelector jwkSelector, IssuerSecurityContext context) throws KeySourceException {
        List<JWK> jwks = new ArrayList<>();
        JWKSource<SecurityContext> securityContextJWKSource = sources.get(context.getIssuer());
        if (securityContextJWKSource != null) {
            jwks.addAll(securityContextJWKSource.get(jwkSelector, context));
        }
        // Add our own internal 
        jwks.addAll(jwkSet.getKeys());
        return jwks;
    }
}
