package uk.ac.ox.ctl.ltiauth.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKSet;
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
import uk.ac.ox.ctl.ltiauth.LtiSettings;
import uk.ac.ox.ctl.ltiauth.MultiAudienceConfigResolver;
import uk.ac.ox.ctl.repository.ToolRepository;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

import static javax.xml.crypto.dsig.SignatureMethod.HMAC_SHA256;

@Configuration
public class LtiJwtConfig {

    private final Logger log = LoggerFactory.getLogger(LtiJwtConfig.class);

    @Autowired
    private LtiSettings ltiSettings;

    @Autowired
    private JWKSet jwkSet;

    // This is useful if we aren't doing JWT -> client ID mapping, but in most instances probably can be null
    @Value("${jwt.audience:#{null}}")
    private String audience;

    @Bean("ltiJwtDecoder")
    @Qualifier("lti")
    public JwtDecoder allDecoder(IssuerConfiguration issuerConfiguration, MultiAudienceConfigResolver multiAudienceConfigResolver, JWKSet jwkSet) {
        // This originally came from: org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder.processor
        ConfigurableJWTProcessor<IssuerAndAudienceSecurityContext> processor = new DefaultJWTProcessor<>();
        // We can't use the Spring retriever as it's package private
        DefaultResourceRetriever retriever = new DefaultResourceRetriever(2000, 10000, 128 * 1024);
        Map<String, JWKSource<SecurityContext>> jwksMap = issuerConfiguration.getIssuer().values().stream()
                .collect(Collectors.toMap(Issuer::getIssuer, issuer -> new RemoteJWKSet<>(issuer.getJwksUrl(), retriever)));
        JWKSource<IssuerAndAudienceSecurityContext> jwkSource = new MultiJWKSource(jwksMap, jwkSet);

        JWSKeySelector<IssuerAndAudienceSecurityContext> key = new AudienceHmacJWSKeySelector(multiAudienceConfigResolver);

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
        DelegatingOAuth2TokenValidator<Jwt> jwtValidators = delegatingOAuth2TokenValidator(issuerConfiguration, multiAudienceConfigResolver);
        decoder.setJwtValidator(jwtValidators);
        return decoder;
    }

    @Bean
    MultiAudienceConfigResolver ltiMultiAudienceConfigResolver(ToolRepository toolRepository) {
        return new MultiAudienceConfigResolver(toolRepository);
    }

    private DelegatingOAuth2TokenValidator delegatingOAuth2TokenValidator(IssuerConfiguration issuerConfiguration, MultiAudienceConfigResolver ltiMultiAudienceConfigResolver) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        if (audience != null) {
            validators.add(new JwtAudienceValidator(audience));
        } else {
            log.info("No audience configured, accepting all JWTs");
        }
        List<String> staticIssuers = issuerConfiguration.getIssuer().values().stream().map(Issuer::getIssuer).toList();
        validators.add(new MultiJwtIssuerValidator(jwt -> {
            // Add the standard static issuers
            List<String> issuers = new ArrayList<>(staticIssuers);
            // Add the issuers for the hmac tokens.
            jwt.getAudience().stream().map(ltiMultiAudienceConfigResolver::findIssuer).forEach(issuers::add);
            return issuers;
        }));
        return new DelegatingOAuth2TokenValidator<>(validators);
    }

    /**
     * This uses the audience of the JWT to lookup a secret and then if found uses that to check
     * the signature.
     */
    private static class AudienceHmacJWSKeySelector implements JWSKeySelector<IssuerAndAudienceSecurityContext> {

        private final MultiAudienceConfigResolver multiAudienceConfigResolver;

        public AudienceHmacJWSKeySelector(MultiAudienceConfigResolver multiAudienceConfigResolver) {
            this.multiAudienceConfigResolver = multiAudienceConfigResolver;
        }

        @Override
        public List<? extends Key> selectJWSKeys(JWSHeader header, IssuerAndAudienceSecurityContext context) {
            if (header.getAlgorithm() != JWSAlgorithm.HS256) {
                return Collections.emptyList();
            }
            return context.getAudience().stream()
                    .map(multiAudienceConfigResolver::findHmacSecret)
                    .filter(Objects::nonNull)
                    .map(secret -> new SecretKeySpec(secret, HMAC_SHA256))
                    .collect(Collectors.toList());
        }
    }

}
