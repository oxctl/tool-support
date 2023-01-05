package uk.ac.ox.ctl.ltiauth.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.util.Assert;

import java.util.List;

/**
 * This is so that we check the audience on our token so that someone can't configure another copy of the tool and
 * change the passed values.
 */
public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static OAuth2Error INVALID_AUDIENCE =
            new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    "This aud claim is not equal to the configured audience",
                    null);

    private final String audience;

    public JwtAudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Assert.notNull(token, "token cannot be null");

        List<String> tokenAudiences = token.getClaimAsStringList(JwtClaimNames.AUD);
        if (tokenAudiences != null && tokenAudiences.contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        } else {
            return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
        }
    }
}
