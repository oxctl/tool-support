package uk.ac.ox.ctl.ltiauth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import uk.ac.ox.ctl.lti13.Lti13Configurer;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final JWTService jwtService;
    
    @Autowired
    @Qualifier("lti")
    private ClientRegistrationRepository clientRegistrationRepository;
    @Value("${lti.repo.state.limit.ip:false}")
    private boolean limitIp;

    public WebSecurityConfig(JWTService jwtService) {
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
            HttpSecurity deepLinking = http.antMatcher("/deep-linking/**");
            return secure(deepLinking, tokenResolver, jwtDecoder).build();
    }

    /**
     * This is used for the Names and Roles Provisioning Service. It needs the webapp to act as a resource server.
     */
    @Bean
    @Order(22)
    public SecurityFilterChain nrpsConfiguration(HttpSecurity http, @Qualifier("lti") BearerTokenResolver tokenResolver, @Qualifier("lti") JwtDecoder jwtDecoder) throws Exception {
            HttpSecurity namesRoles = http.antMatcher("/nrps/**");
            return secure(namesRoles, tokenResolver, jwtDecoder).build();
    }

    @Bean
    @Order(22)
    public SecurityFilterChain tokenConfiguration(HttpSecurity http) throws Exception {
        HttpSecurity token = http.antMatcher("/token");
        token.cors();
        token.csrf().disable();
        token.authorizeRequests().anyRequest().permitAll();
        return token.build();
    }

    @Bean
    @Order(23)
    public SecurityFilterChain ltiConfiguration(HttpSecurity http) throws Exception {
        HttpSecurity lti = http.antMatcher("/lti/**");
        lti.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        Lti13Configurer lti13Configurer = new CustomLti13Configurer(jwtService);
        lti13Configurer.limitIpAddresses(limitIp);
        lti.apply(lti13Configurer);
        // We need to allow the LTI launch to happen from anywhere.
        lti.cors().disable();
        return lti.build();
    }

    private static HttpSecurity secure(HttpSecurity http, BearerTokenResolver tokenResolver, JwtDecoder jwtDecoder) throws Exception {
        http
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .cors().and()
                .csrf().disable()
                .oauth2ResourceServer().jwt().decoder(jwtDecoder).and()
                .bearerTokenResolver(tokenResolver).authenticationEntryPoint(authenticationEntryPoint()).and()
                .authorizeRequests().anyRequest().authenticated();
        return http;
    }

    protected static AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
        entryPoint.setRealmName("lti");
        return entryPoint;
    }

}
