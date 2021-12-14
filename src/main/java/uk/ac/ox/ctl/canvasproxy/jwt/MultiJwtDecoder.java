package uk.ac.ox.ctl.canvasproxy.jwt;

import com.nimbusds.jose.RemoteKeySourceException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.proc.JWTProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @see NimbusJwtDecoder
 */
@Slf4j
public final class MultiJwtDecoder implements JwtDecoder {
    private static final String DECODING_ERROR_MESSAGE_TEMPLATE =
            "An error occurred while attempting to decode the Jwt: %s";

    private final JWTProcessor<IssuerAndAudienceSecurityContext> jwtProcessor;

    private Converter<Map<String, Object>, Map<String, Object>> claimSetConverter =
            MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());
    private OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefault();

    /**
     * Configures a {@link MultiJwtDecoder} with the given parameters
     *
     * @param jwtProcessor - the {@link JWTProcessor} to use
     */
    public MultiJwtDecoder(JWTProcessor<IssuerAndAudienceSecurityContext> jwtProcessor) {
        Assert.notNull(jwtProcessor, "jwtProcessor cannot be null");
        this.jwtProcessor = jwtProcessor;
    }

    /**
     * Use this {@link Jwt} Validator
     *
     * @param jwtValidator - the Jwt Validator to use
     */
    public void setJwtValidator(OAuth2TokenValidator<Jwt> jwtValidator) {
        Assert.notNull(jwtValidator, "jwtValidator cannot be null");
        this.jwtValidator = jwtValidator;
    }

    /**
     * Use the following {@link Converter} for manipulating the JWT's claim set
     *
     * @param claimSetConverter the {@link Converter} to use
     */
    public void setClaimSetConverter(Converter<Map<String, Object>, Map<String, Object>> claimSetConverter) {
        Assert.notNull(claimSetConverter, "claimSetConverter cannot be null");
        this.claimSetConverter = claimSetConverter;
    }

    /**
     * Decode and validate the JWT from its compact claims representation format
     *
     * @param token the JWT value
     * @return a validated {@link Jwt}
     * @throws JwtException If the
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        JWT jwt = parse(token);
        if (jwt instanceof PlainJWT) {
            throw new JwtException("Unsupported algorithm of " + jwt.getHeader().getAlgorithm());
        }
        Jwt createdJwt = createJwt(token, jwt);
        return validateJwt(createdJwt);
    }

    private JWT parse(String token) {
        log.info("token ::: " + token);
        try {
            return JWTParser.parse(token);
        } catch (Exception ex) {
            throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
        }
    }

    private Jwt createJwt(String token, JWT parsedJwt) {
        try {
            // Verify the signature
            String issuer = parsedJwt.getJWTClaimsSet().getIssuer();
            if (issuer == null) {
                throw new JwtException("JWT must have an issuer to be processed.");
            }
            List<String> audience = parsedJwt.getJWTClaimsSet().getAudience();
            if (audience.isEmpty()) {
                throw new JwtException("JWT must have an audience to be processed.");
            }
            
            JWTClaimsSet jwtClaimsSet = this.jwtProcessor.process(parsedJwt, new IssuerAndAudienceSecurityContext(issuer, audience));

            Map<String, Object> headers = new LinkedHashMap<>(parsedJwt.getHeader().toJSONObject());
            Map<String, Object> claims = this.claimSetConverter.convert(jwtClaimsSet.getClaims());

            Jwt.Builder builder = Jwt.withTokenValue(token)
                    .headers(h -> h.putAll(headers));
            if (claims != null) {
                builder.claims(c -> c.putAll(claims));
            }
            return builder.build();
        } catch (RemoteKeySourceException ex) {
            if (ex.getCause() instanceof ParseException) {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, "Malformed Jwk set"));
            } else {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
            }
        } catch (Exception ex) {
            if (ex.getCause() instanceof ParseException) {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, "Malformed payload"));
            } else {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
            }
        }
    }

    private Jwt validateJwt(Jwt jwt) {
        OAuth2TokenValidatorResult result = this.jwtValidator.validate(jwt);
        if (result.hasErrors()) {
            String description = result.getErrors().iterator().next().getDescription();
            throw new JwtValidationException(
                    String.format(DECODING_ERROR_MESSAGE_TEMPLATE, description),
                    result.getErrors());
        }

        return jwt;
    }
}

