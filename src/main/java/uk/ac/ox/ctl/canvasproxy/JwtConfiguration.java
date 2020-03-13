package uk.ac.ox.ctl.canvasproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri;

@Configuration
public class JwtConfiguration {

    private final Logger log = LoggerFactory.getLogger(JwtConfiguration.class);

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.jwks.uri}")
    private String jwksUri;

    // This is useful if we aren't doing doing JWT -> client ID mapping, but in most instances probably can be null
    @Value("${jwt.audience:#{null}}")
    private String audience;


    @Bean
    public JwtDecoder jwtDecoder() {
        // See org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer
        // We should do some validation here that the configuration is good.
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        if (audience != null) {
            validators.add(new JwtAudienceValidator(audience));
        } else {
            log.info("No audience configured, accepting all JWTs");
        }
        validators.add(new JwtIssuerValidator(issuer));
        DelegatingOAuth2TokenValidator<Jwt> jwtValidators = new DelegatingOAuth2TokenValidator<>(validators);
        NimbusJwtDecoder jwtDecoder = withJwkSetUri(jwksUri).build();
        jwtDecoder.setJwtValidator(jwtValidators);
        return jwtDecoder;
    }
}
