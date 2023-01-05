package uk.ac.ox.ctl.canvasproxy.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import uk.ac.ox.ctl.Issuer;
import uk.ac.ox.ctl.IssuerConfiguration;
import uk.ac.ox.ctl.canvasproxy.AudienceConfiguration;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

import static javax.xml.crypto.dsig.SignatureMethod.HMAC_SHA256;

/**
 * This configures JWT validation to allow any of the Canvas instances to sign a JWT.
 */
@Configuration
public class ProxyJwtConfig {

    private final Logger log = LoggerFactory.getLogger(ProxyJwtConfig.class);

    @Autowired
    private IssuerConfiguration issuerConfiguration;
    
    @Autowired
    private AudienceConfiguration audienceConfiguration; 

    // This is useful if we aren't doing JWT -> client ID mapping, but in most instances probably can be null
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
        List<String> canvasIssuers = issuerConfiguration.getIssuer().values().stream().map(Issuer::getIssuer).collect(Collectors.toList());
        validators.add(new MultiJwtIssuerValidator(jwt -> {
            // Add the standard Canvas issuers
            List<String> issuers = new ArrayList<>(canvasIssuers);
            // Add the issuers for the hmac tokens.
            jwt.getAudience().stream().map(audienceConfiguration::findIssuer).forEach(issuers::add);
            return issuers;
        }));
        return validators;
    }

    @Bean("proxyJwtDecoder")
    @Qualifier("proxy")
    public JwtDecoder allDecoder() {
        // This originally came from: org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder.processor
        ConfigurableJWTProcessor<IssuerAndAudienceSecurityContext> processor = new DefaultJWTProcessor<>();
        // We can't use the Spring retriever as it's package private
        DefaultResourceRetriever retriever = new DefaultResourceRetriever(2000, 10000, 128 * 1024);
        Map<String, JWKSource<SecurityContext>> jwksMap = issuerConfiguration.getIssuer().values().stream()
                .collect(Collectors.toMap(Issuer::getIssuer, issuer -> new RemoteJWKSet<>(issuer.getJwksUrl(), retriever)));
        JWKSource<IssuerAndAudienceSecurityContext> jwkSource = new MultiJWKSource(jwksMap);
        
        JWSKeySelector<IssuerAndAudienceSecurityContext> key = new AudienceHmacJWSKeySelector();

        // Must make sure we disable plain JWTs
        JWSVerificationKeySelector<IssuerAndAudienceSecurityContext> jwkKeySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        // Enable both RSA keys (public/private) and HMAC keys (shared secret).
        // This is so that we can have services that can impersonate any of their users.
        JWSKeySelector<IssuerAndAudienceSecurityContext> keySelector = (header, context) -> {
            Map<JWSAlgorithm, JWSKeySelector<IssuerAndAudienceSecurityContext>> selectors = Map.of(
                    JWSAlgorithm.HS256, key,
                    JWSAlgorithm.RS256, jwkKeySelector
            );
            JWSKeySelector<IssuerAndAudienceSecurityContext> selector = selectors.get(header.getAlgorithm());
            return selector != null ? selector.selectJWSKeys(header, context) : Collections.emptyList();
        };
        processor.setJWSKeySelector(keySelector);

        // Spring Security validates the claim set independent from Nimbus
        processor.setJWTClaimsSetVerifier((claims, context) -> {
        });

        MultiJwtDecoder decoder = new MultiJwtDecoder(processor);
        DelegatingOAuth2TokenValidator<Jwt> jwtValidators = new DelegatingOAuth2TokenValidator<>(jwtValidators());
        decoder.setJwtValidator(jwtValidators);
        return decoder;
    }

    /**
     * This uses the audience of the JWT to lookup a secret and then if found uses that to check
     * the signature.
     */
    private class AudienceHmacJWSKeySelector implements JWSKeySelector<IssuerAndAudienceSecurityContext> {
        @Override
        public List<? extends Key> selectJWSKeys(JWSHeader header, IssuerAndAudienceSecurityContext context) {
            if (header.getAlgorithm() != JWSAlgorithm.HS256) {
                return Collections.emptyList();
            }
            return context.getAudience().stream()
                    .map(audience -> audienceConfiguration.findHmacSecret(audience))
                    .filter(Objects::nonNull)
                    .map(secret -> new SecretKeySpec(secret, HMAC_SHA256))
                    .collect(Collectors.toList());
        }
    }
}
