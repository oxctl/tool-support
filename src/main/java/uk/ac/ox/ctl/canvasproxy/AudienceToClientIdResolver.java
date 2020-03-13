package uk.ac.ox.ctl.canvasproxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.PrincipalClientIdResolver;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This takes a JWT authentication token and attempts to find which client ID we should try and get a token for.
 */
@ConstructorBinding
@ConfigurationProperties("proxy")
public class AudienceToClientIdResolver implements PrincipalClientIdResolver {

    private final Map<String,String> mapping;

    public AudienceToClientIdResolver(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public String findClientId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
            List<String> audiences = jwt.getToken().getAudience();
            Set<String> clientIds = audiences.stream().map(mapping::get).filter(Objects::nonNull).collect(Collectors.toSet());
            Iterator<String> iterator = clientIds.iterator();
            if (iterator.hasNext()) {
                String clientId = iterator.next();
                if (iterator.hasNext()) {
                    throw new IllegalStateException("We found multiple possible client IDs for the audiences");
                }
                return clientId;
            }
        }
        return null;
    }
}
