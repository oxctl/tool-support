package uk.ac.ox.ctl.ltiauth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    public ResponseEntity<Map<String, Object>> keys() {
        return
            ResponseEntity.ok()
                    // This is needed so that consumers cache it for a reasonable amount of time.
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                    .body(this.jwkSet.toJSONObject());
    }

}
