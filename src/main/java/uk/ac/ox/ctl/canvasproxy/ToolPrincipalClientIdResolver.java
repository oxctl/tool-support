package uk.ac.ox.ctl.canvasproxy;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import uk.ac.ox.ctl.oauth2.client.web.method.annotation.PrincipalClientIdResolver;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This takes a JWT authentication token and attempts to find which client ID we should try and get a token for.
 */
public class ToolPrincipalClientIdResolver implements PrincipalClientIdResolver {

    private final AudienceConfigResolver resolver;

    public ToolPrincipalClientIdResolver(AudienceConfigResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public String findClientId(Authentication authentication) {
        if (authentication instanceof AbstractOAuth2TokenAuthenticationToken) {
            AbstractOAuth2TokenAuthenticationToken<Jwt> jwt = (AbstractOAuth2TokenAuthenticationToken<Jwt>) authentication;
            List<String> audiences = jwt.getToken().getAudience();
            Set<String> clientNames = audiences.stream()
                    .map(resolver::findProxyRegistration)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Iterator<String> iterator = clientNames.iterator();
            if (iterator.hasNext()) {
                String clientName = iterator.next();
                if (iterator.hasNext()) {
                    throw new IllegalStateException("We found multiple possible client IDs for the audiences");
                }
                return clientName;
            }
        }
        return null;
    }
}
