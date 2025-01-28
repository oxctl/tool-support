package uk.ac.ox.ctl.ltiauth.servicetoken;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTClaimsSetAwareJWSKeySelector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import uk.ac.ox.ctl.ltiauth.MultiAudienceConfigResolver;
import uk.ac.ox.ctl.ltiauth.jwt.MultiJwtIssuerValidator;

import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static javax.xml.crypto.dsig.SignatureMethod.HMAC_SHA256;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class ServiceTokenSecurity {

    @Bean
    @Order(25)
    public SecurityFilterChain another(HttpSecurity http, @Qualifier("securityTokenDecoder") JwtDecoder jwtDecoder) throws Exception {
        return http.securityMatcher("/service-token")
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .bearerTokenResolver(new DefaultBearerTokenResolver()) // Needed because there are 2 default resolvers
                )
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .build();
    }

    @Bean("securityTokenDecoder")
    public JwtDecoder securityTokenDecoder(MultiAudienceConfigResolver multiAudienceConfigResolver) {
        // This is a decoder that just uses a shared secret between the tool and the platform.
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

        // Lookup key based on the audience.
        JWTClaimsSetAwareJWSKeySelector<SecurityContext> jwsKeySelector = (header, claimsSet, context) -> claimsSet.getAudience().stream()
                .map(multiAudienceConfigResolver::findHmacSecret)
                .filter(Objects::nonNull)
                .map(secret -> new SecretKeySpec(secret, HMAC_SHA256))
                .toList();
        processor.setJWTClaimsSetAwareJWSKeySelector(jwsKeySelector);

        NimbusJwtDecoder jwtDecoder = new NimbusJwtDecoder(processor);
        // Spring Security validates the claim set independently of Nimbus, so we don't do that on the decoder.

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new MultiJwtIssuerValidator(jwt -> {
            // Add the issuers for the hmac tokens.
            return jwt.getAudience().stream().map(multiAudienceConfigResolver::findIssuer).toList();
        }));
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));

        return jwtDecoder;
    }
}
