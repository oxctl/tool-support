package uk.ac.ox.ctl;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.security.oauth2.core.AuthenticationMethod;

public class AuthenticationMethodJsonConverter {
    public static class Deserialize extends StdConverter<String, AuthenticationMethod> {
        @Override
        public AuthenticationMethod convert(String value) {
            if (AuthenticationMethod.HEADER.getValue().equalsIgnoreCase(value)) {
                return AuthenticationMethod.HEADER;
            }
            if (AuthenticationMethod.FORM.getValue().equalsIgnoreCase(value)) {
                return AuthenticationMethod.FORM;
            }
            if (AuthenticationMethod.QUERY.getValue().equalsIgnoreCase(value)) {
                return AuthenticationMethod.QUERY;
            }
            return null;
        }
    }

    public static class Serialize extends StdConverter<AuthenticationMethod, String>{
        @Override
        public String convert(AuthenticationMethod authenticationMethod) {
            return authenticationMethod != null ? authenticationMethod.toString() : null;
        }
    }
}
