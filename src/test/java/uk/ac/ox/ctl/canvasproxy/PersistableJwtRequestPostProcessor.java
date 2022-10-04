package uk.ac.ox.ctl.canvasproxy;

import org.springframework.core.convert.converter.Converter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.util.Assert;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationToken;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * This is copied from Spring but updated to support PersistableJwtAuthenticationTokens.
 */
public final class PersistableJwtRequestPostProcessor implements RequestPostProcessor {

    private Jwt jwt;

    private Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    public PersistableJwtRequestPostProcessor() {
        this.jwt((jwt) -> {
        });
    }

    /**
     * Use the given {@link Jwt.Builder} {@link Consumer} to configure the underlying
     * {@link Jwt}
     *
     * This method first creates a default {@link Jwt.Builder} instance with default
     * values for the {@code alg}, {@code sub}, and {@code scope} claims. The
     * {@link Consumer} can then modify these or provide additional configuration.
     *
     * Calling {@link SecurityMockMvcRequestPostProcessors#jwt()} is the equivalent of
     * calling {@code SecurityMockMvcRequestPostProcessors.jwt().jwt(() -> {})}.
     * @param jwtBuilderConsumer For configuring the underlying {@link Jwt}
     * @return the {@link PersistableJwtRequestPostProcessor} for additional customization
     */
    public PersistableJwtRequestPostProcessor jwt(Consumer<Jwt.Builder> jwtBuilderConsumer) {
        Jwt.Builder jwtBuilder = Jwt.withTokenValue("token").header("alg", "none").claim(JwtClaimNames.SUB, "user")
                .claim("scope", "read");
        jwtBuilderConsumer.accept(jwtBuilder);
        this.jwt = jwtBuilder.build();
        return this;
    }

    /**
     * Use the given {@link Jwt}
     * @param jwt The {@link Jwt} to use
     * @return the {@link PersistableJwtRequestPostProcessor} for additional customization
     */
    public PersistableJwtRequestPostProcessor jwt(Jwt jwt) {
        this.jwt = jwt;
        return this;
    }

    /**
     * Use the provided authorities in the token
     * @param authorities the authorities to use
     * @return the {@link PersistableJwtRequestPostProcessor} for further configuration
     */
    public PersistableJwtRequestPostProcessor authorities(Collection<GrantedAuthority> authorities) {
        Assert.notNull(authorities, "authorities cannot be null");
        this.authoritiesConverter = (jwt) -> authorities;
        return this;
    }

    /**
     * Use the provided authorities in the token
     * @param authorities the authorities to use
     * @return the {@link PersistableJwtRequestPostProcessor} for further configuration
     */
    public PersistableJwtRequestPostProcessor authorities(GrantedAuthority... authorities) {
        Assert.notNull(authorities, "authorities cannot be null");
        this.authoritiesConverter = (jwt) -> Arrays.asList(authorities);
        return this;
    }

    /**
     * Provides the configured {@link Jwt} so that custom authorities can be derived
     * from it
     * @param authoritiesConverter the conversion strategy from {@link Jwt} to a
     * {@link Collection} of {@link GrantedAuthority}s
     * @return the {@link PersistableJwtRequestPostProcessor} for further configuration
     */
    public PersistableJwtRequestPostProcessor authorities(Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter) {
        Assert.notNull(authoritiesConverter, "authoritiesConverter cannot be null");
        this.authoritiesConverter = authoritiesConverter;
        return this;
    }

    @Override
    public MockHttpServletRequest postProcessRequest(MockHttpServletRequest request) {
        CsrfFilter.skipRequest(request);
        PersistableJwtAuthenticationToken token = new PersistableJwtAuthenticationToken(this.jwt,
                this.authoritiesConverter.convert(this.jwt));
        return new AuthenticationRequestPostProcessor(token).postProcessRequest(request);
    }

}

