package uk.ac.ox.ctl.canvasproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import uk.ac.ox.ctl.canvasproxy.security.PersistableJwtAuthenticationConverter;
import uk.ac.ox.ctl.canvasproxy.security.config.annotation.web.configurers.oauth2.client.OAuth2ClientConfigurer;
import uk.ac.ox.ctl.oauth2.client.userinfo.CanvasUserService;

@EnableWebSecurity
@Configuration
public class WebSecurity {

    @Autowired
    @Qualifier("proxy")
    private ClientRegistrationRepository clientRegistrationRepository;
    @Autowired
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;
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
        configurer.setBuilder(http);
        configurer.authorizedClientRepository(oAuth2AuthorizedClientRepository)
                .authorizationCodeGrant()
                .accessTokenResponseClient(accessTokenResponseClient);
        http.apply(configurer);
        http.antMatcher("/api/**")
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .cors().and()
                .csrf().disable()
                .oauth2ResourceServer()
                .jwt()
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(new PersistableJwtAuthenticationConverter())
                .and()
                .bearerTokenResolver(tokenResolver).authenticationEntryPoint(authenticationEntryPoint())
                .and().authorizeRequests().anyRequest().authenticated()
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

        OAuth2ClientConfigurer<HttpSecurity> configurer = new OAuth2ClientConfigurer<>();
        configurer.setBuilder(http);
        configurer.authorizedClientRepository(oAuth2AuthorizedClientRepository)
                .authorizationCodeGrant()
                .accessTokenResponseClient(accessTokenResponseClient);
        http.antMatcher("/tokens/refresh")
                .apply(configurer).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
                .cors().and()
                .csrf().disable()
                .oauth2ResourceServer().jwt()
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(new PersistableJwtAuthenticationConverter()).and()
                .bearerTokenResolver(tokenResolver).and()
                .authorizeRequests().anyRequest().authenticated()
        ;
        return http.build();
    }
    
    @Bean
    @Order(13)
    public SecurityFilterChain checkTokenHttpSecurity(HttpSecurity http) throws Exception {
        http.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);

        OAuth2ClientConfigurer<HttpSecurity> configurer = new OAuth2ClientConfigurer<>();
        configurer.setBuilder(http);
        configurer.authorizedClientRepository(oAuth2AuthorizedClientRepository)
                .authorizationCodeGrant()
                .accessTokenResponseClient(accessTokenResponseClient);
        http.antMatcher("/tokens/check")
                .oauth2Client()
                .authorizedClientRepository(oAuth2AuthorizedClientRepository)
                .authorizationCodeGrant()
                .accessTokenResponseClient(accessTokenResponseClient)
                .and().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
                .cors().and()
                .csrf().disable()
                .oauth2ResourceServer().jwt()
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(new PersistableJwtAuthenticationConverter()).and()
                .bearerTokenResolver(tokenResolver).and()
                .authorizeRequests().anyRequest().authenticated()
        ;
        return http.build();
    }

    @Bean
    @Order(14)
    public SecurityFilterChain loginHttpSecurity(HttpSecurity http) throws Exception {
        http.setSharedObject(ClientRegistrationRepository.class, clientRegistrationRepository);
        OAuth2ClientConfigurer<HttpSecurity> configurer = new OAuth2ClientConfigurer<>();
        configurer.setBuilder(http);
        configurer.authorizedClientRepository(oAuth2AuthorizedClientRepository)
                .authorizationCodeGrant()
                .accessTokenResponseClient(accessTokenResponseClient);
        http.antMatcher("/login/**")
                .oauth2Client()
                .authorizedClientRepository(oAuth2AuthorizedClientRepository)
                .authorizationCodeGrant()
                .accessTokenResponseClient(accessTokenResponseClient)
                .and().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
                .cors().and()
                .csrf().disable()
                .oauth2ResourceServer().jwt()
                .decoder(jwtDecoder)
                .jwtAuthenticationConverter(new PersistableJwtAuthenticationConverter()).and()
                .bearerTokenResolver(tokenResolver).and()
                .authorizeRequests().anyRequest().authenticated()
        ;
        return http.build();
    }

}
