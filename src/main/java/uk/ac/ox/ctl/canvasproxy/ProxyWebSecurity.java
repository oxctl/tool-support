package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationConverter;
import uk.ac.ox.ctl.canvasproxy.security.config.annotation.web.configurers.oauth2.client.OAuth2ClientConfigurer;
import uk.ac.ox.ctl.oauth2.client.userinfo.CanvasUserService;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class ProxyWebSecurity {

    @Autowired
    @Qualifier("proxy")
    private ClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
    @Autowired
    @Qualifier("proxy")
    private BearerTokenResolver tokenResolver;
    @Autowired
    @Qualifier("proxy")
    private JwtDecoder jwtDecoder;

    @Bean
    public CanvasUserService canvasUserService() {
        return new CanvasUserService();
    }


    @Bean
    @Order(11)
    public SecurityFilterChain apiHttpSecurity(HttpSecurity http) throws Exception {
        http.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        OAuth2ClientConfigurer<HttpSecurity> configurer = new OAuth2ClientConfigurer<>();
        configurer.authorizationCodeGrant().accessTokenResponseClient(accessTokenResponseClient);
        http.apply(configurer);
        http.securityMatcher("/api/**")
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .bearerTokenResolver(tokenResolver).authenticationEntryPoint(authenticationEntryPoint())
                )
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        ;
        return http.build();
    }

    protected AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
        entryPoint.setRealmName("proxy");
        return entryPoint;
    }

    @Bean
    @Order(12)
    public SecurityFilterChain refreshTokenHttpSecurity(HttpSecurity http) throws Exception {
        http.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        http.securityMatcher("/tokens/refresh")
                .oauth2Client(oauth -> oauth.authorizationCodeGrant(authcode -> authcode.accessTokenResponseClient(accessTokenResponseClient)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder)).bearerTokenResolver(tokenResolver))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        ;
        return http.build();
    }

    @Bean
    @Order(13)
    public SecurityFilterChain checkTokenHttpSecurity(HttpSecurity http, SecurityContextRepository contextRepository) throws Exception {
        http.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        http.securityMatcher("/tokens/check")
                .oauth2Client(oauth -> oauth.authorizationCodeGrant(authcode -> authcode.accessTokenResponseClient(accessTokenResponseClient)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(securityContext -> securityContext.securityContextRepository(contextRepository))
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                // This is the only endpoint that should take the JWT out of the header and use it to start a session.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(new PersistableJwtAuthenticationConverter()))
                        .bearerTokenResolver(tokenResolver)
                        // We use this to persist the security context so that when returning from Canvas
                        // we know who the current user is and can persist the token for them. Normally a bearer token
                        // isn't persisted across requests but we use the JWT to authenticate the session
                        .withObjectPostProcessor(new ObjectPostProcessor<>() {
                            @Override
                            public Object postProcess(Object object) {
                                if (object instanceof BearerTokenAuthenticationFilter filter) {
                                    filter.setSecurityContextRepository(contextRepository);
                                }
                                return object;
                            }}))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        ;
        return http.build();
    }

    @Bean
    @Order(14)
    public SecurityFilterChain loginHttpSecurity(HttpSecurity http, SecurityContextRepository contextRepository) throws Exception {
        http.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        http.securityMatcher("/login/**")
                .oauth2Client(oauth -> oauth.authorizationCodeGrant(authcode -> authcode.accessTokenResponseClient(accessTokenResponseClient)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(securityContext -> securityContext.securityContextRepository(contextRepository))
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        ;
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        // We want to save the security context across requests for the OAuth2 flow.
        // We set a custom context key so we don't accidentally use this security context for anything else
        HttpSessionSecurityContextRepository httpSessionSecurityContextRepository = new HttpSessionSecurityContextRepository();
        httpSessionSecurityContextRepository.setSpringSecurityContextKey("OAUTH_TOKEN_GRANT");
        return httpSessionSecurityContextRepository;
    }

}
