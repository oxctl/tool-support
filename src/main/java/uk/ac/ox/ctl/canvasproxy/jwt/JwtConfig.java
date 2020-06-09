package uk.ac.ox.ctl.canvasproxy.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This configures JWT validation to allow any of the Canvas instances to sign a JWT.
 */
@Configuration
public class JwtConfig {

    private final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Autowired
    private IssuerConfiguration issuerConfiguration;

    // This is useful if we aren't doing doing JWT -> client ID mapping, but in most instances probably can be null
    @Value("${jwt.audience:#{null}}")
    private String audience;


    private List<OAuth2TokenValidator<Jwt>> jwtValidators() {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        if (audience != null) {
            validators.add(new JwtAudienceValidator(audience));
        } else {
            log.info("No audience configured, accepting all JWTs");
        }
        String[] objects = issuerConfiguration.getIssuer().values().stream().map(Issuer::getIssuer).toArray(String[]::new);
        validators.add(new MultiJwtIssuerValidator(objects));
        return validators;
    }

    @Bean
    public JwtDecoder allDecoder() {
        // This originally came from: org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder.processor
        ConfigurableJWTProcessor<IssuerSecurityContext> processor = new DefaultJWTProcessor<>();
        // We can't use the Spring retriever as it's package private
        DefaultResourceRetriever retriever = new DefaultResourceRetriever(2000, 10000, 128 * 1024);
        Map<String, JWKSource<SecurityContext>> jwksMap = issuerConfiguration.getIssuer().values().stream()
                .collect(Collectors.toMap(Issuer::getIssuer, issuer -> new RemoteJWKSet<>(issuer.getJwksUrl(), retriever)));
        JWKSource<IssuerSecurityContext> jwkSource = new MultiJWKSource(jwksMap);

        // Must make sure we disable plain JWTs
        JWSVerificationKeySelector<IssuerSecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        processor.setJWSKeySelector(keySelector);

        // Spring Security validates the claim set independent from Nimbus
        processor.setJWTClaimsSetVerifier((claims, context) -> {
        });

        MultiJwtDecoder decoder = new MultiJwtDecoder(processor);
        DelegatingOAuth2TokenValidator<Jwt> jwtValidators = new DelegatingOAuth2TokenValidator<>(jwtValidators());
        decoder.setJwtValidator(jwtValidators);
        return decoder;
    }

}
