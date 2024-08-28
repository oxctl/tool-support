package uk.ac.ox.ctl;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.web.LTIAuthorizationGrantType;

public class AuthorizationGrantTypeJsonConverter {
    public static class Deserialize extends StdConverter<String, AuthorizationGrantType> {
        @Override
        public AuthorizationGrantType convert(String value) {
            if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equalsIgnoreCase(value)) {
                return AuthorizationGrantType.AUTHORIZATION_CODE;
            }
            if (LTIAuthorizationGrantType.IMPLICIT.getValue().equalsIgnoreCase(value)) {
                return LTIAuthorizationGrantType.IMPLICIT;
            }
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equalsIgnoreCase(value)) {
                return AuthorizationGrantType.CLIENT_CREDENTIALS;
            }
            if (AuthorizationGrantType.PASSWORD.getValue().equalsIgnoreCase(value)) {
                return AuthorizationGrantType.PASSWORD;
            }
            return new AuthorizationGrantType(value);
        }
    }

    public static class Serialize extends StdConverter<AuthorizationGrantType, String> {

        @Override
        public String convert(AuthorizationGrantType authorizationGrantType) {
            return authorizationGrantType != null ? authorizationGrantType.getValue() : null;
        }
    }
}
