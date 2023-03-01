package uk.ac.ox.ctl.ltiauth.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import uk.ac.ox.ctl.Issuer;
import uk.ac.ox.ctl.IssuerConfiguration;
import uk.ac.ox.ctl.ltiauth.LtiSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class LtiJwtConfig {

    private final Logger log = LoggerFactory.getLogger(LtiJwtConfig.class);

    @Autowired
    private IssuerConfiguration issuerConfiguration;

    @Autowired
    private LtiSettings ltiSettings;

    @Autowired
    private JWKSet jwkSet;


    private List<OAuth2TokenValidator<Jwt>> jwtValidators() {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        List<String> issuers = issuerConfiguration.getIssuer().values().stream().map(Issuer::getIssuer).collect(Collectors.toList());
        issuers.add(ltiSettings.getIssuer());
        validators.add(new MultiJwtIssuerValidator(issuers.toArray(new String[]{})));
        return validators;
    }

    @Bean("ltiJwtDecoder")
    @Qualifier("lti")
    public JwtDecoder allDecoder() {
        // This originally came from: org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder.processor
        ConfigurableJWTProcessor<IssuerSecurityContext> processor = new DefaultJWTProcessor<>();
        // We can't use the Spring retriever as it's package private
        DefaultResourceRetriever retriever = new DefaultResourceRetriever(2000, 10000, 128 * 1024);
        // We ignore the map keys here as it's just the values we need.
        Map<String, JWKSource<SecurityContext>> jwksMap = issuerConfiguration.getIssuer().values().stream()
                .collect(Collectors.toMap(Issuer::getIssuer, issuer -> new RemoteJWKSet<>(issuer.getJwksUrl(), retriever)));
        JWKSource<IssuerSecurityContext> jwkSource = new MultiJWKSource(jwksMap, jwkSet);

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
