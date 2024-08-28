/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ox.ctl.canvasproxy.security.oauth2.client.web.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.endpoint.RefreshOAuth2AuthorizedClient;
import uk.ac.ox.ctl.canvasproxy.security.oauth2.client.annotation.RegisteredOAuth2AccessToken;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.PrincipalClientIdResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An implementation of a {@link HandlerMethodArgumentResolver} that is capable of resolving a
 * method parameter to an argument value of type {@link OAuth2AccessToken}. This resolver ensures that the token
 * is also valid and if it isn't tries to refresh it.
 *
 * <p>For example:
 *
 * <pre>
 * &#64;Controller
 * public class MyController {
 *     &#64;GetMapping("/authorized-client")
 *     public String authorizedClient(@RegisteredOAuth2AccessToken("login-client") OAuth2AccessToken accessToken) {
 *         // do something with authorizedClient
 *     }
 * }
 * </pre>
 * <p>
 * This has been customised from the Spring version so that you can set a resolver that maps from a principal to a
 * client ID.
 *
 * @author Matthew Buckett
 * @see RegisteredOAuth2AccessToken
 * @see RefreshOAuth2AuthorizedClient
 * @since 5.1
 */
public final class OAuth2AccessTokenArgumentResolver implements HandlerMethodArgumentResolver {



    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RefreshOAuth2AuthorizedClient authorizedClientRepository;
    private PrincipalClientIdResolver principalClientIdResolver;
    private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest>
            clientCredentialsTokenResponseClient = new DefaultClientCredentialsTokenResponseClient();

    /**
     * Constructs an {@code OAuth2AuthorizedClientArgumentResolver} using the provided parameters.
     *
     * @param clientRegistrationRepository the repository of client registrations
     * @param authorizedClientRepository   the repository of authorized clients
     */
    public OAuth2AccessTokenArgumentResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            RefreshOAuth2AuthorizedClient authorizedClientRepository) {
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        Assert.notNull(authorizedClientRepository, "authorizedClientRepository cannot be null");
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientRepository = authorizedClientRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> parameterType = parameter.getParameterType();
        return (OAuth2AccessToken.class.isAssignableFrom(parameterType)
                && (AnnotatedElementUtils.findMergedAnnotation(
                parameter.getParameter(), RegisteredOAuth2AccessToken.class)
                != null));
    }

    @NonNull
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) {

        String clientRegistrationId = this.resolveClientRegistrationId();
        Authentication principal = SecurityContextHolder.getContext().getAuthentication();
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse servletResponse = webRequest.getNativeRequest(HttpServletResponse.class);

        OAuth2AuthorizedClient authorizedClient =
                this.authorizedClientRepository.loadAuthorizedClient(
                        clientRegistrationId, principal, servletRequest);
        if (authorizedClient != null) {
            // Need to check that it's still valid.
            if (this.authorizedClientRepository.needsRenewal(authorizedClient)) {
                authorizedClient = this.authorizedClientRepository.renewAccessToken(clientRegistrationId, principal, servletRequest, servletResponse);
            }
        }
        if (authorizedClient != null) {
            return authorizedClient.getAccessToken();
        }

        ClientRegistration clientRegistration =
                this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (clientRegistration == null) {
            return null;
        }

        RegisteredOAuth2AccessToken mergedAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                parameter.getParameter(), RegisteredOAuth2AccessToken.class);
        if (mergedAnnotation.required()) {

            if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(
                    clientRegistration.getAuthorizationGrantType())) {
                throw new ClientAuthorizationRequiredException(clientRegistrationId);
            }

            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
                    clientRegistration.getAuthorizationGrantType())) {
                authorizedClient =
                        this.authorizeClientCredentialsClient(
                                clientRegistration, servletRequest, servletResponse);
            }
        }

        return authorizedClient;
    }

    private String resolveClientRegistrationId() {
        Authentication principal = SecurityContextHolder.getContext().getAuthentication();
        return principalClientIdResolver.findClientId(principal);
    }

    private OAuth2AuthorizedClient authorizeClientCredentialsClient(
            ClientRegistration clientRegistration,
            HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest =
                new OAuth2ClientCredentialsGrantRequest(clientRegistration);
        OAuth2AccessTokenResponse tokenResponse =
                this.clientCredentialsTokenResponseClient.getTokenResponse(clientCredentialsGrantRequest);

        Authentication principal = SecurityContextHolder.getContext().getAuthentication();

        OAuth2AuthorizedClient authorizedClient =
                new OAuth2AuthorizedClient(
                        clientRegistration,
                        (principal != null ? principal.getName() : "anonymousUser"),
                        tokenResponse.getAccessToken());

        this.authorizedClientRepository.saveAuthorizedClient(
                authorizedClient, principal, request, response);

        return authorizedClient;
    }

    /**
     * Sets the client used when requesting an access token credential at the Token Endpoint for the
     * {@code client_credentials} grant.
     *
     * @param clientCredentialsTokenResponseClient the client used when requesting an access token
     *                                             credential at the Token Endpoint for the {@code client_credentials} grant
     */
    public final void setClientCredentialsTokenResponseClient(
            OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest>
                    clientCredentialsTokenResponseClient) {
        Assert.notNull(
                clientCredentialsTokenResponseClient,
                "clientCredentialsTokenResponseClient cannot be null");
        this.clientCredentialsTokenResponseClient = clientCredentialsTokenResponseClient;
    }

    /**
     * Sets the object that should be used when trying to extract a client ID from a principal.
     *
     * @param principalClientIdResolver the principal to client ID mapper that should be used when a client ID isn't
     *                                  specified in the annotation.
     */
    public void setPrincipalClientIdResolver(PrincipalClientIdResolver principalClientIdResolver) {
        this.principalClientIdResolver = principalClientIdResolver;
    }
}
