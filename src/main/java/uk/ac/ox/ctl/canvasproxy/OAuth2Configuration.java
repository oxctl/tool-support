package uk.ac.ox.ctl.canvasproxy;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.CanvasAuthorizationCodeTokenResponseClient;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.OAuth2AccessTokenRefresher;
import uk.ac.ox.ctl.oauth2.client.endpoint.CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter;
import uk.ac.ox.ctl.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

import java.util.Arrays;

@Configuration
public class OAuth2Configuration {

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        CanvasAuthorizationCodeTokenResponseClient client =
                new CanvasAuthorizationCodeTokenResponseClient();
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
    @Primary
    public OAuth2AccessTokenRefresher oAuth2AccessTokenRefresher() {
        return new OAuth2AccessTokenRefresher();
    }

    @Bean("proxyBearerTokenResolver")
    @Qualifier("proxy")
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();
        // We need this, but it's not ideal
        tokenResolver.setAllowUriQueryParameter(true);
        // However I'm not sure we will fit the whole JWT in the URL.
        tokenResolver.setAllowFormEncodedBodyParameter(true);
        return tokenResolver;
    }

}
