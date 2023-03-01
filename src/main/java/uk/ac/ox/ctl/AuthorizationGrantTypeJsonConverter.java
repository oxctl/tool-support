package uk.ac.ox.ctl;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationGrantTypeJsonConverter {
    public static class Deserialize extends StdConverter<String, AuthorizationGrantType> {
        @Override
        public AuthorizationGrantType convert(String value) {
            if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equalsIgnoreCase(value)) {
                return AuthorizationGrantType.AUTHORIZATION_CODE;
            }
            if (AuthorizationGrantType.IMPLICIT.getValue().equalsIgnoreCase(value)) {
                return AuthorizationGrantType.IMPLICIT;
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
            return authorizationGrantType != null ? authorizationGrantType.toString() : null;
        }
    }
}
