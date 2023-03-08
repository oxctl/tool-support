package uk.ac.ox.ctl.service;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import uk.ac.ox.ctl.model.ToolRegistration;
import uk.ac.ox.ctl.model.ToolRegistrationLti;
import uk.ac.ox.ctl.model.ToolRegistrationProxy;

/**
 * This looks up a Tool and then retrieves a ToolRegistration and converts it to a ClientRegistration.
 */
public class ToolRegistrationUtilities {

    public static ClientRegistration toClientRegistration(ToolRegistration toolRegistration) {
        final ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(toolRegistration.getRegistrationId())
                .clientName(toolRegistration.getClientName())
                .clientId(toolRegistration.getClientId())
                .clientSecret(toolRegistration.getClientSecret())
                .clientAuthenticationMethod(toolRegistration.getClientAuthenticationMethod())
                .authorizationGrantType(toolRegistration.getAuthorizationGrantType())
                .redirectUri(toolRegistration.getRedirectUri())
                .scope(toolRegistration.getScopes())
                .issuerUri(toolRegistration.getProviderDetails().getIssuerUri())
                .jwkSetUri(toolRegistration.getProviderDetails().getJwkSetUri())
                .tokenUri(toolRegistration.getProviderDetails().getTokenUri());
        // The user info endpoint might be null
        if (toolRegistration.getProviderDetails().getUserInfoEndpoint() != null) {
            builder
                .userInfoAuthenticationMethod(toolRegistration.getProviderDetails().getUserInfoEndpoint().getAuthenticationMethod())
                .userInfoUri(toolRegistration.getProviderDetails().getUserInfoEndpoint().getUri())
                .userNameAttributeName(toolRegistration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
        }
        builder
                .providerConfigurationMetadata(toolRegistration.getProviderDetails().getConfigurationMetadata())
                .authorizationUri(toolRegistration.getProviderDetails().getAuthorizationUri())
                .build();
        return builder.build();
    }

    public static ToolRegistrationLti toToolRegistrationLti(ClientRegistration clientRegistration) {
        ToolRegistrationLti registration = new ToolRegistrationLti();
        map(clientRegistration, registration);
        return registration;
    }

    public static ToolRegistrationProxy toToolRegistrationProxy(ClientRegistration clientRegistration) {
        ToolRegistrationProxy registration = new ToolRegistrationProxy();
        map(clientRegistration, registration);
        return registration;
    }

    private static void map(ClientRegistration clientRegistration, ToolRegistration toolRegistration) {
        toolRegistration.setRegistrationId(clientRegistration.getRegistrationId());
        toolRegistration.setClientName(clientRegistration.getClientName());
        toolRegistration.setClientId(clientRegistration.getClientId());
        toolRegistration.setClientSecret(clientRegistration.getClientSecret());
        toolRegistration.setClientAuthenticationMethod(clientRegistration.getClientAuthenticationMethod());
        toolRegistration.setAuthorizationGrantType(clientRegistration.getAuthorizationGrantType());
        toolRegistration.setRedirectUri(clientRegistration.getRedirectUri());
        toolRegistration.setScopes(clientRegistration.getScopes());
        toolRegistration.getProviderDetails().setIssuerUri(clientRegistration.getProviderDetails().getIssuerUri());
        toolRegistration.getProviderDetails().setJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri());
        toolRegistration.getProviderDetails().setTokenUri(clientRegistration.getProviderDetails().getTokenUri());
        toolRegistration.getProviderDetails().getUserInfoEndpoint().setAuthenticationMethod(clientRegistration.getProviderDetails().getUserInfoEndpoint().getAuthenticationMethod());
        toolRegistration.getProviderDetails().getUserInfoEndpoint().setUri(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri());
        toolRegistration.getProviderDetails().getUserInfoEndpoint().setUserNameAttributeName(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
        toolRegistration.getProviderDetails().setConfigurationMetadata(clientRegistration.getProviderDetails().getConfigurationMetadata());
        toolRegistration.getProviderDetails().setAuthorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri());
    }
    
    
}
