package uk.ac.ox.ctl.ltiauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import uk.ac.ox.ctl.lti13.Lti13Configurer;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class LtiWebSecurity {

    private final JWTService jwtService;

    @Autowired
    @Qualifier("lti")
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @Value("${lti.repo.state.limit.ip:false}")
    private boolean limitIp;

    public LtiWebSecurity(JWTService jwtService) {
        this.jwtService = jwtService;
    }


    @Bean("ltiBearerTokenResolver")
    @Qualifier("lti")
    public BearerTokenResolver bearerTokenResolver() {
        return new DefaultBearerTokenResolver();
    }

    /**
     * This is used for the Deep Linking support. It needs the webapp to act as a resource server.
     */
    @Bean
    @Order(21)
    public SecurityFilterChain deepLinkingConfiguration(HttpSecurity http, @Qualifier("lti") BearerTokenResolver tokenResolver, @Qualifier("lti") JwtDecoder jwtDecoder) throws Exception {
        HttpSecurity deepLinking = http.securityMatcher("/deep-linking/**");
        return secure(deepLinking, tokenResolver, jwtDecoder).build();
    }

    /**
     * This is used for the Names and Roles Provisioning Service. It needs the webapp to act as a resource server.
     */
    @Bean
    @Order(22)
    public SecurityFilterChain nrpsConfiguration(HttpSecurity http, @Qualifier("lti") BearerTokenResolver tokenResolver, @Qualifier("lti") JwtDecoder jwtDecoder) throws Exception {
        HttpSecurity namesRoles = http.securityMatcher("/nrps/**");
        return secure(namesRoles, tokenResolver, jwtDecoder).build();
    }

    @Bean
    @Order(22)
    public SecurityFilterChain tokenConfiguration(HttpSecurity http) throws Exception {
        return http.securityMatcher("/token")
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(23)
    public SecurityFilterChain ltiConfiguration(HttpSecurity http) throws Exception {
        HttpSecurity lti = http.securityMatcher("/lti/**");
        lti.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        Lti13Configurer lti13Configurer = new CustomLti13Configurer(jwtService, clientRegistrationService);
        lti13Configurer.limitIpAddresses(limitIp);
        lti.apply(lti13Configurer);
        // We need to allow the LTI launch to happen from anywhere.
        lti.cors(AbstractHttpConfigurer::disable);
        return lti.build();
    }

    @Bean
    @Order(24)
    public SecurityFilterChain jwksSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/.well-known/jwks.json")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }

    private static HttpSecurity secure(HttpSecurity http, BearerTokenResolver tokenResolver, JwtDecoder jwtDecoder) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder)).bearerTokenResolver(tokenResolver).authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
    }

    protected static AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
        entryPoint.setRealmName("lti");
        return entryPoint;
    }

}
