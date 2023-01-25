package uk.ac.ox.ctl.ltiauth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * This publishes the public keys we have so that it's easier to configure things.
 */
@RestController
public class JWKSController {

    private final JWKSet jwkSet;

    public JWKSController(JWKSet jwkSet) {
        this.jwkSet = jwkSet;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return this.jwkSet.toJSONObject();
    }

}
