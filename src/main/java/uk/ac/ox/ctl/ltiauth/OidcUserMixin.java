package uk.ac.ox.ctl.ltiauth;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.boot.jackson.JsonMixin;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

/**
 * This is needed to make sure that we use "token_value" instead of "tokenValue" when retrieving the token
 * from the LTI service. This is the way of getting an unsigned token and should be removed in time.
 */
@JsonMixin({OAuth2AuthenticationToken.class, OidcIdToken.class})
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class OidcUserMixin {
    
}
