package uk.ac.ox.ctl.canvasproxy;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import uk.ac.ox.ctl.oauth2.client.endpoint.CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter;
import uk.ac.ox.ctl.oauth2.client.userinfo.CanvasUserService;
import uk.ac.ox.ctl.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

import javax.mail.Session;
import java.time.Duration;
import java.util.Arrays;

@EnableWebSecurity
@Configuration
public class WebSecurity {

    @Bean("corsConfigurationSource")
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // TODO Pull this from the client configuration
        // If we support multiple clients how can we have a small list here?
        // The preflight contains the origin header so we will just return rules for that.
        corsConfiguration.addAllowedOrigin("https://localhost:3000");
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.DELETE.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.POST.name()));
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);
        // On simple requests we want to expose the Link header
        corsConfiguration.addExposedHeader("Link");
        corsConfiguration.setMaxAge(Duration.ofMinutes(30));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public CanvasUserService canvasUserService() {
        return new CanvasUserService();
    }

    // This is so we can remove old tokens.
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient client =
                new DefaultAuthorizationCodeTokenResponseClient();
        client.setRequestEntityConverter(
                new CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter());
        RestTemplate restTemplate =
                new RestTemplate(
                        Arrays.asList(
                                new FormHttpMessageConverter(),
                                new OAuth2AccessTokenResponseHttpMessageConverter()));
        // Switch to Apache HTTP Components;
        HttpClient requestFactory = HttpClientBuilder.create().disableContentCompression().build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(requestFactory));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        client.setRestOperations(restTemplate);
        return client;
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();
        // We need this, but it's not ideal
        tokenResolver.setAllowUriQueryParameter(true);
        // However I'm not sure we will fit the whole JWT in the URL.
        tokenResolver.setAllowFormEncodedBodyParameter(true);
        return tokenResolver;
    }

    @Configuration
    @Order(1)
    public static class ApiConfiguration extends WebSecurityConfigurerAdapter {
        @Autowired
        private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;
        @Autowired
        private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
        @Autowired
        private BearerTokenResolver tokenResolver;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**")
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                    .oauth2Client()
                    .authorizedClientRepository(oAuth2AuthorizedClientRepository)
                    .authorizationCodeGrant()
                    .accessTokenResponseClient(accessTokenResponseClient).and().and()
                    .cors().and()
                    .csrf().disable()
                    .oauth2ResourceServer().jwt().and().bearerTokenResolver(tokenResolver).authenticationEntryPoint(authenticationEntryPoint()).and()
                    .authorizeRequests().anyRequest().authenticated()
            ;
        }

        protected AuthenticationEntryPoint authenticationEntryPoint() {
            BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
            entryPoint.setRealmName("proxy");
            return entryPoint;
        }
    }

    @Configuration
    @Order(2)
    public static class TokenConfiguration extends WebSecurityConfigurerAdapter {
        @Autowired
        private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;
        @Autowired
        private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
        @Autowired
        private BearerTokenResolver tokenResolver;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER).and()
                    .oauth2Client()
                    .authorizedClientRepository(oAuth2AuthorizedClientRepository)
                    .authorizationCodeGrant()
                    .accessTokenResponseClient(accessTokenResponseClient).and().and()
                    .cors().and()
                    .csrf().disable()
                    .oauth2ResourceServer().jwt().and().bearerTokenResolver(tokenResolver).and()
                    .authorizeRequests().anyRequest().authenticated();

        }
    }


}
